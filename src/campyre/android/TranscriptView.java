package campyre.android;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import campyre.java.Campfire;
import campyre.java.CampfireException;
import campyre.java.Message;
import campyre.java.Room;
import campyre.java.User;

public class TranscriptView extends ListActivity {
	private Campfire campfire;
	private Room room;
	private ArrayList<Message> messages;
	
	private LoadTranscriptTask loadTranscriptTask;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.transcript);
		
		campfire = Utils.getCampfire(this);
		room = new Room(campfire, getIntent().getStringExtra("room_id"));
		
		TranscriptViewHolder holder = (TranscriptViewHolder) getLastNonConfigurationInstance();
		if (holder != null) {
			this.messages = holder.messages;
			this.loadTranscriptTask = holder.loadTranscriptTask;
		}
		
		loadTranscripts();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new TranscriptViewHolder(messages, loadTranscriptTask);
	}
	
	public void loadTranscripts() {
		if (messages != null) 
			displayTranscript();
		else {
			if (loadTranscriptTask == null)
				loadTranscriptTask = (LoadTranscriptTask) new LoadTranscriptTask(this).execute();
			else
				loadTranscriptTask.onScreenLoad(this);
		}
	}
	
	public void onLoadTranscripts(ArrayList<Message> messages) {
		this.messages = messages;
		displayTranscript();
	}
	
	public void onLoadTranscripts(CampfireException exception) {
		Utils.alert(this, exception);
	}
	
	public void displayTranscript() {
		setListAdapter(new MessageAdapter(this, messages));
	}
	
	private class LoadTranscriptTask extends AsyncTask<Void,Void,ArrayList<Message>> {
		public TranscriptView context;
    	public CampfireException exception = null;
    	HashMap<String,User> users;
    	
    	public LoadTranscriptTask(TranscriptView context) {
    		super();
    		this.context = context;
    		this.users = new HashMap<String,User>();
    	}
    	
    	protected void onScreenLoad(TranscriptView context) {
       		this.context = context;
       	}
    	
    	@Override
    	protected ArrayList<Message> doInBackground(Void... nothing) {
    		
    		try {
				ArrayList<Message> messages = Message.allToday(context.room);
				
				int length = messages.size();
				for (int i=0; i<length; i++) {
					Message message = messages.get(i);
					if (message.user_id != null)
						fillPerson(message);
				}
				return messages;
    		} catch (CampfireException e) {
    			this.exception = e;
    			return null;
    		}
    	}
    	
    	@Override
    	protected void onPostExecute(ArrayList<Message> messages) {
    		context.loadTranscriptTask = null;
    		
    		if (exception == null)
    			context.onLoadTranscripts(messages);
    		else
    			context.onLoadTranscripts(exception);
    	}
    	
    	private void fillPerson(Message message) throws CampfireException {
    		User speaker;
			if (users.containsKey(message.user_id))
				speaker = (User) users.get(message.user_id);
			else {
				speaker = User.find(context.campfire, message.user_id);
				users.put(message.user_id, speaker);
			}
			message.person = speaker.displayName();
    	}
	}
	
	static class TranscriptViewHolder {
		ArrayList<Message> messages;
		LoadTranscriptTask loadTranscriptTask;
		
		public TranscriptViewHolder(ArrayList<Message> messages, LoadTranscriptTask loadTranscriptTask) {
			this.messages = messages;
			this.loadTranscriptTask = loadTranscriptTask;
		}
	}
}