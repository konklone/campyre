package com.github.klondike.android.campfire;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;
import com.github.klondike.java.campfire.Room;

public class RoomList extends ListActivity { 
	private static final int MENU_CLEAR = 0;
	
	private Campfire campfire = null;
	private Room[] rooms = null;
	
	private LoadRoomsTask loadRoomsTask = null;
	private ProgressDialog dialog = null;
	
	private boolean forResult = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_list);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        	forResult = extras.getBoolean("for_result", false);
        
        RoomListHolder holder = (RoomListHolder) getLastNonConfigurationInstance();
        if (holder != null) {
	    	rooms = holder.rooms;
	    	loadRoomsTask = holder.loadRoomsTask;
	    	if (loadRoomsTask != null) {
	    		loadRoomsTask.context = this;
	    		loadingDialog();
	    	}
        }
        
        verifyLogin();
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	RoomListHolder holder = new RoomListHolder();
    	holder.rooms = this.rooms;
    	holder.loadRoomsTask = this.loadRoomsTask;
    	return holder;
    }
    
    // will only be run after we are assured of being logged in
    public void onLogin() {
    	loadRooms();
    }
    
    public void selectRoom(Room room) {
    	if (forResult) {
    		Intent intent = new Intent();
    		Bundle extras = new Bundle();
        	extras.putString("room_id", room.id);
        	intent.putExtras(extras);
        	
        	setResult(RESULT_OK, intent);
        	finish();
    	} else {
    		Intent intent = new Intent(Intent.ACTION_MAIN);
    		intent.setClassName("com.github.klondike.android.campfire", "com.github.klondike.android.campfire.RoomView");
        	Bundle extras = new Bundle();
        	extras.putString("room_id", room.id);
        	intent.putExtras(extras);
        	
        	startActivity(intent);
    	}
    }
    
    public void displayRooms() {
    	if (rooms.length <= 0)
			((TextView) findViewById(R.id.rooms_empty)).setVisibility(View.VISIBLE);
		setListAdapter(new ArrayAdapter<Room>(RoomList.this, android.R.layout.simple_list_item_1, rooms));
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	//selectRoom((Room) parent.getItemAtPosition(position));
    }
    
    public void loadRooms() {
    	// if a LoadRoomsTask is running, and we've updated its context to be our instance in onCreate,
    	// then we can trust its onPostExecute to load the rooms when it's done
    	if (loadRoomsTask == null) {
	    	if (rooms == null)
		    	new LoadRoomsTask(this).execute();
	    	else
	    		displayRooms();
    	}
    }
    
    public void verifyLogin() {
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
	    
        menu.add(0, MENU_CLEAR, 0, "Clear Credentials").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_CLEAR:
    		Login.clearCampfire(this);
    		finish();
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    public void alert(String msg) {
		Toast.makeText(RoomList.this, msg, Toast.LENGTH_SHORT).show();
	}
    
    public void loadingDialog() {
    	dialog = new ProgressDialog(RoomList.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Loading rooms...");
        dialog.show();
    }
    
    private class LoadRoomsTask extends AsyncTask<Void,Void,Room[]> {
    	public RoomList context;
    	
    	public LoadRoomsTask(RoomList context) {
    		super();
    		
    		// link the task to the context
    		this.context = context;
    		this.context.loadRoomsTask = this;
    	}
    	 
       	@Override
    	protected void onPreExecute() {
            context.loadingDialog();
    	}
    	
    	@Override
    	protected Room[] doInBackground(Void... nothing) {
    		try {
				return context.campfire.getRooms();
			} catch (CampfireException e) {
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Room[] foundRooms) {
    		if (foundRooms != null) {
    			context.rooms = foundRooms;
            	context.displayRooms();
            	if (context.dialog != null && context.dialog.isShowing())
        			context.dialog.dismiss();
        		context.loadRoomsTask = null;
    		} else {
    			context.alert("Error connecting to Campfire. Please try again later.");
    			context.finish();
    		}
    	}
    }
    
    static class RoomListHolder {
    	Room[] rooms;
    	LoadRoomsTask loadRoomsTask;
    }
    
}