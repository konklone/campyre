package com.github.klondike.android.campfire;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.klondike.java.campfire.Campfire;
import com.github.klondike.java.campfire.CampfireException;
import com.github.klondike.java.campfire.Room;

public class RoomList extends ListActivity { 
	private static final int MENU_CLEAR = 0;
	
	private Campfire campfire = null;
	private ArrayList<Room> rooms = null;
	
	private LoadRoomsTask loadRoomsTask = null;
	private ProgressDialog dialog = null;
	
	private boolean forResult = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.room_list);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        	forResult = extras.getBoolean("for_result", false);
        
        RoomListHolder holder = (RoomListHolder) getLastNonConfigurationInstance();
        if (holder != null) {
	    	rooms = holder.rooms;
	    	loadRoomsTask = holder.loadRoomsTask;
	    	if (loadRoomsTask != null) {
	    		loadRoomsTask.context = this;
	    		loadingDialog();
	    	}
        }
        
        if (loadRoomsTask == null)
        	verifyLogin();
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	RoomListHolder holder = new RoomListHolder();
    	holder.rooms = this.rooms;
    	holder.loadRoomsTask = this.loadRoomsTask;
    	return holder;
    }
    
    public void onLogin() {
    	if (rooms == null)
	    	new LoadRoomsTask(this).execute();
    	else
    		displayRooms();
    }
    
    public void onLoadRooms(ArrayList<Room> rooms, CampfireException exception) {
    	if (exception == null && rooms != null) {
    		this.rooms = rooms;
        	displayRooms();
    	} else {
			Utils.alert(this, exception);
			finish();
		}
    }
    
    public void selectRoom(Room room) {
    	if (forResult) {
    		Intent intent = new Intent();
    		intent.putExtra("room_id", room.id);
        	
        	setResult(RESULT_OK, intent);
        	finish();
    	} else {
    		Intent intent = new Intent(Intent.ACTION_MAIN);
    		intent.setClassName("com.github.klondike.android.campfire", "com.github.klondike.android.campfire.RoomView");
        	intent.putExtra("room_id", room.id);
        	
        	startActivity(intent);
    	}
    }
    
    public void displayRooms() {
    	if (rooms.size() <= 0)
			((TextView) findViewById(R.id.rooms_empty)).setVisibility(View.VISIBLE);
		setListAdapter(new ArrayAdapter<Room>(RoomList.this, android.R.layout.simple_list_item_1, rooms));
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	selectRoom((Room) parent.getItemAtPosition(position));
    }
    
    public void verifyLogin() {
    	campfire = Utils.getCampfire(this);
        if (campfire != null)
        	onLogin();
        else
        	startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case Login.RESULT_LOGIN:
    		if (resultCode == RESULT_OK) {
    			Utils.alert(this, "You have been logged in successfully.");
    			campfire = Utils.getCampfire(this);
    			onLogin();
    		} else
    			finish();
    	}
    }
    
    @Override 
    public boolean onCreateOptionsMenu(Menu menu) { 
	    boolean result = super.onCreateOptionsMenu(menu);
	    
        menu.add(0, MENU_CLEAR, 0, R.string.logout).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_CLEAR:
    		Utils.clearCampfire(this);
    		finish();
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    public void loadingDialog() {
    	dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setMessage("Loading rooms...");
        dialog.show();
    }
    
    private class LoadRoomsTask extends AsyncTask<Void,Void,ArrayList<Room>> {
    	public RoomList context;
    	public CampfireException exception = null;
    	
    	public LoadRoomsTask(RoomList context) {
    		super();
    		this.context = context;
    		this.context.loadRoomsTask = this;
    	}
    	 
       	@Override
    	protected void onPreExecute() {
            context.loadingDialog();
    	}
    	
    	@Override
    	protected ArrayList<Room> doInBackground(Void... nothing) {
    		try {
				return Room.all(context.campfire);
			} catch (CampfireException e) {
				this.exception = e;
				return null;
			}
    	}
    	
    	@Override
    	protected void onPostExecute(ArrayList<Room> rooms) {
    		if (context.dialog != null && context.dialog.isShowing())
    			context.dialog.dismiss();
    		context.loadRoomsTask = null;
    		
    		context.onLoadRooms(rooms, exception);
    	}
    }
    
    static class RoomListHolder {
    	ArrayList<Room> rooms;
    	LoadRoomsTask loadRoomsTask;
    }
    
}