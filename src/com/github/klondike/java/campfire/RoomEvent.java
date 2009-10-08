package com.github.klondike.java.campfire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is going to handle each type of possible room event, pivoting on a "type" field.
 * Not all fields will be filled in for each type of event, but that's okay for right now.
 * @author eric
 *
 */
public class RoomEvent {
	public static final int TEXT = 0;
	public static final int TIMESTAMP = 1;
	public static final int ENTRY = 2;
	
	private static final boolean DEBUG = true;
	
	public int type;
	public String id, user_id, person;
	public String message;
	public String body;
	
	
	// RoomEvent knows how to construct itself from the HTML returned from Campfire
	public RoomEvent(String body) {
		this.body = body;
		
		// common to all types
		this.type = typeFor(extractBody("(\\w+)_message"));
		this.id = extractBody("id=\\\\\"message_(\\d+)\\\\\"");
		
		if (type == TIMESTAMP)
			this.message = extractBody("\\\\u003Ctd class=\\\\\"time\\\\\"\\\\u003E\\\\u003Cdiv\\\\u003E(.+?)\\\\u003C/div\\\\u003E");
		else {
			this.message = extractBody("\\\\u003Ctd class=\\\\\"body\\\\\"\\\\u003E\\\\u003Cdiv\\\\u003E(.+?)\\\\u003C/div\\\\u003E");
			this.user_id = extractBody("user_(\\d+)");
			this.person = extractBody("\\\\u003Ctd class=\\\\\"person\\\\\"\\\\u003E(?:\\\\u003Cspan\\\\u003E)?(.+?)(?:\\\\u003C/span\\\\u003E)?\\\\u003C/td\\\\u003E");
		}
		
		// backups
		this.id = (this.id != null ? this.id : "unknown");
		this.user_id = (this.user_id != null ? this.user_id : "unknown");
		this.person = (this.person != null ? this.person : "unknown");
		this.message = (this.message != null ? this.message : (DEBUG ? this.body : "[Bug: could not parse message.]"));
	}
	
	public String toString() {
		return message;
	}
	
	private static int typeFor(String type) {
		if (type == null)
			return TEXT;
		else if (type.equals("text"))
			return TEXT;
		else if (type.equals("timestamp"))
			return TIMESTAMP;
		else if (type.equals("leave"))
			return ENTRY;
		else if (type.equals("enter"))
			return ENTRY;
		else if (type.equals("kick"))
			return ENTRY;
		else
			return TEXT;
	}
	
	private static String extract(String regex, String source) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source);
		if (matcher.find())
			return matcher.group(1);
		else
			return null;
	}
	
	private String extractBody(String regex) {
		return extract(regex, body);
	}
}