package campyre.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import campyre.java.Campfire;
import campyre.java.Message;
import campyre.java.Room;

public class MessageAdapter extends ArrayAdapter<Message> {
	private static final int PASTE_TRUNCATE = 200;
	private static String TIMESTAMP_FORMAT = "h:mm a";
	
	private Context context;
	private LayoutInflater inflater;
	private Resources resources;
	
	private Campfire campfire;
	private Room room;
	
    public MessageAdapter(RoomContext context, ArrayList<Message> messages) {
    	super(context.getContext(), 0, messages);
    	
    	this.context = context.getContext();
    	this.campfire = context.getCampfire();
    	this.room = context.getRoom();
        
        inflater = LayoutInflater.from(this.context);
        resources = this.context.getResources();
    }
    
    
    @Override
    public int getItemViewType(int position) {
    	int type = getItem(position).type; 
    	if (type > 0)
    		return type;
    	else
    		return Adapter.IGNORE_ITEM_VIEW_TYPE;
    }
    
    @Override
    public int getViewTypeCount() {
    	return Message.SUPPORTED_MESSAGE_TYPES;
    }
    
    @Override
	public View getView(int position, View view, ViewGroup parent) {
		Message message = getItem(position);
		
		ViewHolder holder = null;
		if (view != null)
			holder = (ViewHolder) view.getTag();
		else {
			view = inflater.inflate(viewForMessage(message.type), null);
			holder = holderForMessage(message.type, view);
			
			view.setTag(holder);
		}
		
		bindMessage(message, view, holder);
		
		return view;
	}
	
	public int viewForMessage(int type) {
		switch (type) {
		case Message.ERROR:
			return R.layout.message_error;
		case Message.TRANSIT:
			return R.layout.message_transit;
		case Message.TEXT:
			return R.layout.message_text;
		case Message.PASTE:
			return R.layout.message_paste;
		case Message.TIMESTAMP:
			return R.layout.message_timestamp;
		case Message.ENTRY:
		case Message.LEAVE:
			return R.layout.message_entry;
		case Message.TOPIC:
			return R.layout.message_topic;
		default:
			return R.layout.message_text;
		}
	}
	
	public ViewHolder holderForMessage(int type, View view) {
		ViewHolder holder = new ViewHolder();
		
		holder.body = (TextView) view.findViewById(R.id.text);
		
		switch(type) {
		case Message.TEXT:
		case Message.PASTE:
		case Message.ENTRY:
		case Message.LEAVE:
		case Message.TOPIC:
			holder.person = (TextView) view.findViewById(R.id.person);
		}
		
		if (type == Message.PASTE)
			holder.paste = (Button) view.findViewById(R.id.paste);
		
		return holder;
	}
	
	public void bindMessage(Message message, View view, ViewHolder holder) {
		// load the person's name if necessary
		switch(message.type) {
		case Message.TEXT:
		case Message.PASTE:
		case Message.ENTRY:
		case Message.LEAVE:
		case Message.TOPIC:
			holder.person.setText(message.person);
		}
		
		// format the body text
		switch (message.type) { 
		case Message.ENTRY:
			holder.body.setText(R.string.message_entered_room);
			break;
		case Message.LEAVE:
			holder.body.setText(R.string.message_left_room);
			break;
		case Message.TIMESTAMP:
			holder.body.setText(new SimpleDateFormat(TIMESTAMP_FORMAT).format(message.timestamp));
			break;
		case Message.PASTE:
			String body = message.body.trim();
			if (body.length() > PASTE_TRUNCATE)
				holder.body.setText(body.substring(0, PASTE_TRUNCATE-1) + "...");
			else
				holder.body.setText(body);
			break;
		case Message.TEXT:
		case Message.TRANSIT:
		case Message.TOPIC:
		default:
			holder.body.setText(message.body.trim());
		}
		
		// change background color of text view if the owner is the logged in user (like the web client)
		// no need to do this for messages of type TRANSIT because they are always that way
		switch (message.type) {
		case Message.TEXT:
		case Message.PASTE:
			if (message.user_id.equals(campfire.user_id))
				view.setBackgroundColor(resources.getColor(R.color.message_text_background_own));
			else
				view.setBackgroundColor(resources.getColor(R.color.message_text_background));
		}
		
		if (message.type == Message.PASTE) {
			final String person = message.person;
			final String paste = message.body;
			final Date timestamp = message.timestamp;
			holder.paste.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					context.startActivity(new Intent(context, PasteView.class)
						.putExtra("person", person)
						.putExtra("paste", paste)
						.putExtra("timestamp", timestamp)
						.putExtra("room_name", room.name));
				}
			});
		}
	}
	
	// needs to have a field for every type of view that could be found on a message object
	static class ViewHolder {
        TextView body, person;
        Button paste;
    }

	public interface RoomContext {
		public abstract Campfire getCampfire();
		public abstract Room getRoom();
		public abstract Context getContext();
	}
}