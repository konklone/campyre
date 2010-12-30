package campyre.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
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
	
	//private String ownId;
	
    public MessageAdapter(Activity context, ArrayList<Message> messages) {
        super(context, 0, messages);
        //this.ownId = ownId;
        inflater = LayoutInflater.from(context);
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
	public View getView(int position, View convertView, ViewGroup parent) {
		Message message = getItem(position);
		
		ViewHolder holder = null;
		if (convertView != null)
			holder = (ViewHolder) convertView.getTag();
		
		if (convertView == null) {
			convertView = inflater.inflate(viewForMessage(message), null);
			
			holder = new ViewHolder();
			holder.body = (TextView) convertView.findViewById(R.id.text);
			holder.person = (TextView) convertView.findViewById(R.id.person); // could be null
			
			convertView.setTag(holder);
		}
		
		holder.body.setText(bodyForMessage(message));
		if (holder.person != null)
			holder.person.setText(message.person);
		
		return convertView;
	}
	
	public int viewForMessage(Message message) {
		switch (message.type) {
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
	
	public String bodyForMessage(Message message) {
		switch (message.type) { 
		case Message.ENTRY:
			return "has entered the room";
		case Message.LEAVE:
			return "has left the room";
		case Message.TIMESTAMP:
			return new SimpleDateFormat(TIMESTAMP_FORMAT).format(message.timestamp);
		case Message.PASTE:
			if (message.body.length() > PASTE_TRUNCATE)
				return message.body.substring(0, PASTE_TRUNCATE-1) + "\n\n[paste truncated]";
			else
				return message.body;
		default: // all others
			return message.body;
		}
	}
	
	static class ViewHolder {
        TextView body, person;
    }

}