package com.github.klondike.android.campfire;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.Window;
import android.widget.Toast;

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
	
	// guaranteed to be logged in and the "campfire" variable set
	public void onLogin() {
		loadRoom();
	}
	
	// guaranteed to have a room selected and the "room" variable set
	public void onLoadRoom() {
		uploadImage();
	}
	
	public void onUploadImage() {
		if (uploaded)
			alert("Uploaded image to Campfire.");
		else
			alert(uploadError);
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
					// Don't move this code into another method, or split it up - somehow
					// stuff gets out of scope or garbage collected and file transfers start dying
					Bundle extras = ShareImage.this.getIntent().getExtras();
					Uri uri = (Uri) extras.get("android.intent.extra.STREAM");
					
					ContentResolver cr = ShareImage.this.getContentResolver();
				
					ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
					FileDescriptor fd = pfd.getFileDescriptor();
					FileInputStream image = new FileInputStream(fd);
					
					if (image == null) {
						uploaded = false;
						uploadError = "Error processing photo, image was not uploaded.";
					} else {
						if (room.uploadFile(image))
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
    			alert("You have been logged in successfully.");
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
	
	public void alert(String msg) {
		Toast.makeText(ShareImage.this, msg, Toast.LENGTH_SHORT).show();
	}
	
}
