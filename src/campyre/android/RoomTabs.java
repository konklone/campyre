package campyre.android;

import campyre.android.donate.R;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class RoomTabs extends TabActivity {
	public String roomId, roomName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.room);
		
		Bundle extras = getIntent().getExtras();
		roomId = extras.getString("room_id");
		roomName = extras.getString("room_name");
		
		Utils.setWindowTitle(this, roomName);
		
		setupTabs();
	}
	
	public void setupTabs() {
		TabHost tabHost = getTabHost();
		
		tabHost.addTab(tabHost.newTabSpec("room_tab").setIndicator("Room").setContent(roomIntent()));
		tabHost.addTab(tabHost.newTabSpec("transcript_tab").setIndicator("Today's Transcript").setContent(transcriptIntent()));
		
		tabHost.setCurrentTab(0);
	}
	
	public Intent roomIntent() {
		return new Intent(this, RoomView.class).putExtra("room_id", roomId);
	}
	
	public Intent transcriptIntent() {
		return new Intent(this, TranscriptView.class).putExtra("room_id", roomId);
	}
	
}
