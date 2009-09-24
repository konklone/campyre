package com.github.klondike.android.campfire;

import java.io.File;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class ShareImage extends Activity {
	private File image;
	private TextView explain;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.share);
		
		setupControls();
		loadImage();
	}
	
	public void loadImage() {
		Bundle extras = this.getIntent().getExtras();
		Uri contentURI = (Uri) extras.get("android.intent.extra.STREAM");
		explain.setText(contentURI.toString());
	}
	
	public void setupControls() {
		explain = (TextView) this.findViewById(R.id.explain);
		Button upload = (Button) this.findViewById(R.id.upload);
	}
	
	public void uploadImage() {
		
	}
	
}
