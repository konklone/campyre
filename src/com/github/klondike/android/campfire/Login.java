package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class Login extends Activity {
	// high number because other activities will use this code in their case statements
	public static final int RESULT_LOGIN = 1000;
	
	public static final int MENU_ABOUT = 1;
	
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
	
	public void onLogin(CampfireException exception) {
		if (exception == null) {
			Utils.saveCampfire(this, campfire);
			setResult(RESULT_OK, new Intent());
			finish();
		} else 
			Utils.alert(this, exception);
    };
    
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
    
    @Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    boolean result = super.onCreateOptionsMenu(menu);
	    
	    // add Feedback item here
        menu.add(0, MENU_ABOUT, 0, "About").setIcon(android.R.drawable.ic_menu_help);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_ABOUT:
    		showDialog(Utils.ABOUT);
    		break;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
	protected Dialog onCreateDialog(int id) { 
		return id == Utils.ABOUT ? Utils.aboutDialog(this) : null;
	}
	
	public void loadingDialog() {
		dialog = new ProgressDialog(this);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setMessage("Logging in...");
		   
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (loginTask != null) {
					loginTask.cancel(true);
					loginTask = null; // so that the button will work again
				}
			}
		});
       
       dialog.show();
    }
	
	private class LoginTask extends AsyncTask<Void,Void,CampfireException> {
		public Login context;
    	
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
    	protected CampfireException doInBackground(Void... nothing) {
    		String subdomain = context.subdomainView.getText().toString();
			String token = context.tokenView.getText().toString();
			
			context.campfire = new Campfire(subdomain, token);
			try {
				context.campfire.login();
			} catch (CampfireException exception) {
				return exception;
			}
			return null;
    	}
    	
    	@Override
    	protected void onPostExecute(CampfireException exception) {
    		if (context.dialog != null && context.dialog.isShowing())
    			context.dialog.dismiss();
    		context.loginTask = null;
    		
    		context.onLogin(exception);
    	}
	}
	
	static class LoginHolder {
		Campfire campfire;
		LoginTask loginTask;
	}
}