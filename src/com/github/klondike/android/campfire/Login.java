package com.github.klondike.android.campfire;

import org.json.JSONException;

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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class Login extends Activity {
	private static final int LOGGING_IN = 1;
	
	// high because other activities will use this code in their case statements
	public static final int RESULT_LOGIN = 1000;
	
	private Campfire campfire;
	private EditText tokenView, subdomainView;
	private CheckBox sslView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		setupControls();
	}
	
	final Handler handler = new Handler();
    final Runnable loginSuccess = new Runnable() {
    	public void run() {
    		dismissDialog(LOGGING_IN);
    		
    		Login.saveCampfire(Login.this, campfire);
			setResult(RESULT_OK, new Intent());
			finish();
    	}
    };
    
    final Runnable loginFailure = new Runnable() {
    	public void run() {
    		dismissDialog(LOGGING_IN);
    		alert("Invalid credentials.");
    	}
    };
    
    final Runnable loginError = new Runnable() {
    	public void run() {
    		dismissDialog(LOGGING_IN);
    		alert("Error while attempting to log in, please try again.");
    	}
    };
    
    public void setupControls() {
    	tokenView = (EditText) findViewById(R.id.token);
    	subdomainView = (EditText) findViewById(R.id.subdomain);
    	sslView = (CheckBox) findViewById(R.id.ssl);
    	
    	SharedPreferences prefs = getSharedPreferences("campfire", 0); 
    	String subdomain = prefs.getString("subdomain", null);
        String token = prefs.getString("token", null);
        
        subdomainView.setText(subdomain);
        tokenView.setText(token);
    	
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
    				String token = tokenView.getText().toString();
    				boolean ssl = sslView.isChecked();
    				
    				campfire = new Campfire(subdomain, token, ssl);
					if (campfire.login())
						handler.post(loginSuccess);
					else
						handler.post(loginFailure);
    	        } catch(CampfireException e) {
    	        	handler.post(loginError);
    	        } catch(JSONException e) {
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
        String token = prefs.getString("token", null);
        boolean ssl = prefs.getBoolean("ssl", false);
        String user_id = prefs.getString("user_id", null);
        
        if (token != null)
        	return new Campfire(subdomain, token, ssl, user_id);
        else
        	return null;
	}
	
	public static void saveCampfire(Context context, Campfire campfire) {
		SharedPreferences prefs = context.getSharedPreferences("campfire", 0);
		Editor editor = prefs.edit();
	
		editor.putString("subdomain", campfire.subdomain);
		editor.putString("token", campfire.token);
		editor.putBoolean("ssl", campfire.ssl);
		editor.putString("user_id", campfire.user_id);
		
		editor.commit();
	}
	
	public static void clearCampfire(Context context) {
		SharedPreferences prefs = context.getSharedPreferences("campfire", 0);
		Editor editor = prefs.edit();
	
		editor.putString("subdomain", null);
		editor.putString("token", null);
		editor.putBoolean("ssl", false);
		editor.putString("user_id", null);
		
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