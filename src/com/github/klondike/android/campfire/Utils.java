package com.github.klondike.android.campfire;

import android.content.Context;
import android.widget.Toast;

import com.github.klondike.java.campfire.CampfireException;

public class Utils {

	public static void alert(Context context, String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
	}
    
    public static void alert(Context context, CampfireException e) {
    	alert(context, (e != null ? e.getMessage() : "Error loading rooms."));
    }
}