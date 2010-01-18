package campyre.android;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TextView;

public class RoomTabs extends TabActivity {
	public String roomId, roomName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.room);
		
		Bundle extras = getIntent().getExtras();
		roomId = extras.getString("room_id");
		roomName = "placeholder";
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.room_title);
        ((TextView) findViewById(R.id.room_title)).setText(roomName);
		
		setupTabs();
	}
	
	public void setupTabs() {
		TabHost tabHost = getTabHost();
		
		tabHost.addTab(tabHost.newTabSpec("room_tab").setIndicator("Room").setContent(roomIntent()));
		tabHost.addTab(tabHost.newTabSpec("transcript_tab").setIndicator("Transcript").setContent(transcriptIntent()));
		
		tabHost.setCurrentTab(0);
	}
	
	public Intent roomIntent() {
		return new Intent(this, RoomView.class).putExtra("room_id", roomId);
	}
	
	public Intent transcriptIntent() {
		return new Intent(this, TranscriptView.class).putExtra("room_id", roomId);
	}
	
}
