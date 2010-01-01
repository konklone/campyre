package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class Login extends Activity {
	// high number because other activities will use this code in their case statements
	public static final int RESULT_LOGIN = 1000;
	
	private Campfire campfire;
	private EditText tokenView, subdomainView;
	
	private LoginTask loginTask = null;
	private ProgressDialog dialog = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		setupControls();
		
		LoginHolder holder = (LoginHolder) getLastNonConfigurationInstance();
        if (holder != null) {
	    	campfire = holder.campfire;
	    	loginTask = holder.loginTask;
	    	if (loginTask != null) {
	    		loginTask.context = this;
	    		loadingDialog();
	    	}
        }
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
    	LoginHolder holder = new LoginHolder();
    	holder.campfire = this.campfire;
    	holder.loginTask = this.loginTask;
    	return holder;
    }
	
	public void login() {
		if (loginTask == null)
        	new LoginTask(this).execute();
	}
	
	public void onLogin(boolean loggedIn) {
		if (loggedIn) {
			Utils.saveCampfire(this, campfire);
			setResult(RESULT_OK, new Intent());
			finish();
		} else 
			Utils.alert(this, "Invalid credentials.");
    };
    
    public void onLogin(CampfireException exception) {
    	Utils.alert(this, exception);
    }
    
    public void setupControls() {
    	tokenView = (EditText) findViewById(R.id.token);
    	subdomainView = (EditText) findViewById(R.id.subdomain);
    	
    	SharedPreferences prefs = getSharedPreferences("campfire", 0); 
    	String subdomain = prefs.getString("subdomain", null);
        String token = prefs.getString("token", null);
        
        subdomainView.setText(subdomain);
        tokenView.setText(token);
    	
    	Button loginButton = (Button) findViewById(R.id.login_button);
    	loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				login();
			}
		});
    }
	
	public void loadingDialog() {
       dialog = new ProgressDialog(this);
       dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
       dialog.setMessage("Logging in...");
       dialog.setCancelable(false);
       dialog.show();
    }
	
	private class LoginTask extends AsyncTask<Void,Void,Boolean> {
		public Login context;
    	public CampfireException exception = null;
    	
    	public LoginTask(Login context) {
    		super();
    		this.context = context;
    		this.context.loginTask = this;
    	}
    	 
       	@Override
    	protected void onPreExecute() {
            context.loadingDialog();
    	}
    	
    	@Override
    	protected Boolean doInBackground(Void... nothing) {
    		String subdomain = subdomainView.getText().toString();
			String token = tokenView.getText().toString();
			
			campfire = new Campfire(subdomain, token);
			try {
				return new Boolean(campfire.login());
			} catch (CampfireException e) {
				this.exception = e;
				return new Boolean(false);
			}
    	}
    	
    	@Override
    	protected void onPostExecute(Boolean result) {
    		if (context.dialog != null && context.dialog.isShowing())
    			context.dialog.dismiss();
    		context.loginTask = null;
    		
    		if (exception == null)
    			context.onLogin(result.booleanValue());
    		else
    			context.onLogin(exception);
    	}
	}
	
	static class LoginHolder {
		Campfire campfire;
		LoginTask loginTask;
	}
}