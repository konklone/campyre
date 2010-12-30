package campyre.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import campyre.java.Message;

public class MessageAdapter extends ArrayAdapter<Message> {
	private static final int PASTE_TRUNCATE = 200;
	private static String TIMESTAMP_FORMAT = "hh:mm a";
	
	private LayoutInflater inflater;
	private Resources resources;
	
	// should be refactored to be an entire Campfire object, instead of hacking a userId around the system
	private String ownId;
	
    public MessageAdapter(Activity context, ArrayList<Message> messages, String ownId) {
        super(context, 0, messages);
        this.ownId = ownId;
        inflater = LayoutInflater.from(context);
        resources = context.getResources();
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
		case Message.PASTE:
			return R.layout.message_text;
		case Message.TIMESTAMP:
			return R.layout.message_timestamp;
		case Message.ENTRY:
		case Message.LEAVE:
			return R.layout.message_entry;
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
			holder.person = (TextView) view.findViewById(R.id.person);
		}
		
		return holder;
	}
	
	public void bindMessage(Message message, View view, ViewHolder holder) {
		// load the person's name if necessary
		switch(message.type) {
		case Message.TEXT:
		case Message.PASTE:
		case Message.ENTRY:
		case Message.LEAVE:
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
			if (message.body.length() > PASTE_TRUNCATE)
				holder.body.setText(message.body.substring(0, PASTE_TRUNCATE-1) + "\n\n[paste truncated]");
			else
				holder.body.setText(message.body);
			break;
		case Message.TEXT:
		case Message.TRANSIT:
		default:
			holder.body.setText(message.body);
		}
		
		// change background color of text view if the owner is the logged in user (like the web client)
		// no need to do this for messages of type TRANSIT because they are always that way
		switch (message.type) {
		case Message.TEXT:
		case Message.PASTE:
			if (message.user_id.equals(ownId))
				view.setBackgroundColor(resources.getColor(R.color.message_text_background_own));
			else
				view.setBackgroundColor(resources.getColor(R.color.message_text_background));
		}
	}
	
	// needs to have a field for every type of view that could be found on a message object
	static class ViewHolder {
        TextView body, person;
    }

}