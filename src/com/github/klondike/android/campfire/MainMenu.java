package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;
import com.github.klondike.java.campfire.Room;

public class MainMenu extends Activity { 
	private static final int LOGGING_IN = 0;
	private static final int SPEAKING = 1;
	
	private static final int PREFS = 0;
	private static final int LOGOUT = 1;
	private static final int LOGIN = 2;
	
	
	private Campfire campfire;
	private Room room;
	private EditText message;
	private Button speak;

	private ProgressDialog loginDialog;
	private boolean spoke;
	private boolean joined;
	private String roomId;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupControls();
        
        loginCampfire();
    }
    
    final Handler handler = new Handler();
    final Runnable afterLogin = new Runnable() {
    	public void run() {
    		if (campfire.session != null && joined) {
        		alert("Logged in to Campfire and joined room.");
        		speak.setEnabled(true);
    		}
    		else if (campfire.session == null)
    			alert("Couldn't log into Campfire. Please check your credentials.");
    		else
    			alert("Logged in, but couldn't join the room. Please try again.");
    		
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
    	loadCampfire();
    	
    	if (campfire.subdomain == null)
    		return;
    	
    	if (campfire.session != null) {
    		speak.setEnabled(true);
    		return;
    	}
    	
    	Thread loginThread = new Thread() {
    		public void run() {
    			try {
					String session = campfire.login();
    		        if (session != null) {
    		        	storeSession(session);
    		        	handler.post(updateLoginMessage);
    		        	room = new Room(campfire, roomId);
    		        	joined = room.join();
    		        }
    	        } catch(CampfireException e) {}
    	        handler.post(afterLogin);
    		}
    	};
    	loginThread.start();
    	
    	showDialog(LOGGING_IN);
    }
    
    public void speak() {
    	Thread speakingThread = new Thread() {
    		public void run() {
    			try {
    				spoke = room.speak(message.getText().toString());
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
    	String subdomain = Preferences.getSubdomain(this);
        String email = Preferences.getEmail(this);
        String password = Preferences.getPassword(this);
        boolean ssl = Preferences.getSsl(this);
        
        campfire = new Campfire(subdomain, email, password, ssl);
        campfire.session = getSharedPreferences("campfire", 0).getString("session", null);
        
        roomId = Preferences.getRoomId(this);
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
    
    @Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    boolean result = super.onCreateOptionsMenu(menu);
	    
        MenuItem prefs = menu.add(0, PREFS, 0, "Preferences");
        prefs.setIcon(android.R.drawable.ic_menu_preferences);
        
        if (campfire.session != null) {
        	MenuItem logout = menu.add(0, LOGOUT, 1, "Log Out");
        	logout.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        } else {
        	MenuItem login = menu.add(0, LOGIN, 1, "Log In");
        	login.setIcon(android.R.drawable.ic_menu_view);
        }
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case PREFS:
    		startActivity(new Intent(this, Preferences.class)); 
    		return true;
    	case LOGOUT:
    		campfire.session = null;
    		storeSession(null);
    		finish();
    		return true;
    	case LOGIN:
    		loginCampfire();
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    public void alert(String msg) {
		Toast.makeText(MainMenu.this, msg, Toast.LENGTH_SHORT).show();
	}
    
    public void storeSession(String session) {
    	SharedPreferences prefs = getSharedPreferences("campfire", 0);
    	prefs.edit().putString("session", session).commit();
    }
    
}