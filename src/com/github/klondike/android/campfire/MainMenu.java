package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class MainMenu extends Activity {
	private static final int LOGGING_IN = 0;
	private static final int SPEAKING = 1;
	
	private Campfire campfire;
	private String roomId;
	private EditText message;
	private Button speak;

	private ProgressDialog loginDialog;
	private boolean spoke;
	private boolean loggedIn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadCampfire();
        loginCampfire();
        setupControls();
    }
    
    final Handler handler = new Handler();
    final Runnable updateLoggingIn = new Runnable() {
    	public void run() {
    		if (loggedIn) {
        		alert("Logged in to Campfire and joined room.");
        		speak.setEnabled(true);
    		}
    		else
    			alert("Couldn't log into Campfire. Please check your credentials.");
    		
    		dismissDialog(LOGGING_IN);
    	}
    };
    final Runnable updateLoginMessage = new Runnable() {
    	public void run() {
    		loginDialog.setMessage("Joining room...");
    	}
    };
    final Runnable updateSpeaking = new Runnable() {
        public void run() {
        	if (spoke)
        		message.setText("");
        	else
        		alert("Error sending your message, you can try again.");
        	
        	dismissDialog(SPEAKING);
        }
    };
    
    public void loginCampfire() {
    	Thread loginThread = new Thread() {
    		public void run() {
    			try {
    		        if (campfire.login()) {
    		        	handler.post(updateLoginMessage);
    		        	loggedIn = campfire.joinRoom(roomId);
    		        }	
    		        else
    		        	loggedIn = false;
    	        } catch(CampfireException e) {
    	        	loggedIn = false;
    	        }
    	        handler.post(updateLoggingIn);
    		}
    	};
    	loginThread.start();
    	
    	showDialog(LOGGING_IN);
    }
    
    public void speak() {
    	Thread speakingThread = new Thread() {
    		public void run() {
    			try {
    				spoke = campfire.speak(message.getText().toString(), roomId);
	    		} catch (CampfireException e) {
	    			spoke = false;
				}
	    		handler.post(updateSpeaking);
    		}
    	};
    	speakingThread.start();
    	
    	showDialog(SPEAKING);
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
    
    public void setupControls() {
        speak = (Button) this.findViewById(R.id.speak);
        message = (EditText) this.findViewById(R.id.message);
        
    	speak.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			speak();
    		}
    	});
    }
    
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOGGING_IN:
            loginDialog = new ProgressDialog(this);
            loginDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loginDialog.setMessage("Logging in...");
            return loginDialog;
        case SPEAKING:
            ProgressDialog speakDialog = new ProgressDialog(this);
            speakDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            speakDialog.setMessage("Speaking...");
            return speakDialog;
        default:
            return null;
        }
    }
    
    public void alert(String msg) {
		Toast.makeText(MainMenu.this, msg, Toast.LENGTH_SHORT).show();
	}
    
}