package com.github.klondike.android.campfire;

import java.text.SimpleDateFormat;
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
import com.github.klondike.java.campfire.Message;
import com.github.klondike.java.campfire.Room;
import com.github.klondike.java.campfire.User;

public class RoomView extends ListActivity {
	private static final int MENU_AUTOPOLL = 0;
	private static final int MENU_LOGOUT = 1;

	private static final int JOINING = 0;
	private static final int SPEAKING = 1;
	private static final int POLLING = 2;
	
	private static final int MAX_MESSAGES = 20;
	private static final int AUTOPOLL_INTERVAL = 15; // in seconds
	
	private static String timestampFormat = "hh:mm a";

	private Campfire campfire;
	private String roomId;
	private Room room;
	
	private ArrayList<Message> messages = new ArrayList<Message>();
	private HashMap<String,User> users = new HashMap<String,User>();
	
	private EditText message;
	private Button speak, refresh;
	private ImageView polling;
	private Message newMessage;
	
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
		
		// messages is already filled with a starting set of messages
		setListAdapter(new RoomAdapter(this, messages));
		scrollToBottom();
		
		if (autoPoll) 
			autoPoll();
	}
	
	// "messages" has been populated with the latest MAX_MESSAGES messages
	private void onPoll() {
		boolean wasAtBottom = scrolledToBottom();
		
		setListAdapter(new RoomAdapter(this, messages));
		
		if (wasAtBottom)
			scrollToBottom();	
	}
	
	// The message the user just posted has just been added to the bottom of the list
	private void onSpeak() {
		((RoomAdapter) getListAdapter()).add(newMessage);
		
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
					// Speaking is a 2-step process
					
					// 1) Join the room (in case auto-polling is off and we've since been idle-kicked out)
					if (!room.join()) {
						handler.post(speakError);
						return;
					}
					
					// 2) Post to the room
					newMessage = room.speak(msg);
					if (newMessage == null) {
						handler.post(speakError);
						return;
					}
					
					// 3) Fill in the message with our user details 
					fillPerson(newMessage);
					
					handler.post(speakSuccess);
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
					// Joining a room is a five-step process with 4 network requests:
					
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
					
					// 5) Do an initial poll
					poll();
					
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
				handler.post(pollStart);
				try {
					poll();
					
					// ping the room so we don't get idle-kicked out
					room.join();
					
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
				while(autoPoll) {
					try {
						sleep(AUTOPOLL_INTERVAL * 1000);
					} catch(InterruptedException ex) {
						// well, I never
					}
					
					// the user might have turned off autoPoll while we were sleeping!
					if (autoPoll) {
						handler.post(pollStart);
						try {
							poll();
							
							// ping the room so we don't get idle-kicked out
							room.join();
							
							handler.post(pollSuccess);
						} catch(CampfireException e) {
							handler.post(pollFailure);
						}
					}
				}
			}
		}.start();
	}
	
	// Fetches latest MAX_MESSAGES from the transcript, then for each message
	// looks up the associated User to assign a display name
	// we use the "users" HashMap to cache Users from the network 
	private void poll() throws CampfireException {
		messages = Message.allToday(room, MAX_MESSAGES);
		int length = messages.size();
		for (int i=0; i<length; i++) {
			Message message = messages.get(i);
			if (message.user_id != null)
				fillPerson(message);
		}
	}
	
	private void fillPerson(Message message) throws CampfireException {
		User speaker;
		if (users.containsKey(message.user_id))
			speaker = (User) users.get(message.user_id);
		else {
			speaker = User.find(campfire, message.user_id);
			users.put(message.user_id, speaker);
		}
		message.person = speaker.displayName();
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
	
	private static class RoomAdapter extends ArrayAdapter<Message> {
		private LayoutInflater inflater;
		
        public RoomAdapter(Activity context, ArrayList<Message> messages) {
            super(context, 0, messages);
            inflater = LayoutInflater.from(context);
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			Message message = getItem(position);
			
			ViewHolder holder;
			if (convertView != null)
				holder = (ViewHolder) convertView.getTag();
			else
				holder = null;
			
			if (convertView == null || holder.type != message.type) {
				convertView = inflater.inflate(viewForMessage(message), null);
				
				holder = new ViewHolder();
				holder.body = (TextView) convertView.findViewById(R.id.text);
				holder.type = message.type;
				if (message.person != null)
					holder.person = (TextView) convertView.findViewById(R.id.person);
				
				convertView.setTag(holder);
			}
			
			holder.body.setText(bodyForMessage(message));
			if (message.person != null)
				holder.person.setText(message.person);
			
			return convertView;
		}
		
		public int viewForMessage(Message message) {
			switch (message.type) {
			case Message.TEXT:
				return R.layout.message_text;
			case Message.TIMESTAMP:
				return R.layout.message_timestamp;
			case Message.ENTRY:
			case Message.LEAVE:
				return R.layout.message_entry;
			default:
				return R.layout.message_text;
			}
		}
		
		public String bodyForMessage(Message message) {
			switch (message.type) {
			case Message.TEXT:
				return message.body; 
			case Message.ENTRY:
				return "has entered the room";
			case Message.LEAVE:
				return "has left the room";
			case Message.TIMESTAMP:
				return new SimpleDateFormat(timestampFormat).format(message.timestamp);
			default:
				return message.body;
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
		ArrayList<Message> messages;
		HashMap<String,User> users;
	}
}