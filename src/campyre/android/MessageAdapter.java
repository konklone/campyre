package campyre.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import campyre.java.Message;

public class MessageAdapter extends ArrayAdapter<Message> {
	private static final int PASTE_TRUNCATE = 200;
	private static String timestampFormat = "hh:mm a";
	
	private LayoutInflater inflater;
	
    public MessageAdapter(Activity context, ArrayList<Message> messages) {
        super(context, 0, messages);
        inflater = LayoutInflater.from(context);
    }

	public View getView(int position, View convertView, ViewGroup parent) {
		Message message = getItem(position);
		
		ViewHolder holder;
		if (convertView != null)
			holder = (ViewHolder) convertView.getTag();
		else
			holder = null;
		
		if (convertView == null || holder.type != message.type) {
			convertView = inflater.inflate(viewForMessage(message), null);
			
			holder = new ViewHolder();
			holder.body = (TextView) convertView.findViewById(R.id.text);
			holder.type = message.type;
			if (message.person != null)
				holder.person = (TextView) convertView.findViewById(R.id.person);
			
			convertView.setTag(holder);
		}
		
		holder.body.setText(bodyForMessage(message));
		if (message.person != null)
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
			return new SimpleDateFormat(timestampFormat).format(message.timestamp);
		case Message.PASTE:
			int length = message.body.length();
			if (length > PASTE_TRUNCATE)
				return message.body.substring(0, PASTE_TRUNCATE-1) + "\n\n[paste truncated]";
			else
				return message.body;
		default: // all others
			return message.body;
		}
	}
	
	static class ViewHolder {
        TextView body, person;
        int type;
    }

}