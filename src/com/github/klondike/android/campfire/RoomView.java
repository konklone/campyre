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
import android.widget.LinearLayout;
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
	
	private static final int MAX_STARTING_MESSAGES = 20;
	private static final int MAX_MESSAGES = 20;
	private static final int AUTOPOLL_INTERVAL = 8; // in seconds

	private Campfire campfire;
	private String roomId;
	private Room room;
	
	private ArrayList<RoomEvent> events;
	private ArrayList<RoomEvent> newEvents;
	private RoomEvent newPost;
	
	private EditText message;
	private Button speak, refresh;
	
	private boolean autoPoll;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room);
		
		roomId = getIntent().getStringExtra("room_id");
		autoPoll = true;
		
		verifyLogin();
	}
	
	// Will only happen after we are definitely logged in, 
	// and the campfire member variable has been loaded with a logged-in Campfire
	private void onLogin() {
		joinRoom();
	}
	
	// Will only happen after we are both logged in and the room has been joined
	// events has been populated with the starting messages of a room
	private void onJoined() {
		setupControls();
		loadEvents();
		
		autoPoll();
	}
	
	// newEvents has been populated by a helper thread with the new events
	private void onPoll() {
		if (events.size() == 0)
			events = newEvents;
		else {
			int size = newEvents.size();
			for (int i=0; i<size; i++)
				events.add(newEvents.get(i));
			if (events.size() > MAX_MESSAGES) {
				for (int i=0; i < (events.size() - MAX_MESSAGES); i++)
					events.remove(0);
			}
		}
		
		loadEvents();
	}
	
	// newPost has been populated with the last message the user just posted
	// and which (currently) is guaranteed to be actually posted
	private void onSpeak() {
		events.add(newPost);
		loadEvents();
	}
	
	private void setupControls() {
		//TODO still:
		// set name of room in window title
		
		message = (EditText) this.findViewById(R.id.room_message);
		message.setEnabled(true);
		message.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					speak();
				else if (event != null) // the event will only be present for "Enter"
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
		refresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				poll();
			}
		});
	}
	
	private void loadEvents() {
		setListAdapter(new RoomAdapter(this, events));
		// keep it scrolled to the bottom
		getListView().setSelection(events.size()-1);
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
			alert("Couldn't join room. Select the Re-join menu option to try again.");
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
			showDialog(POLLING);
		}
	};
	
	final Runnable pollSuccess = new Runnable() {
		public void run() {
			removeDialog(POLLING);
			onPoll();
		}
	};
	
	final Runnable pollFailure = new Runnable() {
		public void run() {
			removeDialog(POLLING);
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
	
	private void poll() {
		new Thread() {
			public void run() {
				handler.post(pollStart);
				try {
					newEvents = room.listen();
					handler.post(pollSuccess);
				} catch(CampfireException e) {
					handler.post(pollFailure);
				}
			}
		}.start();
	}
	
	private void autoPoll() {
		new Thread() {
			public void run() {
				while(true) {
					if (autoPoll) {
						handler.post(pollStart);
						try {
							newEvents = room.listen();
							handler.post(pollSuccess);
						} catch(CampfireException e) {
							handler.post(pollFailure);
						}
						
						try {
							sleep(AUTOPOLL_INTERVAL * 1000);
						} catch(InterruptedException ex) {
							// well, I never
						}
					}
				}
			}
		}.start();
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
}