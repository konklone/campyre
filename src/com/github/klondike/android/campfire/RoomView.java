package com.github.klondike.android.campfire;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
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
	
	private static final int MAX_MESSAGES = 20;
	private static final int AUTOPOLL_INTERVAL = 15; // in seconds
	
	private static String timestampFormat = "hh:mm a";

	private Campfire campfire;
	private String roomId;
	private Room room;
	private SpeakTask speakTask;
	private JoinTask joinTask;
	private ProgressDialog dialog = null;
	
	private ArrayList<Message> messages = new ArrayList<Message>();
	private HashMap<String,User> users = new HashMap<String,User>();
	
	private EditText body;
	private Button speak, refresh;
	private ImageView polling;
	
	private boolean autoPoll = true;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room);
		
		roomId = getIntent().getStringExtra("room_id");
		
		if (savedInstanceState != null)
			autoPoll = savedInstanceState.getBoolean("autoPoll", true);
		
		setupControls();
		
		// on screen flip, attempt to restore state without rejoining everything
		RoomViewHolder holder = (RoomViewHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			campfire = holder.campfire;
			room = holder.room;
			messages = holder.messages;
			users = holder.users;
			speakTask = holder.speakTask;
			joinTask = holder.joinTask;
			
			if (speakTask != null) {
				speakTask.context = this;
				loadingDialog(SPEAKING);
			}
			
			if (joinTask != null) {
				joinTask.context = this;
				loadingDialog(JOINING);
			} else
				onJoined();
		} else
			verifyLogin();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		RoomViewHolder holder = new RoomViewHolder();
		holder.campfire = this.campfire;
		holder.room = this.room;
		holder.messages = this.messages;
		holder.users = this.users;
		holder.speakTask = this.speakTask;
		holder.joinTask = this.joinTask;
		return holder;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("autoPoll", autoPoll);
		super.onSaveInstanceState(outState);
	}
	
	// Will only happen after we are definitely logged in, 
	// and the campfire member variable has been loaded with a logged-in Campfire
	private void onLogin() {
		join();
	}
	
	// "messages" is already filled with a starting set of messages
	private void onJoined() {
		setListAdapter(new RoomAdapter(this, messages));
		scrollToBottom();
		
		if (autoPoll)
			autoPoll();
	}
	
	private void onJoined(CampfireException exception) {
		Utils.alert(this, exception);
		finish();
	}
	
	// "messages" has been populated with the latest MAX_MESSAGES messages
	private void onPoll() {
		boolean wasAtBottom = scrolledToBottom();
		int position = scrollPosition();
		
		setListAdapter(new RoomAdapter(this, messages));
		
		if (wasAtBottom)
			scrollToBottom();
		else
			scrollToPosition(position);
	}
	
	private void onSpeak(Message message) {
		body.setText("");
		speak.setEnabled(true);
		((RoomAdapter) getListAdapter()).add(message);
		
		scrollToBottom();
	}
	
	private void onSpeak(CampfireException exception) {
		speak.setEnabled(true);
		Utils.alert(this, exception);
	}
	
	private void setupControls() {
		polling = (ImageView) findViewById(R.id.room_polling);
		body = (EditText) findViewById(R.id.room_message_body);
		body.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
		speak.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				speak();
			}
		});
		
		refresh = (Button) this.findViewById(R.id.room_refresh);
		refresh.setVisibility(autoPoll ? View.GONE : View.VISIBLE);
		refresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				pollOnce();
			}
		});
	}
	
	private boolean scrolledToBottom() {
		return (getListView().getLastVisiblePosition() == (getListAdapter().getCount()-1));
	}
	
	private void scrollToBottom() {
		getListView().setSelection(getListAdapter().getCount()-1);
	}
	
	private int scrollPosition() {
		return getListView().getFirstVisiblePosition();
	}
	
	private void scrollToPosition(int position) {
		getListView().setSelection(position);
	}
	
	final Handler handler = new Handler();
	
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
			Utils.alert(RoomView.this, "Connection error.");
		}
	};
	
	private void speak() {
		String msg = body.getText().toString();
		
		if (!msg.equals("") && speakTask == null)
			new SpeakTask(this).execute(msg);
	}

	private void join() {
		if (joinTask == null)
			new JoinTask(this).execute();
	}
	
	private void pollOnce() {
		new Thread() {
			public void run() {
				handler.post(pollStart);
				try {
					messages = poll(room, users);
					
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
							messages = poll(room, users);
							
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
	
	// Fetches latest MAX_MESSAGES from the transcript, then for each message,
	// looks up the associated User to assign a display name.
	// We use the "users" HashMap to cache Users from the network. 
	private ArrayList<Message> poll(Room room, HashMap<String,User> users) throws CampfireException {
		ArrayList<Message> messages = Message.allToday(room, MAX_MESSAGES);
		int length = messages.size();
		for (int i=0; i<length; i++) {
			Message message = messages.get(i);
			if (message.user_id != null)
				fillPerson(message, users);
		}
		return messages;
	}
	
	private void fillPerson(Message message, HashMap<String,User> users) throws CampfireException {
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
		campfire = Utils.getCampfire(this);
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
    			Utils.alert(this, "You have been logged in successfully.");
    			campfire = Utils.getCampfire(this);
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
    		Utils.clearCampfire(this);
    		finish();
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    protected void loadingDialog(int id) {
    	dialog = new ProgressDialog(this);
    	dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        if (id == SPEAKING)
            dialog.setMessage("Speaking...");
        else if (id == JOINING)
        	dialog.setMessage("Joining room...");
        dialog.show();
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
	
	private class SpeakTask extends AsyncTask<String,Void,Message> {
		public RoomView context;
    	public CampfireException exception = null;
    	
    	public SpeakTask(RoomView context) {
    		super();
    		this.context = context;
    		this.context.speakTask = this;
    	}
    	 
       	@Override
    	protected void onPreExecute() {
       		context.speak.setEnabled(false);
            context.loadingDialog(SPEAKING);
    	}
    	
    	@Override
    	protected Message doInBackground(String... body) {
    		try {
    			context.room.join(); // in case we've been idle-kicked out since we last spoke
    			Message message = context.room.speak(body[0]);
    			context.fillPerson(message, context.users);
    			return message;
			} catch (CampfireException e) {
				this.exception = e;
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Message message) {
    		if (context.dialog != null && context.dialog.isShowing())
    			context.dialog.dismiss();
    		context.speakTask = null;
    		
    		if (exception == null)
    			context.onSpeak(message);
    		else
    			context.onSpeak(exception);
    	}
	}
	
	private class JoinTask extends AsyncTask<Void,Void,CampfireException> {
		public RoomView context;
    	public CampfireException exception = null;
    	
    	public ArrayList<Message> messages = null;
    	public Room room = null;
    	public HashMap<String,User> users;
    	
    	public JoinTask(RoomView context) {
    		super();
    		this.context = context;
    		this.context.joinTask = this;
    		
    		// get the current state of the user cache, so that we can write to it as we poll
    		// and then assign it back to the new context
    		// preserves caching in the case of a screen flip during this task
    		users = this.context.users;
    	}
    	 
       	@Override
    	protected void onPreExecute() {
            context.loadingDialog(JOINING);
    	}
    	
    	@Override
    	protected CampfireException doInBackground(Void... nothing) {
    		try {
    			room = Room.find(campfire, roomId);
    			room.join();
    			messages = poll(room, users);
			} catch (CampfireException e) {
				return exception;
			}
			return null;
    	}
    	
    	@Override
    	protected void onPostExecute(CampfireException exception) {
    		if (context.dialog != null && context.dialog.isShowing())
    			context.dialog.dismiss();
    		context.joinTask = null;
    		
    		context.room = room;
    		context.messages = messages;
    		context.users = users;
    		
    		if (exception == null)
    			context.onJoined();
    		else
    			context.onJoined(exception);
    	}
	}
	
	static class RoomViewHolder {
		Campfire campfire;
		Room room;
		ArrayList<Message> messages;
		HashMap<String,User> users;
		SpeakTask speakTask;
		JoinTask joinTask;
	}
}