package com.github.klondike.android.campfire;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
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
	private boolean shortcut = false;
	private boolean error = false;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.room_list);
        
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.room_title);
        ((TextView) findViewById(R.id.room_title)).setText(R.string.room_list_title);
        
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null)
        	forResult = extras.getBoolean("for_result", false);
        
        String action = intent.getAction();
        shortcut = (action != null && action.equals(Intent.ACTION_CREATE_SHORTCUT));
        
        RoomListHolder holder = (RoomListHolder) getLastNonConfigurationInstance();
        if (holder != null) {
	    	rooms = holder.rooms;
	    	loadRoomsTask = holder.loadRoomsTask;
	    	error = holder.error;
	    	if (loadRoomsTask != null) {
	    		loadRoomsTask.context = this;
	    		loadingDialog();
	    	}
        }
        
        verifyLogin();
    }
    
    public void verifyLogin() {
    	campfire = Utils.getCampfire(this);
        if (campfire != null)
        	onLogin();
        else
	        startActivityForResult(new Intent(this, Login.class), Login.RESULT_LOGIN);
    }
    
    public void onLogin() {
    	if (loadRoomsTask == null) {
	    	if (rooms == null)
	    		new LoadRoomsTask(this).execute();
	    	else
	    		displayRooms();
    	}
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
    	RoomListHolder holder = new RoomListHolder();
    	holder.rooms = this.rooms;
    	holder.loadRoomsTask = this.loadRoomsTask;
    	holder.error = this.error;
    	return holder;
    }
    
    public void onLoadRooms(ArrayList<Room> rooms, CampfireException exception) {
    	if (exception == null && rooms != null)
    		this.rooms = rooms;
    	else {
    		this.rooms = new ArrayList<Room>();
    		this.error = true;
			Utils.alert(this, exception);
		}
    	displayRooms();
    }
    
    public void selectRoom(Room room) {
    	if (forResult) {
        	setResult(RESULT_OK, new Intent().putExtra("room_id", room.id));
        	finish();
    	} else if (shortcut) {
        	Intent roomIntent = roomIntent(room).putExtra("shortcut", true);
    		
    		Intent intent = new Intent();
    		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, roomIntent);
    		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, room.name);

    		// returning a Parcelable shortcut icon resource causes a force close on acore?!
    		// Parcelable resource = Intent.ShortcutIconResource.fromContext(this, R.drawable.icon);
    		// intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, resource);
    		
    		// returning just the resource int doesn't work, but it's here as a placeholder
    		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, R.drawable.icon);
    		
    		setResult(RESULT_OK, intent);
    		
    		finish();
    	} else
    		startActivity(new Intent(this, RoomView.class).putExtra("room_id", room.id));
    }
    
    public void displayRooms() {
    	if (rooms.size() <= 0) {
    		TextView empty = (TextView) findViewById(R.id.rooms_empty);
    		empty.setText(error ? R.string.rooms_error : R.string.no_rooms);
    		empty.setVisibility(View.VISIBLE);
    	} else
    		setListAdapter(new ArrayAdapter<Room>(RoomList.this, android.R.layout.simple_list_item_1, rooms));
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	selectRoom((Room) parent.getItemAtPosition(position));
    }
    
    public static Intent roomIntent(Room room) {
    	Intent intent = new Intent(Intent.ACTION_MAIN).putExtra("room_id", room.id);
    	intent.setClassName("com.github.klondike.android.campfire", "com.github.klondike.android.campfire.RoomView");
    	return intent;
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
        dialog.setMessage("Loading rooms...");
        
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (loadRoomsTask != null)
					loadRoomsTask.cancel(true);
				finish();
			}
		});
        
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
    	boolean error;
    }
    
}