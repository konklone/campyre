package com.github.klondike.android.campfire;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class ShareImage extends Activity {
	private Campfire campfire;
	private String roomId;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.share);
		
		loadCampfire();
		if (loginCampfire())
			uploadImage();
		else
			alert("Couldn't log in to Campfire, image was not uploaded. Check your Campfire credentials.");
		finish();
	}
	
	public void uploadImage() {
		Bundle extras = this.getIntent().getExtras();
		Uri uri = (Uri) extras.get("android.intent.extra.STREAM");
		
		ContentResolver cr = this.getContentResolver();
		try {
			ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			FileDescriptor fd = pfd.getFileDescriptor();
			FileInputStream image = new FileInputStream(fd);
			
			if (image == null) {
				alert("Error getting photo from gallery, image was not uploaded.");
				finish();
			}
			
			if (campfire.uploadFile(roomId, image))
				alert("Uploaded image to Campfire.");
			
		} catch (FileNotFoundException e) {
			alert("Error processing photo, image was not uploaded.");
		} catch (CampfireException e) {
			alert("Error connecting to Campfire, image was not uploaded.");
		}
	}
	
	public void loadCampfire() {
    	Resources res = getResources();
    	String username = res.getString(R.string.campfire_username);
        String email = res.getString(R.string.campfire_email);
        String password = res.getString(R.string.campfire_password);
        String sslString = res.getString(R.string.campfire_ssl);
        roomId = res.getString(R.string.campfire_room_id);
        
        boolean ssl = false;
        if (sslString == "true")
        	ssl = true;
        
        campfire = new Campfire(username, email, password, ssl);
    }
	
	public boolean loginCampfire() {
		try {
			return campfire.login();
		} catch (CampfireException e) {
			return false;
		}
	}
	
	public void alert(String msg) {
		Toast.makeText(ShareImage.this, msg, Toast.LENGTH_SHORT).show();
	}
	
}
