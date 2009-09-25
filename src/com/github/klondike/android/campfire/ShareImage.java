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

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;

public class ShareImage extends Activity {
	private FileInputStream image;
	private FileDescriptor fd;
	
	private TextView explain;
	private Campfire campfire;
	private String roomId;
	private boolean loggedIn;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.share);
		
		setupControls();
		loadCampfire();
		loginCampfire();
		
		//loadImage();
		uploadImage();
	}
	
	public void setupControls() {
		explain = (TextView) this.findViewById(R.id.explain);
		Button upload = (Button) this.findViewById(R.id.upload);
		upload.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (image != null)
					uploadImage();
				else
					explain.setText("No file found.");
			}
		});
	}
	
	public void uploadImage() {
		Bundle extras = this.getIntent().getExtras();
		Uri uri = (Uri) extras.get("android.intent.extra.STREAM");
		
		ContentResolver cr = this.getContentResolver();
		try {
			ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
			fd = pfd.getFileDescriptor();
			image = new FileInputStream(fd);
		} catch (FileNotFoundException e) {
			image = null;
		}
	
		try {
			if (image != null && campfire.uploadFile(roomId, image))
				explain.setText("Uploaded file\n");
			else
				explain.setText("Didn't upload file :(\n");
		} catch (CampfireException e) {
			explain.setText("Error uploading file!\n");
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
	
	public void loginCampfire() {
		try {
			loggedIn = campfire.login();
		} catch (CampfireException e) {
			loggedIn = false;
		}
	}
	
}
