package campyre.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import campyre.java.Campfire;
import campyre.java.CampfireException;

public class Utils {
	public static final int ABOUT = 0;
	
	// change this to false for the donate version
	public static final boolean ASK_DONATE = true;
	
	// change this to icon_donate for the donate version
	public static final int SHORTCUT_ICON = R.drawable.icon; 
	
	public static void alert(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
    
    public static void alert(Context context, CampfireException exception) {
    	String message = exception == null ? "Unhandled error." : exception.getMessage();
    	Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
    public static Dialog aboutDialog(Context context) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	LayoutInflater inflater = LayoutInflater.from(context);
    	
    	ScrollView aboutView = (ScrollView) inflater.inflate(R.layout.about, null);
    	
    	TextView about3 = (TextView) aboutView.findViewById(R.id.about_links);
    	about3.setText(R.string.about_links);
    	Linkify.addLinks(about3, Linkify.WEB_URLS);
    	
    	String versionString = context.getResources().getString(R.string.version_string);
    	((TextView) aboutView.findViewById(R.id.about_version)).setText("Version " + versionString);
    	
    	builder.setView(aboutView);
    	builder.setPositiveButton(R.string.about_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {}
		});
        return builder.create();
    }
    
    public static Intent feedbackIntent(Context context) {
    	return new Intent(Intent.ACTION_SENDTO, 
    			Uri.fromParts("mailto", context.getResources().getString(R.string.contact_email), null))
    		.putExtra(Intent.EXTRA_SUBJECT, context.getResources().getString(R.string.contact_subject));
    }
    
    public static Intent donateIntent(Context context) {
    	return new Intent(Intent.ACTION_VIEW,
    			Uri.parse("market://details?id=" + context.getResources().getString(R.string.package_name_donate)));
    }
    
    public static Campfire getCampfire(Context context) {
    	SharedPreferences prefs = context.getSharedPreferences("campfire", 0);
    	String user_id = prefs.getString("user_id", null);
        
        if (user_id != null) {
        	String subdomain = prefs.getString("subdomain", null);
            String token = prefs.getString("token", null);
        	return new Campfire(subdomain, token, user_id);
        } else
        	return null;
	}
    
    public static String getCampfireValue(Context context, String key) {
    	return context.getSharedPreferences("campfire", 0).getString(key, null);
    }
	
	public static void saveCampfire(Context context, Campfire campfire) {
		SharedPreferences prefs = context.getSharedPreferences("campfire", 0);
		Editor editor = prefs.edit();
	
		editor.putString("subdomain", campfire.subdomain);
		editor.putString("token", campfire.token);
		editor.putString("user_id", campfire.user_id);
		
		editor.commit();
	}
	
	public static void logoutCampfire(Context context) {
		SharedPreferences prefs = context.getSharedPreferences("campfire", 0);
		Editor editor = prefs.edit();
	
		editor.putString("user_id", null);		
		editor.commit();
	}
	
	public static String getStringPreference(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
	}
	
	public static String getStringPreference(Context context, String key, String value) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, value);
	}

	public static boolean setStringPreference(Context context, String key, String value) {
		return PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).commit();
	}
	
	public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
	}
	
	public static boolean setBooleanPreference(Context context, String key, boolean value) {
		return PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(key, value).commit();
	}
	
	// this should probably be moved into the Settings class itself
	public static int getIntPreferenceFromString(Context context, String key, int defaultValue) {
		int value;
		
		String stringValue = getStringPreference(context, key, null);
		if (stringValue == null)
			value = defaultValue;
		else {
			try {
				value = Integer.parseInt(stringValue); 
			} catch (NumberFormatException e) {
				value = defaultValue;
			}
		}
		
		return value;
	}
	
	public static String truncate(String original, int length) {
		if (original.length() > length)
			return original.substring(0, length-1) + "...";
		else
			return original;
	}
}