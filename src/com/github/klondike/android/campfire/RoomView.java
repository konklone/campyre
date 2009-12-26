package com.github.klondike.android.campfire;

import java.util.ArrayList;
import java.util.HashMap;

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
import com.github.klondike.java.campfire.User;

public class RoomView extends ListActivity {
	private static final int MENU_AUTOPOLL = 0;
	private static final int MENU_LOGOUT = 1;

	private static final int JOINING = 0;
	private static final int SPEAKING = 1;
	private static final int POLLING = 2;
	
	private static final int MAX_MESSAGES = 20;
	private static final int AUTOPOLL_INTERVAL = 15; // in seconds

	private Campfire campfire;
	private String roomId;
	private Room room;
	
	private ArrayList<RoomEvent> messages = new ArrayList<RoomEvent>();
	private HashMap<String,User> users = new HashMap<String,User>();
	
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
			this.messages = holder.messages;
			this.users = holder.users;
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
			holder.messages = this.messages;
			holder.users = this.users;
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
	private void onJoined() {
		setupControls();
		
		// messages is an empty array at this point
		setListAdapter(new RoomAdapter(this, messages));
		
		if (autoPoll) 
			autoPoll();
		else
			pollOnce();
	}
	
	// "messages" has been populated with the latest MAX_MESSAGES events
	private void onPoll() {
		boolean wasAtBottom = scrolledToBottom();
		
		setListAdapter(new RoomAdapter(this, messages));
		
		if (wasAtBottom)
			scrollToBottom();	
	}
	
	// newPost has been populated with the last message the user just posted
	// and which is guaranteed to be actually posted
	private void onSpeak() {
		//TODO: Poll for new messages instead of adding new post to bottom
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
			joined = true;
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
					RoomEvent newPost = room.speak(msg);
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
				try {
					// Joining a room is a four-step process with 3 network requests:
					
					// 1) Get the room details (name, topic, etc.)
					room = Room.find(campfire, roomId);
					
					// 2) Make sure the room isn't full
					if (room.full) {
						handler.post(joinFailure);
						return;
					}
					
					// 3) Join the room
					if (!room.join()) {
						handler.post(joinFailure);
						return;
					}
					
					// 4) Get the details of the logged in user and throw it in the User hash 
					users.put(campfire.user_id, User.find(campfire, campfire.user_id));
					
					handler.post(joinSuccess);
				} catch (CampfireException e) {
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
					poll();
					
					try {
						sleep(AUTOPOLL_INTERVAL * 1000);
					} catch(InterruptedException ex) {
						// well, I never
					}	
				}
			}
		}.start();
	}
	
	private void poll() {
		handler.post(pollStart);
		try {
			//TODO: Have this pull the latest MAX_MESSAGES events from today's transcript
			//TODO: Store user details on the message object
			//TODO: Look up users for any messages whose user_id's we don't know
			messages = room.listen();
			handler.post(pollSuccess);
		} catch(CampfireException e) {
			handler.post(pollFailure);
		}
	}
	
	private void verifyLogin() {
		campfire = Login.getCampfire(this);
        if (campfire != null)
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
	    
        menu.add(0, MENU_AUTOPOLL, MENU_AUTOPOLL, autoPoll ? R.string.autopoll_off : R.string.autopoll_on)
        	.setIcon(android.R.drawable.ic_menu_rotate);
        menu.add(0, MENU_LOGOUT, MENU_LOGOUT, R.string.logout)
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
    	case MENU_AUTOPOLL:
    		// until there exist race conditions on this variable, no synchronization required
    		autoPoll = !autoPoll;
    		refresh.setVisibility(autoPoll ? View.INVISIBLE: View.VISIBLE);
    		if (autoPoll) autoPoll();
    		return true;
    	case MENU_LOGOUT:
    		Login.clearCampfire(this);
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
		
        public RoomAdapter(Activity context, ArrayList<RoomEvent> messages) {
            super(context, 0, messages);
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
				holder.body = (TextView) convertView.findViewById(R.id.text);
				holder.type = item.type;
				if (item.person != null)
					holder.person = (TextView) convertView.findViewById(R.id.person);
				
				convertView.setTag(holder);
			}
			
			holder.body.setText(item.body);
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
            TextView body, person;
            int type;
        }

    }
	
	static class RoomViewHolder {
		Campfire campfire;
		Room room;
		ArrayList<RoomEvent> messages;
		HashMap<String,User> users;
	}
}