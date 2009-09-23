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
        String output = "";
        
        try {
	        if (campfire.login()) {
	        	output += "Logged in successfully\n";
	        	String room_id = getResources().getString(R.string.campfire_room_id);
	        	if (campfire.joinRoom(room_id)) {
	        		output += "Joined room!\n";
	        		if (campfire.speak("Hello from MainMenu line 31!", room_id))
	        			output += "Spoke to room!\n";
	        		else
	        			output += "Couldn't speak to room :(\n";
	        	}
	        	else
	        		output += "Failed to join room #" + room_id + ". :(\n";
	        }	
	        else
	        	output += "Failed to log in.\n";
        } catch(CampfireException e) {
        	output += "Error: " + e.getMessage();
        }
        
        debug.setText(output);
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