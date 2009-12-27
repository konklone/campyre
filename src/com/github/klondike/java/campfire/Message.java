package com.github.klondike.java.campfire;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is going to handle each type of possible room event, pivoting on a "type" field.
 * Not all fields will be filled in for each type of event, but that's okay for right now.
 * @author eric
 *
 */
public class Message {
	public static final int UNSUPPORTED = -1;
	public static final int TEXT = 0;
	public static final int TIMESTAMP = 1;
	public static final int ENTRY = 2;
	public static final int LEAVE = 3;
	
	public int type;
	public String id, user_id, body, timestamp;
	
	// This is really just here to serve the Android client. 
	// It really needs the display name to put on the Message object itself for help in adapting it to the list.
	// It violates the intended separation between the two packages, but oh well.
	public String person;
	
	public Message(JSONObject json) throws JSONException {
		this.type = typeFor(json.getString("type"));
		this.id = json.getString("id");
		this.user_id = json.getString("user_id");
		this.body = json.getString("body");
		this.timestamp = json.getString("created_at");
		
		// for testing
		if (requiresPerson(this.type))
			this.person = "Test " + this.user_id;
		else
			this.person = null;
	}
	
	public static ArrayList<Message> allToday(Room room) throws CampfireException {
		return allToday(room, -1);
	}
	
	public static ArrayList<Message> allToday(Room room, int max) throws CampfireException {
		ArrayList<Message> messages = new ArrayList<Message>();
		try {
			JSONArray items = new CampfireRequest(room.campfire).getList(todayPath(room.id), "messages");
			int length = items.length();
			
			// we want the bottom-most messages, up to a maximum of "max"
			// if max is 0 or -1, then just return everything
			int start;
			if (max > 0 && max < length)
				start = length - max;
			else
				start = 0;
			
			for (int i=start; i<length; i++) {
				Message message = new Message(items.getJSONObject(i));
				
				if (message.type != UNSUPPORTED)
					messages.add(message);
			}
			
		} catch (JSONException e) {
			throw new CampfireException(e, "Could not load messages from their JSON.");
		}
		return messages; 
	}
	
	private static int typeFor(String type) {
		if (type.equals("TextMessage"))
			return TEXT;
		else if (type.equals("TimestampMessage"))
			return TIMESTAMP;
		else if (type.equals("EnterMessage"))
			return ENTRY;
		else if (type.equals("LeaveMessage") || type.equals("KickMessage"))
			return LEAVE;
		else
			return UNSUPPORTED;
	}
	
	public boolean requiresPerson(int type) {
		switch (type) {
		case TEXT:
		case ENTRY:
		case LEAVE:
			return true;
		case TIMESTAMP:
			return false;
		default:
			return false;
		}
	}
	
	public static String todayPath(String room_id) {
		return "/room/" + room_id + "/transcript";
	}
}