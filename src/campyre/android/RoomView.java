package campyre.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import campyre.java.Campfire;
import campyre.java.CampfireException;
import campyre.java.Message;
import campyre.java.Room;
import campyre.java.User;

public class RoomView extends ListActivity {
	private static final int MENU_SETTINGS = 0;
	private static final int MENU_LOGOUT = 1;
	
	private static final int AUTOPOLL_INTERVAL = 5; // in seconds
	private static final long JOIN_TIMEOUT = 60; // in seconds
	
	private Campfire campfire;
	private String roomId;
	private Room room;
	
	private HashMap<String,SpeakTask> speakTasks = new HashMap<String,SpeakTask>();
	private LoadRoomTask loadRoomTask;
	private PollTask pollTask;
	
	private int transitId = 1;
	private long lastJoined = 0;
	
	private ArrayList<Message> messages = new ArrayList<Message>();
	private HashMap<String,Message> transitMessages = new HashMap<String,Message>();
	private Message errorMessage;
	
	private HashMap<String,User> users = new HashMap<String,User>();
	
	private EditText body;
	private Button speak;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room_view);
		
		Bundle extras = getIntent().getExtras();
		roomId = extras.getString("room_id"); // will always be set
		room = (Room) extras.getSerializable("room"); // may be null
		
		setupControls();
		
		if (savedInstanceState != null) {
			transitId = savedInstanceState.getInt("transitId");
			lastJoined = savedInstanceState.getLong("lastJoined");
		}
		
		RoomViewHolder holder = (RoomViewHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			campfire = holder.campfire;
			room = holder.room;
			messages = holder.messages;
			transitMessages = holder.transitMessages;
			errorMessage = holder.errorMessage;
			users = holder.users;
			speakTasks = holder.speakTasks;
			loadRoomTask = holder.loadRoomTask;
			pollTask = holder.pollTask;
		}
		
		if (speakTasks != null) {
			Iterator<SpeakTask> iterator = speakTasks.values().iterator();
			while (iterator.hasNext())
				iterator.next().onScreenLoad(this);
		}
		
		if (pollTask != null)
			pollTask.onScreenLoad(this);
		
		if (loadRoomTask != null)
			loadRoomTask.onScreenLoad(this);
		
		verifyLogin();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		RoomViewHolder holder = new RoomViewHolder();
		holder.campfire = this.campfire;
		holder.room = this.room;
		holder.messages = this.messages;
		holder.transitMessages = this.transitMessages;
		holder.errorMessage = this.errorMessage;
		holder.users = this.users;
		holder.speakTasks = this.speakTasks;
		holder.loadRoomTask = this.loadRoomTask;
		holder.pollTask = this.pollTask;
		return holder;
	}
	
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt("transitId", transitId);
		outState.putLong("lastJoined", lastJoined);
		super.onSaveInstanceState(outState);
	}
	
	private void onLogin() {
		loadRoom();
	}
	
	private void onRoomLoaded() {
		updateMessages();
		scrollToBottom();
		
		body.setFocusableInTouchMode(true);
		body.setEnabled(true);
		speak.setEnabled(true);
		((TextView) findViewById(R.id.empty_message)).setText(R.string.loading_messages);
		
		startPoll();
	}
	
	private void onRoomLoaded(CampfireException exception) {
		Utils.alert(this, exception);
		finish();
	}
	
	private void onPoll(ArrayList<Message> messages) {
		this.messages = messages;
		errorMessage = null;
		
		boolean wasAtBottom = scrolledToBottom();
		int position = scrollPosition();
		
		// one-way, since no other "Loading..." messages will be shown after this.
		if (messages.size() == 0) {
			findViewById(R.id.empty_spinner).setVisibility(View.GONE);
			((TextView) findViewById(R.id.empty_message)).setText(R.string.no_messages);
		}
		
		updateMessages();
		
		if (wasAtBottom)
			scrollToBottom();
		else
			scrollToPosition(position);
	}
	
	// polling failed, messages still has the old list
	private void onPoll(CampfireException exception) {
		errorMessage = new Message("error", Message.ERROR, exception.getMessage());
		updateMessages();
		scrollToBottom();
	}
	
	private void onSpeak(Message message, String transitId) {
		transitMessages.remove(transitId);
		messages.add(message);
		updateMessages();
		scrollToBottom();
	}
	
	private void onSpeak(CampfireException exception) {
		Utils.alert(this, exception);
	}
	
	private void updateMessages() {
		ArrayList<Message> allMessages = new ArrayList<Message>();
		allMessages.addAll(messages);
		allMessages.addAll(transitMessages.values());
		if (errorMessage != null)
			allMessages.add(errorMessage);
		
		setListAdapter(new MessageAdapter(this, allMessages));
	}
	
	private void setupControls() {
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.room_title);
		
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
	
	private void speak() {
		String msg = body.getText().toString();
		
		if (!msg.equals("")) {
			body.setText("");
			
			String id = transitId + "-" + campfire.user_id;
			transitId += 1;
			Message message = new Message(id, Message.TRANSIT, msg);
			transitMessages.put(id, message);
			
			// avoid refreshing the whole adapter if I don't have to
			((MessageAdapter) getListAdapter()).add(message);
			scrollToBottom();
			
			// actually do the speaking in the background
			new SpeakTask(this, message).execute();
		}
	}

	private void loadRoom() {
		if (room != null)
			onRoomLoaded();
		else {
			if (loadRoomTask == null)
				new LoadRoomTask(this).execute();
		}
	}
	
	private void startPoll() {
		if (pollTask == null)
			pollTask = (PollTask) new PollTask(this).execute();
	}
	
	// Fetches latest MAX_MESSAGES from the transcript, then for each message,
	// looks up the associated User to assign a display name.
	// We use the "users" HashMap to cache Users from the network. 
	private ArrayList<Message> poll(Room room, HashMap<String,User> users) throws CampfireException {
		int maxMessages = Utils.getIntPreferenceFromString(this, Settings.KEY_NUMBER_MESSAGES, Settings.DEFAULT_NUMBER_MESSAGES);
		
		if (maxMessages < 1) // sanity check for this value
			maxMessages = Settings.DEFAULT_NUMBER_MESSAGES;
			
		ArrayList<Message> messages = Message.allToday(room, maxMessages);
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
		if (campfire != null) 
			onLogin();
		else {
			campfire = Utils.getCampfire(this);
	        if (campfire != null)
	        	onLogin();
	        else
	        	startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
		} 
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
	    
	    menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings)
	    	.setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(1, MENU_LOGOUT, 1, R.string.logout)
        	.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case MENU_SETTINGS:
    		startActivity(new Intent(this, Settings.class));
    		break;
    	case MENU_LOGOUT:
    		Utils.logoutCampfire(this);
    		finish();
    		break;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    private boolean shouldJoin() {
    	return (System.currentTimeMillis() - lastJoined) > (JOIN_TIMEOUT * 1000);
    }
    
    
    private class PollTask extends AsyncTask<Void,ArrayList<Message>,Integer> {
    	public RoomView context;
    	public CampfireException exception = null;
    	private int pollFailures = 0;
    	
    	public PollTask(RoomView context) {
    		super();
    		this.context = context;
    	}
    	
    	protected void onScreenLoad(RoomView context) {
       		this.context = context;
       	}
    	
    	@Override
    	protected Integer doInBackground(Void... nothing) {
    		new Thread() {
    			
    			@SuppressWarnings("unchecked") // for the autocasting to publishProgress
				public void run() {
		    		while(true) {
						try {
							publishProgress(context.poll(context.room, context.users));
							
							// ping the room so we don't get idle-kicked out
							if (context.shouldJoin()) {
								context.room.join();
								context.lastJoined = System.currentTimeMillis();
							}
						} catch(CampfireException e) {
							exception = e;
							publishProgress((ArrayList<Message>) null);
						}
						
						try {
							sleep(AUTOPOLL_INTERVAL * 1000);
						} catch(InterruptedException ex) {
							// well, I never
						}
					}
    			}
    		}.start();
    		return -1; // Integer instead of Void, to avoid compiler errors in Eclipse
    	}
    	
    	@Override
    	public void onProgressUpdate(ArrayList<Message>... messages) {
    		if (exception == null) {
    			pollFailures = 0;
    			context.onPoll(messages[0]);
    		} else {
    			pollFailures += 1;
    			context.onPoll(new CampfireException(exception, "Connection error while trying to poll. (Try #" + pollFailures + ")"));
    		}
    	}
	};
	
	private class SpeakTask extends AsyncTask<Void,Void,Message> {
		public RoomView context;
    	public CampfireException exception = null;
    	private ProgressDialog dialog = null;
    	private Message transitMessage;
    	
    	public SpeakTask(RoomView context, Message transitMessage) {
    		super();
    		this.context = context;
    		this.context.speakTasks.put(transitMessage.id, this);
    		this.transitMessage = transitMessage;
    	}
       	
       	protected void onScreenLoad(RoomView context) {
       		this.context = context;
       	}
       	
    	@Override
    	protected Message doInBackground(Void... nothing) {
    		try {
    			// in case we've been idle-kicked out since we last spoke
    			if (context.shouldJoin()) { 
    				context.room.join();
    				context.lastJoined = System.currentTimeMillis();
    			}
    			
    			Message newMessage = context.room.speak(transitMessage.body);
    			context.fillPerson(newMessage, context.users);
    			return newMessage;
			} catch (CampfireException e) {
				this.exception = e;
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Message newMessage) {
    		if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
    		context.speakTasks.remove(transitMessage.id);
    		
    		if (exception == null)
    			context.onSpeak(newMessage, transitMessage.id);
    		else
    			context.onSpeak(exception);
    	}
	}
	
	private class LoadRoomTask extends AsyncTask<Void,String,CampfireException> {
		public RoomView context;
    	
    	public Room room = null;
    	public HashMap<String,User> users;
    	
    	public LoadRoomTask(RoomView context) {
    		super();
    		this.context = context;
    		this.context.loadRoomTask = this;
    		
    		// get the current state of the user cache, so that we can write to it as we poll
    		// and then assign it back to the new context
    		// preserves caching in the case of a screen flip during this task
    		users = this.context.users;
    	}
    	
    	public void onScreenLoad(RoomView context) {
	    	this.context = context;
    	}
    	 
       	@Override
    	protected CampfireException doInBackground(Void... nothing) {
    		try {
    			room = Room.find(context.campfire, context.roomId);
    			
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
    		context.loadRoomTask = null;
    		
    		context.room = room;
    		context.users = users;
    		   		
    		if (exception == null)
    			context.onRoomLoaded();
    		else
    			context.onRoomLoaded(exception);
    	}
	}
	
	static class RoomViewHolder {
		Campfire campfire;
		Room room;
		ArrayList<Message> messages;
		HashMap<String,Message> transitMessages;
		Message errorMessage;
		HashMap<String,User> users;
		HashMap<String,SpeakTask> speakTasks;
		LoadRoomTask loadRoomTask;
		PollTask pollTask;
	}
}
