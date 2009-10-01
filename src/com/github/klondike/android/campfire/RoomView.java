package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.Room;

public class RoomView extends Activity {
	private static final int MENU_PREFS = 0;
	private static final int MENU_LOGOUT = 1;
	
	private static final int SPEAKING = 1;

	private Campfire campfire;
	private String roomId;
	private Room room;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		roomId = getIntent().getStringExtra("room_id");
		
		verifyLogin();
	}
	
	private void onLogin() {
		joinRoom();
	}

	private void joinRoom() {
		
	}
	
	public void verifyLogin() {
		campfire = Login.getCampfire(this);
        if (campfire.loggedIn())
        	onLogin();
        else
        	startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
    }
	
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case SPEAKING:
		    ProgressDialog speakDialog = new ProgressDialog(this);
		    speakDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    speakDialog.setMessage("Speaking...");
		    return speakDialog;
		default:
			return null;
        }
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
		Toast.makeText(RoomView.this, msg, Toast.LENGTH_SHORT).show();
	}
}