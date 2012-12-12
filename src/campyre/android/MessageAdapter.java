package campyre.android;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import campyre.java.Campfire;
import campyre.java.Message;
import campyre.java.Message.Type;
import campyre.java.Room;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MessageAdapter extends ArrayAdapter<Message> {
	private static final int PASTE_TRUNCATE = 200;
	private static String TIMESTAMP_FORMAT = "h:mm a";

	private RoomContext context;
	private Context originalContext;
	private LayoutInflater inflater;
	private Resources resources;

	private Campfire campfire;
	private Room room;

    public MessageAdapter(RoomContext context, ArrayList<Message> messages) {
    	super(context.getContext(), 0, messages);

    	this.context = context;
    	this.campfire = context.getCampfire();
    	this.room = context.getRoom();
    	this.originalContext = context.getContext();

        inflater = LayoutInflater.from(originalContext);
        resources = originalContext.getResources();
    }


    @Override
    public int getItemViewType(int position) {
    	int type = getItem(position).type.ordinal();
    	if (type > 0)
    		return type;
    	else
    		return Adapter.IGNORE_ITEM_VIEW_TYPE;
    }

    @Override
    public int getViewTypeCount() {
    	return Message.Type.values().length;
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

		holder.messageId = message.id;
		bindMessage(message, view, holder, position);

		return view;
	}

	public int viewForMessage(Type type) {
	  // TODO for all the Type things, it'd be great if we could simplify and just call type.method() or something.
		switch (type) {
		case ERROR:
			return R.layout.message_error;
		case TRANSIT:
			return R.layout.message_transit;
		case IMAGE:
			return R.layout.message_image;
		case PASTE:
			return R.layout.message_paste;
		case TIMESTAMP:
			return R.layout.message_timestamp;
		case ENTRY:
		case LEAVE:
			return R.layout.message_entry;
		case TOPIC:
			return R.layout.message_topic;
		case TEXT:
		default:
			return R.layout.message_text;
		}
	}

	public ViewHolder holderForMessage(Type type, View view) {
		ViewHolder holder = new ViewHolder();

		if (type != Type.IMAGE)
			holder.body = (TextView) view.findViewById(R.id.text);

		switch(type) {
		case TEXT:
		case IMAGE:
		case PASTE:
		case ENTRY:
		case LEAVE:
		case TOPIC:
			holder.person = (TextView) view.findViewById(R.id.person);
		}

		if (type == Type.PASTE)
			holder.paste = (Button) view.findViewById(R.id.paste);

		if (type == Type.IMAGE) {
			holder.image = (ImageView) view.findViewById(R.id.image);
			holder.imageLoading = (ViewGroup) view.findViewById(R.id.loading);
			holder.imageSpinner = holder.imageLoading.findViewById(R.id.loading_spinner);
			holder.imageMessage = (TextView) holder.imageLoading.findViewById(R.id.loading_message);
		}

		return holder;
	}

	public void bindMessage(Message message, View view, ViewHolder holder, int position) {
		// load the person's name if necessary
		switch(message.type) {
		case TEXT:
		case IMAGE:
		case PASTE:
		case ENTRY:
		case LEAVE:
		case TOPIC:
			holder.person.setText(message.person);
		}

		// hide the person if the previous message in the adapter is a text message of the same person
		switch(message.type) {
		case TEXT:
		case IMAGE:
		case PASTE:
			if ((position - 1) >= 0 && (position - 1) < this.getCount()) {
				Message previous = getItem(position - 1);
				if (previous != null) {
					switch(previous.type) {
					case TEXT:
					case IMAGE:
					case PASTE:
						if (previous.user_id.equals(message.user_id))
							holder.person.setVisibility(View.INVISIBLE);
					}
				}
			}
		}

		// format the body text
		switch (message.type) {
		case ENTRY:
			holder.body.setText(R.string.message_entered_room);
			break;
		case LEAVE:
			holder.body.setText(R.string.message_left_room);
			break;
		case TIMESTAMP:
			holder.body.setText(new SimpleDateFormat(TIMESTAMP_FORMAT).format(message.timestamp));
			break;
		case PASTE:
			String body = message.body.trim();
			holder.body.setText(Utils.truncate(body, PASTE_TRUNCATE));
			break;
		case TEXT:
		case TRANSIT:
		case TOPIC:
			holder.body.setText(message.body.trim());
		}

		// change background color of text view if the owner is the logged in user (like the web client)
		// no need to do this for messages of type TRANSIT because they are always that way
		switch (message.type) {
		case TEXT:
		case PASTE:
			if (message.user_id.equals(campfire.user_id))
				view.setBackgroundColor(resources.getColor(R.color.message_text_background_own));
			else
				view.setBackgroundColor(resources.getColor(R.color.message_text_background));
		}

		if (message.type == Type.PASTE) {
			final String person = message.person;
			final String paste = message.body;
			final Date timestamp = message.timestamp;
			holder.paste.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					originalContext.startActivity(new Intent(originalContext, PasteDetail.class)
						.putExtra("person", person)
						.putExtra("paste", paste)
						.putExtra("timestamp", timestamp)
						.putExtra("room_name", room.name));
				}
			});
		}

		// spawn a possible image loading task, to load images inline like the web client
		if (message.type == Type.IMAGE) {
			final String url = message.body;
			final String person = message.person;
			final Date timestamp = message.timestamp;

			BitmapDrawable image = context.cachedImage(message.id);
			if (image != null)
				holder.showImage(image);
			else {
				holder.showLoading();
				context.loadImage(message.body, message.id);
			}

			// take the user to a dedicated activity when it's clicked on
			View.OnClickListener listener = new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					originalContext.startActivity(new Intent(originalContext, ImageDetail.class)
						.putExtra("person", person)
						.putExtra("url", url)
						.putExtra("timestamp", timestamp)
						.putExtra("room_name", room.name));
				}
			};
			holder.image.setOnClickListener(listener);
			holder.imageLoading.setOnClickListener(listener);
		}
	}

  // needs to have a field for every type of view that could be found on a message object
  static class ViewHolder {
    TextView body, person;
    Button paste;
    String messageId; // used as hook for attaching a downloaded image to the right message

    // image related views
    ImageView image;
    ViewGroup imageLoading;
    View imageSpinner;
    TextView imageMessage;

    public void showImage(BitmapDrawable drawable) {
      image.setImageDrawable(drawable);
      imageLoading.setVisibility(View.GONE);
      image.setVisibility(View.VISIBLE);
    }

    public void showLoading() {
      image.setImageDrawable(null);
      image.setVisibility(View.GONE);
      imageSpinner.setVisibility(View.VISIBLE);
      imageMessage.setText(R.string.image_loading);
      imageLoading.setVisibility(View.VISIBLE);
    }

    public void imageFailed() {
      showLoading();
      imageSpinner.setVisibility(View.GONE);
      imageMessage.setText(R.string.image_failed);
    }

    @Override public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ViewHolder other = (ViewHolder) obj;
      if (messageId == null) {
        if (other.messageId != null)
          return false;
      } else if (!messageId.equals(other.messageId))
        return false;
      return true;
    }

    @Override public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
      return result;
    }
  }

	public interface RoomContext {
    Campfire getCampfire();
    Room getRoom();
    Context getContext();
    void loadImage(String url, String id);
    BitmapDrawable cachedImage(String id);
	}
}