package com.github.klondike.android.campfire;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
	
	private static final int LOADING = 1;
	
	private Campfire campfire;
	private Room[] rooms;
	
	private boolean forResult = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_list);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        	forResult = extras.getBoolean("for_result", false);
        
        verifyLogin();
    }
    
    // will only be run after we are assured of being logged in
    public void onLogin() {
    	getRooms();
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
    
    final Handler handler = new Handler();
    final Runnable afterLoad = new Runnable() {
    	public void run() {
    		if (rooms != null) {
    			if (rooms.length <= 0)
    				((TextView) RoomList.this.findViewById(R.id.rooms_empty)).setVisibility(View.VISIBLE);
    			
	    		setListAdapter(new ArrayAdapter<Room>(RoomList.this, android.R.layout.simple_list_item_1, rooms));
	    		removeDialog(LOADING);
    		} else {
    			alert("Error connecting to Campfire. Please try again later.");
    			finish();
    		}
    	}
    };
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	Room room = (Room) parent.getItemAtPosition(position);    	
    	selectRoom(room);
    }
    
    public void getRooms() {
    	Thread loadRooms = new Thread() {
    		public void run() {
    			try {
    				rooms = campfire.getRooms();
    			} catch (CampfireException e) {
    				rooms = null;
    			}
    			handler.post(afterLoad);
    		}
    	};
    	loadRooms.start();
    	showDialog(LOADING);
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
    
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOADING:
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadingDialog.setCancelable(false);
            loadingDialog.setMessage("Loading rooms...");
            return loadingDialog;
        default:
            return null;
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
    
}