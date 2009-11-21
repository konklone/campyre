package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class Login extends Activity {
	private static final int LOGGING_IN = 1;
	
	// high because other activities will use this code in their case statements
	public static final int RESULT_LOGIN = 1000;
	
	private Campfire campfire;
	
	private EditText emailView, passwordView, subdomainView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		setupControls();
	}
	
	final Handler handler = new Handler();
    final Runnable afterLogin = new Runnable() {
    	public void run() {
    		dismissDialog(LOGGING_IN);
    		
    		if (campfire.loggedIn()) {
    			storeCredentials();
    			setResult(RESULT_OK, new Intent());
    			finish();
    		} else {
    			alert("Invalid credentials.");
    		}
    	}
    };
    
    final Runnable loginError = new Runnable() {
    	public void run() {
    		dismissDialog(LOGGING_IN);
    		alert("Error while attempting to log in, please try again.");
    	}
    };
    
    public void setupControls() {
    	emailView = (EditText) findViewById(R.id.email);
    	passwordView = (EditText) findViewById(R.id.password);
    	subdomainView = (EditText) findViewById(R.id.subdomain);
    	
    	SharedPreferences prefs = getSharedPreferences("campfire", 0); 
    	String subdomain = prefs.getString("subdomain", null);
        String email = prefs.getString("email", null);
        String password = prefs.getString("password", null);
        
        subdomainView.setText(subdomain);
        emailView.setText(email);
        passwordView.setText(password);
    	
    	Button loginButton = (Button) findViewById(R.id.login_button);
    	loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loginCampfire();
			}
		});
    }
	
	public void loginCampfire() {
    	Thread loginThread = new Thread() {
    		public void run() {
    			try {
    				String subdomain = subdomainView.getText().toString();
    				String email = emailView.getText().toString();
    				String password = passwordView.getText().toString();
    				boolean ssl = false;
    				
    				campfire = new Campfire(subdomain, email, password, ssl);
					campfire.login();
					handler.post(afterLogin);
    	        } catch(CampfireException e) {
    	        	handler.post(loginError);
    	        }
    		}
    	};
    	loginThread.start();
    	
    	showDialog(LOGGING_IN);
    }
	
	// Service to provide a Campfire loaded with stored credentials
	public static Campfire getCampfire(Context context) {
    	SharedPreferences prefs = context.getSharedPreferences("campfire", 0);
    	String subdomain = prefs.getString("subdomain", null);
        String email = prefs.getString("email", null);
        String password = prefs.getString("password", null);
        boolean ssl = prefs.getBoolean("ssl", false);
        String session = prefs.getString("session", null);
        
        return new Campfire(subdomain, email, password, ssl, session);
	}
	
	
	private void storeCredentials() {
		SharedPreferences prefs = getSharedPreferences("campfire", 0);
		Editor editor = prefs.edit();
	
		editor.putString("subdomain", campfire.subdomain);
		editor.putString("email", campfire.email);
		editor.putString("password", campfire.password);
		editor.putBoolean("ssl", campfire.ssl);
		editor.putString("session", campfire.session);
		
		editor.commit();
	}
	
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case LOGGING_IN:
            ProgressDialog loginDialog = new ProgressDialog(this);
            loginDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loginDialog.setMessage("Logging in...");
            return loginDialog;
        default:
            return null;
        }
    }
	
	public void alert(String msg) {
		Toast.makeText(Login.this, msg, Toast.LENGTH_SHORT).show();
	}
}