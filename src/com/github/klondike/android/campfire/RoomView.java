package com.github.klondike.android.campfire;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;
import com.github.klondike.java.campfire.Room;
import com.github.klondike.java.campfire.RoomEvent;

public class RoomView extends ListActivity {
	private static final int MENU_PREFS = 0;
	private static final int MENU_AUTOPOLL = 1;
	private static final int MENU_LOGOUT = 2;

	private static final int JOINING = 0;
	private static final int SPEAKING = 1;
	private static final int POLLING = 2;
	
	private static final int MAX_STARTING_MESSAGES = 30;
	private static final int AUTOPOLL_INTERVAL = 15; // in seconds

	private Campfire campfire;
	private String roomId;
	private Room room;
	
	private ArrayList<RoomEvent> events;
	private ArrayList<RoomEvent> newEvents;
	private RoomEvent newPost;
	
	private EditText message;
	private Button speak, refresh;
	private ImageView polling;
	
	private boolean autoPoll = true;
	private boolean joined = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room);
		
		roomId = getIntent().getStringExtra("room_id");
		
		if (savedInstanceState != null)
			autoPoll = savedInstanceState.getBoolean("autoPoll", true);
		
		// on screen flip, attempt to restore state without rejoining everything
		RoomViewHolder holder = (RoomViewHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.campfire = holder.campfire;
			this.room = holder.room;
			this.events = holder.events;
			onJoined();
		} else
			verifyLogin();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		// only store all this state if we made it all the way through the login/join process
		if (joined) {
			RoomViewHolder holder = new RoomViewHolder();
			holder.campfire = this.campfire;
			holder.room = this.room;
			holder.events = this.events;
			return holder;
		} else
			return null;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("autoPoll", autoPoll);
		super.onSaveInstanceState(outState);
	}
	
	// Will only happen after we are definitely logged in, 
	// and the campfire member variable has been loaded with a logged-in Campfire
	private void onLogin() {
		joinRoom();
	}
	
	// Will only happen after we are both logged in and the room has been joined
	// events has been populated with the starting messages of a room
	private void onJoined() {
		joined = true;
		
		setupControls();
		
		setListAdapter(new RoomAdapter(this, events));
		scrollToBottom();
		
		if (autoPoll) autoPoll();
	}
	
	// newEvents has been populated by a helper thread with the new events
	private void onPoll() {
		boolean wasAtBottom = scrolledToBottom();
		
		// add the new items
		RoomAdapter adapter = (RoomAdapter) getListAdapter();
		for (int i=0; i<newEvents.size(); i++)
			adapter.add(newEvents.get(i));
		
		if (wasAtBottom)
			scrollToBottom();	
	}
	
	// newPost has been populated with the last message the user just posted
	// and which (currently) is guaranteed to be actually posted
	private void onSpeak() {
		((RoomAdapter) getListAdapter()).add(newPost);
		scrollToBottom();
	}
	
	private void setupControls() {
		polling = (ImageView) findViewById(R.id.room_polling);
		
		message = (EditText) this.findViewById(R.id.room_message);
		message.setEnabled(true);
		message.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					speak();
				else if (event != null) // the event will only be non-null for a press of the "Enter" key
					speak();
				return false;
			}
		});
		
		speak = (Button) this.findViewById(R.id.room_speak);
		speak.setEnabled(true);
		speak.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				speak();
			}
		});
		
		refresh = (Button) this.findViewById(R.id.room_refresh);
		refresh.setEnabled(true);
		refresh.setVisibility(autoPoll ? View.GONE : View.VISIBLE);
		refresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pollOnce();
			}
		});
	}
	
	private void scrollToBottom() {
		getListView().setSelection(getListAdapter().getCount()-1);
	}
	
	private boolean scrolledToBottom() {
		return (getListView().getLastVisiblePosition() == (getListAdapter().getCount()-1));
	}
	
	final Handler handler = new Handler();
	final Runnable joinSuccess = new Runnable() {
		public void run() {
			removeDialog(JOINING);
			onJoined();
		}
	};
	final Runnable joinFailure = new Runnable() {
		public void run() {
			removeDialog(JOINING);
			alert("Couldn't join room for some reason. Click the room to try again.");
			finish();
		}
	};
	
	final Runnable speakSuccess = new Runnable() {
		public void run() {
			removeDialog(SPEAKING);
			message.setText("");
			speak.setEnabled(true);
			
			onSpeak();
		}
	};
	final Runnable speakError = new Runnable() {
		public void run() {
			alert("Connection error.");
			removeDialog(SPEAKING);
			
			speak.setEnabled(true);
		}
	};
	
	final Runnable pollStart = new Runnable() {
		public void run() {
			refresh.setVisibility(View.GONE);
			polling.setVisibility(View.VISIBLE);
		}
	};
	
	final Runnable pollSuccess = new Runnable() {
		public void run() {
			if (autoPoll)
				refresh.setVisibility(View.INVISIBLE);
			else
				refresh.setVisibility(View.VISIBLE);
			polling.setVisibility(View.GONE);
			onPoll();
		}
	};
	
	final Runnable pollFailure = new Runnable() {
		public void run() {
			refresh.setVisibility(View.VISIBLE);
			polling.setVisibility(View.GONE);
			alert("Connection error.");
		}
	};
	
	private void speak() {
		final String msg = message.getText().toString();
		if (msg.equals(""))
			return;
		
		speak.setEnabled(false);
		
		Thread speakThread = new Thread() {
			public void run() {
				try {
					newPost = room.speak(msg);
					if (newPost != null)
						handler.post(speakSuccess);
					else
						handler.post(speakError);
				} catch (CampfireException e) {
					handler.post(speakError);
				}
			}
		};
		speakThread.start();
		showDialog(SPEAKING);
	}

	private void joinRoom() {
		Thread joinThread = new Thread() {
			public void run() {
				room = new Room(campfire, roomId);
				try {
					if (room.join()) {
						events = room.startingEvents(MAX_STARTING_MESSAGES);
						handler.post(joinSuccess);
					}
					else
						handler.post(joinFailure);
				} catch(CampfireException e) {
					handler.post(joinFailure);
				}
			}
		};
		joinThread.start();
		
		showDialog(JOINING);
	}
	
	private void pollOnce() {
		new Thread() {
			public void run() {
				poll();
			}
		}.start();
	}
	
	private void autoPoll() {
		new Thread() {
			public void run() {
				while(autoPoll) {
					// sleep first so that this doesn't needlessly poll when we first join the room
					try {
						sleep(AUTOPOLL_INTERVAL * 1000);
					} catch(InterruptedException ex) {
						// well, I never
					}
					
					// the user might have turned off autoPoll while we were sleeping!
					if (autoPoll)
						poll();
				}
			}
		}.start();
	}
	
	private void poll() {
		handler.post(pollStart);
		try {
			newEvents = room.listen();
			handler.post(pollSuccess);
		} catch(CampfireException e) {
			handler.post(pollFailure);
		}
	}
	
	private void verifyLogin() {
		campfire = Login.getCampfire(this);
        if (campfire.loggedIn())
        	onLogin();
        else
        	startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
    }
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case Login.RESULT_LOGIN:
    		if (resultCode == RESULT_OK) {
    			alert("You have been logged in successfully.");
    			campfire = Login.getCampfire(this);
    			onLogin();
    		} else
    			finish();
    	}
    }
	
	@Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    boolean result = super.onCreateOptionsMenu(menu);
	    
        menu.add(0, MENU_PREFS, MENU_PREFS, "Preferences")
        	.setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_AUTOPOLL, MENU_AUTOPOLL, autoPoll ? R.string.autopoll_off : R.string.autopoll_on)
        	.setIcon(android.R.drawable.ic_menu_rotate);
        menu.add(0, MENU_LOGOUT, MENU_LOGOUT, "Log Out")
        	.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return result;
    }
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);
		
		menu.getItem(MENU_AUTOPOLL).setTitle(autoPoll ? R.string.autopoll_off : R.string.autopoll_on);
		
		return result;
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_PREFS:
    		startActivity(new Intent(this, Preferences.class)); 
    		return true;
    	case MENU_AUTOPOLL:
    		// until there exist race conditions on this variable (multiple actors writing to it), 
    		// no synchronization required
    		autoPoll = !autoPoll;
    		refresh.setVisibility(autoPoll ? View.INVISIBLE: View.VISIBLE);
    		if (autoPoll) autoPoll();
    		return true;
    	case MENU_LOGOUT:
    		getSharedPreferences("campfire", 0).edit().putString("session", null).commit();
    		finish();
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    protected Dialog onCreateDialog(int id) {
    	ProgressDialog loadingDialog = new ProgressDialog(this);
    	loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        switch(id) {
        case SPEAKING:
            loadingDialog.setMessage("Speaking...");
            return loadingDialog;
        case JOINING:
        	loadingDialog.setMessage("Joining room...");
        	loadingDialog.setCancelable(false);
            return loadingDialog;
        case POLLING:
        	loadingDialog.setMessage("Polling...");
        	return loadingDialog;
        default:
            return null;
        }
    }
	
	public void alert(String msg) {
		Toast.makeText(RoomView.this, msg, Toast.LENGTH_SHORT).show();
	}
	
	private static class RoomAdapter extends ArrayAdapter<RoomEvent> {
		private LayoutInflater inflater;
		
        public RoomAdapter(Activity context, ArrayList<RoomEvent> events) {
            super(context, 0, events);
            inflater = LayoutInflater.from(context);
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			RoomEvent item = getItem(position);
			
			ViewHolder holder;
			if (convertView != null)
				holder = (ViewHolder) convertView.getTag();
			else
				holder = null;
			
			if (convertView == null || holder.type != item.type) {
				convertView = inflater.inflate(viewForType(item.type), null);
				
				holder = new ViewHolder();
				holder.message = (TextView) convertView.findViewById(R.id.text);
				holder.type = item.type;
				if (item.person != null)
					holder.person = (TextView) convertView.findViewById(R.id.person);
				
				convertView.setTag(holder);
			}
			
			holder.message.setText(item.message);
			if (item.person != null)
				holder.person.setText(item.person);
			
			return convertView;
		}
		
		public int viewForType(int type) {
			switch (type) {
			case RoomEvent.TEXT:
				return R.layout.event_text;
			case RoomEvent.TIMESTAMP:
				return R.layout.event_timestamp;
			case RoomEvent.ENTRY:
				return R.layout.event_entry;
			default:
				return R.layout.event_text;
			}
		}
		
		static class ViewHolder {
            TextView message, person;
            int type;
        }

    }
	
	static class RoomViewHolder {
		Campfire campfire;
		Room room;
		ArrayList<RoomEvent> events;
	}
}