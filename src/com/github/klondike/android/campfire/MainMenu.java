package com.github.klondike.android.campfire;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;

public class MainMenu extends Activity { 
	private static final int MENU_PREFS = 0;
	
	private static final int RESULT_LOGIN = 1;
	
	private Campfire campfire;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadCampfire();
    }
    
    public void onLogin() {
    	
    }
    
    public void loadCampfire() {
    	String subdomain = Preferences.getSubdomain(this);
        String email = Preferences.getEmail(this);
        String password = Preferences.getPassword(this);
        boolean ssl = Preferences.getSsl(this);
        
        SharedPreferences prefs = getSharedPreferences("campfire", 0);
        prefs.edit().putString("session", null).commit();
        
        String session = getSharedPreferences("campfire", 0).getString("session", null);
        
        campfire = new Campfire(subdomain, email, password, ssl, session);
        
        if (campfire.loggedIn())
        	alert("You are logged in.");
        else
        	startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
    }
    
    
    @Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    boolean result = super.onCreateOptionsMenu(menu);
	    
        MenuItem prefs = menu.add(0, MENU_PREFS, 0, "Preferences");
        prefs.setIcon(android.R.drawable.ic_menu_preferences);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_PREFS:
    		startActivity(new Intent(this, Preferences.class)); 
    		return true;
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    public void alert(String msg) {
		Toast.makeText(MainMenu.this, msg, Toast.LENGTH_SHORT).show();
	}
    
    
    
}