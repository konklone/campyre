package com.github.klondike.android.campfire;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.klondike.android.campfire.R;
import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class MainMenu extends Activity {   
	private Campfire campfire;
	private String roomId;
	private TextView out;
	private EditText message;
	private Button speak;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadCampfire();
        setupControls();
        
        String output = "";
        try {
	        if (campfire.login()) {
	        	output += "Logged in successfully\n";
	        	if (campfire.joinRoom(roomId))
	        		output += "Joined room!\n";
	        	else
	        		output += "Failed to join room. :(\n";
	        }	
	        else
	        	output += "Failed to log in. :(\n";
        } catch(CampfireException e) {
        	output += "Error: " + e.getMessage();
        }
        
        out.setText(output);
    }
    
    public void setupControls() {
    	out = (TextView) findViewById(R.id.debug);
        speak = (Button) this.findViewById(R.id.speak);
        message = (EditText) this.findViewById(R.id.message);
        
    	speak.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			try {
	    			if (campfire.speak(message.getText().toString(), roomId))
	        			out.setText("Spoke to room!\n");
	        		else
	        			out.setText("Didn't speak to room :(\n");
    			} catch (CampfireException e) {
    				out.setText("Error speaking to room :(\n");
    			}
    			message.setText("");
    		}
    	});
    }
    
    public void loadCampfire() {
        Resources res = getResources();
        String username = res.getString(R.string.campfire_username);
        String email = res.getString(R.string.campfire_email);
        String password = res.getString(R.string.campfire_password);
        String sslString = res.getString(R.string.campfire_ssl);
        roomId = res.getString(R.string.campfire_room_id);
        
        boolean ssl = false;
        if (sslString == "true")
        	ssl = true;
        
        campfire = new Campfire(username, email, password, ssl);
    }
}