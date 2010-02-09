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
import android.widget.EditText;
import campyre.java.Campfire;
import campyre.java.CampfireException;

public class Login extends Activity {
	// high number because other activities will use this code in their case statements
	public static final int RESULT_LOGIN = 1000;
	
	public static final int MENU_ABOUT = 1;
	public static final int MENU_FEEDBACK = 2;
	public static final int MENU_DONATE = 3;
	
	public static final int LOGIN_REGULAR = 1;
	public static final int LOGIN_TOKEN = 2;
	
	private int loginMode;
	private Campfire campfire;
	private EditText tokenView, subdomainView, usernameView, passwordView;
	private View regularInput, tokenInput;
	
	private LoginTask loginTask = null;
	private ProgressDialog dialog = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		
		String token = Utils.getCampfireValue(this, "token");
		if (token != null && !token.equals(""))
			loginMode = LOGIN_TOKEN;
		else
			loginMode = LOGIN_REGULAR;
		
		LoginHolder holder = (LoginHolder) getLastNonConfigurationInstance();
        if (holder != null) {
	    	campfire = holder.campfire;
	    	loginTask = holder.loginTask;
	    	loginMode = holder.loginMode;
        }
        
        setupControls();
        
        if (loginTask != null) {
    		loginTask.context = this;
    		loadingDialog();
    	}
	}
	
	@Override
    public Object onRetainNonConfigurationInstance() {
    	LoginHolder holder = new LoginHolder();
    	holder.campfire = this.campfire;
    	holder.loginTask = this.loginTask;
    	holder.loginMode = this.loginMode;
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
    	usernameView = (EditText) findViewById(R.id.username);
    	passwordView = (EditText) findViewById(R.id.password);
    	
        subdomainView.setText(Utils.getCampfireValue(this, "subdomain"));
        tokenView.setText(Utils.getCampfireValue(this, "token"));
        
        regularInput = findViewById(R.id.regular_input);
        tokenInput = findViewById(R.id.token_input);
        
        // default is already right for LOGIN_REGULAR
        if (loginMode == LOGIN_TOKEN) {
        	tokenInput.setVisibility(View.VISIBLE);
        	regularInput.setVisibility(View.GONE);
        }
        
    	findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				login();
			}
		});
    	
    	findViewById(R.id.regular_switch).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loginMode = LOGIN_TOKEN;
				regularInput.setVisibility(View.GONE);
				tokenInput.setVisibility(View.VISIBLE);
			}
		});
    	
    	findViewById(R.id.token_switch).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loginMode = LOGIN_REGULAR;
				tokenInput.setVisibility(View.GONE);
				regularInput.setVisibility(View.VISIBLE);
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
    		context.campfire = new Campfire(subdomain);
    		
    		if (context.loginMode == Login.LOGIN_TOKEN) {
	    		String token = context.tokenView.getText().toString().trim();
				context.campfire.token = token;
    		} else {
    			String username = context.usernameView.getText().toString().trim();
    			String password = context.passwordView.getText().toString().trim();
    			context.campfire.username = username;
    			context.campfire.password = password;
    		}
    		
    		// save the subdomain and token right away
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
		int loginMode;
	}
}