package com.github.klondike.android.campfire;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {

	public static String getRoomId(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString("roomId", null);
	}
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState); 
		addPreferencesFromResource(R.xml.preferences); 
	}
}