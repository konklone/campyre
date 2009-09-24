package com.github.klondike.android.campfire;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;

public class ShareImage extends Activity {
	private File image;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.share);
		
		loadImage();
		setupControls();
	}
	
	public void loadImage() {
		
	}
	
	public void setupControls() {
		
	}
	
	public void uploadImage() {
		
	}
	
}
