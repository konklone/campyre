package campyre.android;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import campyre.java.Campfire;
import campyre.java.CampfireException;

public class Login extends Activity {
	// high number because other activities will use this code in their case statements
	public static final int RESULT_LOGIN = 1000;
	
	public static final int MENU_ABOUT = 1;
	public static final int MENU_FEEDBACK = 2;
	public static final int MENU_DONATE = 3;
	
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
        subdomainView.setText(Utils.getCampfireValue(this, "subdomain"));
        tokenView.setText(Utils.getCampfireValue(this, "token"));
    	
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
	    
	    if (Utils.ASK_DONATE)
        	menu.add(0, MENU_DONATE, 0, R.string.menu_donate).setIcon(android.R.drawable.ic_menu_send);
	    menu.add(1, MENU_FEEDBACK, 1, "Feedback").setIcon(android.R.drawable.ic_menu_report_image);
        menu.add(2, MENU_ABOUT, 2, "About").setIcon(android.R.drawable.ic_menu_help);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_ABOUT:
    		showDialog(Utils.ABOUT);
    		break;
    	case MENU_FEEDBACK:
    		startActivity(Utils.feedbackIntent(this));
    		break;
    	case MENU_DONATE:
    		startActivity(Utils.donateIntent(this));
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
    		String subdomain = context.subdomainView.getText().toString().trim();
			String token = context.tokenView.getText().toString().trim();
			
			context.campfire = new Campfire(subdomain, token);
			Utils.saveCampfire(context, context.campfire);
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