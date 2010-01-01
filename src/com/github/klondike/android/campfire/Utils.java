package com.github.klondike.android.campfire;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class Utils {

	public static void alert(Context context, String message) {
		Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}
    
    public static void alert(Context context, CampfireException exception) {
    	String message = exception == null ? "Unhandled error." : exception.getMessage();
    	Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    
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
}