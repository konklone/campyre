package com.github.klondike.android.campfire;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

public class RoomView extends Activity {
	private static final int SPEAKING = 1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setupControls();
	}
	

	private void setupControls() {
		
	}
	
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case SPEAKING:
		    ProgressDialog speakDialog = new ProgressDialog(this);
		    speakDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		    speakDialog.setMessage("Speaking...");
		    return speakDialog;
		default:
			return null;
        }
	}
}