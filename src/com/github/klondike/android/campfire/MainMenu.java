package com.github.klondike.android.campfire;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

import com.github.klondike.android.campfire.R;
import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class MainMenu extends Activity {   
	private Campfire campfire;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadCampfire();
        
        TextView debug = (TextView) findViewById(R.id.debug);
        
        try {
	        if (campfire.login()) {
	        	String room_id = getResources().getString(R.string.campfire_room_id);
	        	if (campfire.joinRoom(room_id))
	        		debug.setText("Joined room!");
	        	else
	        		debug.setText("Failed to join room #" + room_id + ". :(");
	        }	
	        else
	        	debug.setText("Failed to log in");
        } catch(CampfireException e) {
        	debug.setText("Error: " + e.getMessage());
        }
    }
    
    public void loadCampfire() {
        Resources res = getResources();
        String username = res.getString(R.string.campfire_username);
        String email = res.getString(R.string.campfire_email);
        String password = res.getString(R.string.campfire_password);
        String sslString = res.getString(R.string.campfire_ssl);
        
        boolean ssl = false;
        if (sslString == "true")
        	ssl = true;
        
        campfire = new Campfire(username, email, password, ssl);
    }
}