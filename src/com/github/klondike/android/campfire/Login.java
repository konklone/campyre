package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class Login extends Activity {
	private static final int LOGGING_IN = 1;
	
	public static final int RESULT_LOGIN = 1;
	
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
    		// after login
    		dismissDialog(LOGGING_IN);
    	}
    };
    
    public void setupControls() {
    	emailView = (EditText) findViewById(R.id.email);
    	passwordView = (EditText) findViewById(R.id.password);
    	subdomainView = (EditText) findViewById(R.id.subdomain);
    }
	
	public void loginCampfire() {
    	Thread loginThread = new Thread() {
    		public void run() {
    			try {
					campfire.login();
    	        } catch(CampfireException e) {}
    	        handler.post(afterLogin);
    		}
    	};
    	loginThread.start();
    	
    	showDialog(LOGGING_IN);
    }
	
	
//	private void storeSession(String session) {
//    	SharedPreferences prefs = getSharedPreferences("campfire", 0);
//    	prefs.edit().putString("session", session).commit();
//    }
	
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
}