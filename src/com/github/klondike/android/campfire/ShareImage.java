package com.github.klondike.android.campfire;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;
import com.github.klondike.java.campfire.Room;

public class ShareImage extends Activity {
	private static final int UPLOADING = 0;
	private static final int RESULT_ROOM_ID = 0;
	
	private Campfire campfire;
	private Room room;
	
	private boolean uploaded;
	private String uploadError;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		verifyLogin();
	}
	
	public void onLogin() {
		loadRoom();
	}
	
	public void onLoadRoom() {
		uploadImage();
	}
	
	public void onUploadImage() {
		if (uploaded)
			Utils.alert(this, "Uploaded image to Campfire.");
		else
			Utils.alert(this, uploadError);
		finish();
	}
	
	final Handler handler = new Handler();
	final Runnable afterUpload = new Runnable() {
		public void run() {
			dismissDialog(UPLOADING);
			onUploadImage();
		}
	};
	
	public void uploadImage() {
		Thread uploadThread = new Thread() {
			public void run() {
				try {
					Uri uri = (Uri) getIntent().getExtras().get("android.intent.extra.STREAM");
					
					InputStream stream = getContentResolver().openInputStream(uri);
					String mimeType = getContentResolver().getType(uri);
					String filename = filenameFor(mimeType);
					
					if (stream == null) {
						uploaded = false;
						uploadError = "Error processing photo, image was not uploaded.";
					} else {
						if (room.uploadImage(stream, filename, mimeType))
							uploaded = true;
						else {
							uploaded = false;
							uploadError = "Couldn't upload file to Campfire.";
						}
					}
				} catch (FileNotFoundException e) {
					uploaded = false;
					uploadError = "Error processing photo, image was not uploaded.";
				} catch (CampfireException e) {
					uploaded = false;
					uploadError = "Error connecting to Campfire, image was not uploaded.";
				}
				handler.post(afterUpload);
			}
		};
		uploadThread.start();
		
		showDialog(UPLOADING);
	}
	
	public void verifyLogin() {
    	campfire = Login.getCampfire(this);
        if (campfire != null)
        	onLogin();
        else
        	startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
    }
	
	public void loadRoom() {
		Intent intent = new Intent(this, RoomList.class);
		intent.putExtra("for_result", true);
		startActivityForResult(intent, RESULT_ROOM_ID);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case Login.RESULT_LOGIN:
    		if (resultCode == RESULT_OK) {
    			Utils.alert(this, "You have been logged in successfully.");
    			campfire = Login.getCampfire(this);
    			onLogin();
    		} else
    			finish();
    		break;
    	case RESULT_ROOM_ID:
			if (resultCode == RESULT_OK) {
				String roomId = data.getExtras().getString("room_id");
				room = new Room(campfire, roomId);
				onLoadRoom();
			} else
				finish();
			break;
    	}
    }
	
	protected Dialog onCreateDialog(int id) {
        switch(id) {
        case UPLOADING:
            ProgressDialog dialog = new ProgressDialog(this);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Uploading image...");
            return dialog;
        default:
            return null;
        }
    }
	
	public static String filenameFor(String mimeType) {
		// default to whatever was in the 2nd half of the mime type
		if (mimeType.equals("image/jpeg"))
			return "from_phone.jpg";
		else
			return "from_phone." + mimeType.split("/")[1];
	}
	
}
