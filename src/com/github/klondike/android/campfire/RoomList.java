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
	private static final int MENU_PREFS = 0;
	private static final int MENU_LOGOUT = 1;
	
	private Campfire campfire = null;
	private Room[] rooms = null;
	
	private LoadRoomsTask loadRoomsTask = null;
	private ProgressDialog dialog;
	
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
//	    	if (loadRoomsTask != null)
//	    		loadRoomsTask.context = this;
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
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	if (dialog != null && dialog.isShowing())
    		dialog.dismiss();
    	super.onSaveInstanceState(state);
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
    	Room room = (Room) parent.getItemAtPosition(position);    	
    	selectRoom(room);
    }
    
    public void loadRooms() {
//    	if (loadRoomsTask == null) {
	    	if (rooms == null)
		    	new LoadRoomsTask().execute();
	    	else
	    		displayRooms();
//    	}
    }
    
    public void verifyLogin() {
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
	    
        menu.add(0, MENU_PREFS, 0, "Preferences").setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, MENU_LOGOUT, 0, "Log Out").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_PREFS:
    		startActivity(new Intent(this, Preferences.class)); 
    		return true;
    	case MENU_LOGOUT:
    		getSharedPreferences("campfire", 0).edit().putString("session", null).commit();
    		finish();
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    public void alert(String msg) {
		Toast.makeText(RoomList.this, msg, Toast.LENGTH_SHORT).show();
	}
    
    private class LoadRoomsTask extends AsyncTask<RoomList,Void,Room[]> {
    	//public RoomList context;
    	
       	@Override
    	protected void onPreExecute() {
            dialog = new ProgressDialog(RoomList.this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(false);
            dialog.setMessage("Loading rooms...");
            dialog.show();
    	}
    	
    	@Override
    	protected Room[] doInBackground(RoomList... originalContext) {
    		//context = originalContext[0];
    		
    		try {
				return campfire.getRooms();
			} catch (CampfireException e) {
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Room[] foundRooms) {
    		dialog.dismiss();
    		
    		if (foundRooms != null) {
    			rooms = foundRooms;
            	displayRooms();
    		} else {
    			alert("Error connecting to Campfire. Please try again later.");
    			finish();
    		}
    	}
    }
    
    static class RoomListHolder {
    	Room[] rooms;
    	LoadRoomsTask loadRoomsTask;
    }
    
}