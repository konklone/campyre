package com.github.klondike.android.campfire;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;
import com.github.klondike.java.campfire.Message;
import com.github.klondike.java.campfire.Room;
import com.github.klondike.java.campfire.User;

public class RoomView extends ListActivity {
	private static final int MENU_LOGOUT = 0;
	
	private static final int MAX_MESSAGES = 20;
	private static final int AUTOPOLL_INTERVAL = 15; // in seconds
	private static final int PASTE_TRUNCATE = 200;
	
	private static String timestampFormat = "hh:mm a";

	private Campfire campfire;
	private String roomId;
	private Room room;
	private SpeakTask speakTask;
	private JoinTask joinTask;
	
	private int pollFailures = 0;
	
	private ArrayList<Message> messages = new ArrayList<Message>();
	private HashMap<String,User> users = new HashMap<String,User>();
	
	private EditText body;
	private Button speak;
	private ProgressBar titleSpinner;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.room);
		
		roomId = getIntent().getStringExtra("room_id");
		
		setupControls();
		
		RoomViewHolder holder = (RoomViewHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			campfire = holder.campfire;
			room = holder.room;
			messages = holder.messages;
			users = holder.users;
			speakTask = holder.speakTask;
			joinTask = holder.joinTask;
			
			if (speakTask != null)
				speakTask.onScreenLoad(this);
			
			if (holder.joinTask != null)
				joinTask.onScreenLoad(this);
			else {
				if (campfire == null)
					verifyLogin();
				else if (room == null)
					onLogin();
				else
					onJoined();
			}
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
	
	private void onLogin() {
		join();
	}
	
	private void onJoined() {
		setWindowTitle(room.name);
		setListAdapter(new RoomAdapter(this, messages));
		scrollToBottom();

		((ProgressBar) findViewById(R.id.empty_spinner)).setVisibility(View.VISIBLE);
		((TextView) findViewById(R.id.empty_message)).setVisibility(View.VISIBLE);
		
		autoPoll();
	}
	
	private void onJoined(CampfireException exception) {
		Utils.alert(this, exception);
		finish();
	}
	
	private void onPoll() {
		boolean wasAtBottom = scrolledToBottom();
		int position = scrollPosition();
		
		setListAdapter(new RoomAdapter(this, messages));
		
		if (wasAtBottom)
			scrollToBottom();
		else
			scrollToPosition(position);
	}
	
	// polling failed, messages still has the old list
	private void onPoll(CampfireException exception) {
		messages.add(new Message(Message.UTILITY, exception.getMessage()));
		setListAdapter(new RoomAdapter(this, messages));
		scrollToBottom();
	}
	
	private void onSpeak(Message message) {
		body.setText("");
		((RoomAdapter) getListAdapter()).add(message);
		
		scrollToBottom();
	}
	
	private void onSpeak(CampfireException exception) {
		Utils.alert(this, exception);
	}
	
	private void setupControls() {
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.room_title);
		setWindowTitle(R.string.app_name);
			
		titleSpinner = (ProgressBar) findViewById(R.id.title_spinner);
		
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
			showSpinner();
		}
	};
	
	final Runnable pollSuccess = new Runnable() {
		public void run() {
			pollFailures = 0;
			
			hideSpinner();
			onPoll();
		}
	};
	
	final Runnable pollFailure = new Runnable() {
		public void run() {
			pollFailures += 1;
			hideSpinner();
			onPoll(new CampfireException("Connection error while trying to poll. (Try #" + pollFailures + ")"));
		}
	};
	
	public void showSpinner() {
		titleSpinner.setVisibility(View.VISIBLE);
	}
	
	public void hideSpinner() {
		titleSpinner.setVisibility(View.INVISIBLE);
	}
	
	private void speak() {
		String msg = body.getText().toString();
		
		if (!msg.equals("") && speakTask == null)
			new SpeakTask(this).execute(msg);
	}

	private void join() {
		if (joinTask == null)
			new JoinTask(this).execute();
	}
	
	private void autoPoll() {
		new Thread() {
			public void run() {
				while(true) {
					handler.post(pollStart);
					try {
						messages = poll(room, users);
						
						// ping the room so we don't get idle-kicked out
						room.join();
						
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
	    
        menu.add(0, MENU_LOGOUT, MENU_LOGOUT, R.string.logout)
        	.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_LOGOUT:
    		Utils.logoutCampfire(this);
    		finish();
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    private void setWindowTitle(String title) {
        ((TextView) findViewById(R.id.room_title)).setText(title);
    }
    
    private void setWindowTitle(int title) {
        ((TextView) findViewById(R.id.room_title)).setText(title);
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
			case Message.UTILITY:
				return R.layout.message_error;
			case Message.TEXT:
			case Message.PASTE:
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
			case Message.ENTRY:
				return "has entered the room";
			case Message.LEAVE:
				return "has left the room";
			case Message.TIMESTAMP:
				return new SimpleDateFormat(timestampFormat).format(message.timestamp);
			case Message.PASTE:
				int length = message.body.length();
				if (length > PASTE_TRUNCATE)
					return message.body.substring(0, PASTE_TRUNCATE-1) + "\n\n[paste truncated]";
				else
					return message.body;
			default: // all others
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
    	private ProgressDialog dialog = null;
    	
    	public SpeakTask(RoomView context) {
    		super();
    		this.context = context;
    		this.context.speakTask = this;
    	}
    	 
       	@Override
    	protected void onPreExecute() {
            loadingDialog();
    	}
       	
       	protected void onScreenLoad(RoomView context) {
       		this.context = context;
			loadingDialog();
       	}
       	
       	protected void loadingDialog() {
       		dialog = new ProgressDialog(context);
        	dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Speaking...");
            dialog.setCancelable(false);
            dialog.show();
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
    		if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
    		context.speakTask = null;
    		
    		if (exception == null)
    			context.onSpeak(message);
    		else
    			context.onSpeak(exception);
    	}
	}
	
	private class JoinTask extends AsyncTask<Void,String,CampfireException> {
		public RoomView context;
    	
    	public Room room = null;
    	public HashMap<String,User> users;
    	private ProgressDialog dialog = null;
    	
    	public JoinTask(RoomView context) {
    		super();
    		this.context = context;
    		this.context.joinTask = this;
    		
    		// get the current state of the user cache, so that we can write to it as we poll
    		// and then assign it back to the new context
    		// preserves caching in the case of a screen flip during this task
    		users = this.context.users;
    	}
    	
    	public void onScreenLoad(RoomView context) {
	    	this.context = context;
			loadingDialog();
    	}
    	 
       	@Override
    	protected void onPreExecute() {
            loadingDialog();
    	}
       	
       	protected void loadingDialog() {
        	dialog = new ProgressDialog(context);
        	dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        	dialog.setMessage("Joining room...");
            
        	dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
    			@Override
    			public void onCancel(DialogInterface dialog) {
    				cancel(true);
    				context.finish();
    			}
    		});
            
        	dialog.show();
        }
    	
    	@Override
    	protected CampfireException doInBackground(Void... nothing) {
    		try {
    			// join first, so that the logged in user shows up in the list of initial users
    			// and can be cached earlier
    			Room.joinRoom(campfire, roomId);
    			room = Room.find(campfire, roomId);
    			
    			// cache the initial users now while we can
    			if (room.initialUsers != null) {
    				int length = room.initialUsers.size();
    				for (int i=0; i<length; i++) {
    					User user = room.initialUsers.get(i);
    					users.put(user.id, user);
    				}
    			}
			} catch (CampfireException e) {
				return e;
			}
			return null;
    	}
    	
    	@Override
    	protected void onPostExecute(CampfireException exception) {
    		if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
    		context.joinTask = null;
    		
    		context.room = room;
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