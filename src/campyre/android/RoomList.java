package campyre.android;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import campyre.java.Campfire;
import campyre.java.CampfireException;
import campyre.java.Room;

public class RoomList extends ListActivity { 
	private static final int MENU_CLEAR = 0;
	private static final int MENU_ABOUT = 1;
	private static final int MENU_FEEDBACK = 2;
	private static final int MENU_DONATE = 3;
	
	private Campfire campfire = null;
	private ArrayList<Room> rooms = null;
	
	private LoadRoomsTask loadRoomsTask = null;
	private TextView empty;
	private Button tryAgain;
	
	private boolean forResult = false;
	private boolean shortcut = false;
	
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
        
        setupControls();
        
        RoomListHolder holder = (RoomListHolder) getLastNonConfigurationInstance();
        if (holder != null) {
	    	rooms = holder.rooms;
	    	loadRoomsTask = holder.loadRoomsTask;
	    	if (loadRoomsTask != null)
	    		loadRoomsTask.onScreenLoad(this);
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
    	loadRooms();
    }
    
    public void loadRooms() {
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
    	return holder;
    }
    
    public void onLoadRooms(ArrayList<Room> rooms, CampfireException exception) {
    	if (exception == null && rooms != null) {
    		this.rooms = rooms;
    		displayRooms();
    	} else {
    		this.rooms = new ArrayList<Room>();
    		displayRooms(exception);
		}
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

    		Parcelable resource = Intent.ShortcutIconResource.fromContext(this, Utils.SHORTCUT_ICON);
    		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, resource);
    		
    		setResult(RESULT_OK, intent);
    		
    		finish();
    	} else
    		startActivity(new Intent(this, RoomTabs.class).putExtra("room_id", room.id));
    }
    
    public void displayRooms() {
    	if (rooms.size() <= 0)
    		showEmpty(R.string.no_rooms);
    	else
    		setListAdapter(new RoomAdapter(this, rooms));
    }
    
    public void displayRooms(CampfireException exception) {
    	showEmpty(R.string.rooms_error);
		Utils.alert(this, exception);
    }
    
    public void setupControls() {
    	empty = (TextView) findViewById(R.id.rooms_empty);
    	tryAgain = (Button) findViewById(R.id.try_again);
    	tryAgain.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				rooms = null;
				hideEmpty();
				loadRooms();
			}
		});
    }
    
    public void showEmpty(int message) {
    	empty.setText(message);
    	empty.setVisibility(View.VISIBLE);
    	tryAgain.setVisibility(View.VISIBLE);
    }
    
    public void hideEmpty() {
    	empty.setVisibility(View.GONE);
    	tryAgain.setVisibility(View.GONE);
    }
    
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	selectRoom((Room) parent.getItemAtPosition(position));
    }
    
    public static Intent roomIntent(Room room) {
    	Intent intent = new Intent(Intent.ACTION_MAIN).putExtra("room_id", room.id);
    	intent.setClassName("campyre.android", "campyre.android.RoomTabs");
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
        if (Utils.ASK_DONATE)
        	menu.add(1, MENU_DONATE, 1, R.string.menu_donate).setIcon(android.R.drawable.ic_menu_send);
        menu.add(2, MENU_FEEDBACK, 2, "Feedback").setIcon(android.R.drawable.ic_menu_report_image);
        menu.add(3, MENU_ABOUT, 3, "About").setIcon(android.R.drawable.ic_menu_help);
        
        return result;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) { 
    	case MENU_CLEAR:
    		Utils.logoutCampfire(this);
    		finish();
    		break;
    	case MENU_FEEDBACK:
    		startActivity(Utils.feedbackIntent(this));
    		break;
    	case MENU_ABOUT:
    		showDialog(Utils.ABOUT);
    		break;
    	case MENU_DONATE:
    		startActivity(Utils.donateIntent(this));
    		break;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
	protected Dialog onCreateDialog(int id) { 
		return id == Utils.ABOUT ? Utils.aboutDialog(this) : null;
	}
    
    private class RoomAdapter extends ArrayAdapter<Room> {
    	LayoutInflater inflater;

        public RoomAdapter(Activity context, ArrayList<Room> items) {
            super(context, 0, items);
            inflater = LayoutInflater.from(context);
        }

		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout view;
			
			if (convertView == null)
				view = (LinearLayout) inflater.inflate(R.layout.room_item, null);
			else
				view = (LinearLayout) convertView;
				
			Room room = getItem(position);
			((TextView) view.findViewById(R.id.name)).setText(room.name);
			((TextView) view.findViewById(R.id.topic)).setText(room.topic);
			
			return view;
		}
    }
    
    private class LoadRoomsTask extends AsyncTask<Void,Void,ArrayList<Room>> {
    	public RoomList context;
    	public CampfireException exception = null;
    	private ProgressDialog dialog = null;
    	
    	public LoadRoomsTask(RoomList context) {
    		super();
    		this.context = context;
    		this.context.loadRoomsTask = this;
    	}
    	 
       	@Override
    	protected void onPreExecute() {
            loadingDialog();
    	}
       	
       	protected void onScreenLoad(RoomList context) {
       		this.context = context;
    		loadingDialog();
       	}
       	
       	protected void loadingDialog() {
       		dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage("Loading rooms...");
            
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
    			@Override
    			public void onCancel(DialogInterface dialog) {
    				cancel(true);
    				context.finish();
    			}
    		});
            
            dialog.show();
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
    		if (dialog != null && dialog.isShowing())
    			dialog.dismiss();
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