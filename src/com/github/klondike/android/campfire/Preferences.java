package com.github.klondike.android.campfire;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

	
	@Override 
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState); 
		addPreferencesFromResource(R.xml.preferences); 
	}
	
	public static String getSubdomain(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("campfire_subdomain", null);
	}
	
	public static String getEmail(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("campfire_email", null);
	}
	
	public static String getPassword(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("campfire_password", null);
	}
	
	public static String getRoomId(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("campfire_room_id", null);
	}
	
	public static boolean getSsl(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("campfire_ssl", false);
	}
}