package com.github.klondike.java.campfire;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is going to handle each type of possible room event, pivoting on a "type" field.
 * Not all fields will be filled in for each type of event, but that's okay for right now.
 * @author eric
 *
 */
public class Message {
	public static final int TEXT = 0;
	public static final int TIMESTAMP = 1;
	public static final int ENTRY = 2;
	
	public int type;
	public String id, user_id, person;
	public String body;
	
	public Message(int type, String id, String user_id, String person, String body) {
		this.type = type;
		this.id = id;
		this.person = person;
		this.user_id = user_id;
		this.body = body;
	}
	
	public String toString() {
		if (type == ENTRY)
			return person + " " + body;
		else if (type == TIMESTAMP)
			return body;
		else // if (type == TEXT)
			return person + ": " + body;
	}
	
	// Message knows how to construct itself from the HTML returned from Campfire
	public static Message fromPoll(String body) {
		String user_id = null;
		String person = null;
		String message;
		
		int type = typeFor(extract("(\\w+)_message", body));
		String id = extract("id=\\\\\"message_(\\d+)\\\\\"", body);
		
		if (type == TIMESTAMP)
			message = extract("\\\\u003Ctd class=\\\\\"time\\\\\"\\\\u003E\\\\u003Cdiv\\\\u003E(.+?)\\\\u003C/div\\\\u003E", body);
		else {
			message = extract("\\\\u003Ctd class=\\\\\"body\\\\\"\\\\u003E\\\\u003Cdiv\\\\u003E(.+?)\\\\u003C/div\\\\u003E", body);
			user_id = extract("user_(\\d+)", body);
			person = extract("\\\\u003Ctd class=\\\\\"person\\\\\"\\\\u003E(?:\\\\u003Cspan\\\\u003E)?(.+?)(?:\\\\u003C/span\\\\u003E)?\\\\u003C/td\\\\u003E", body);
		}
		
		return new Message(type, id, user_id, person, message);
	}
	
	public static Message fromStart(String body) {
		String user_id = null;
		String person = null;
		String message;
		
		String id = extract("id=\"message_(\\d+)\"", body);
		int type = typeFor(extract("class=\"(.*?)_message", body));
		
		if (type == TIMESTAMP) {
			String day = extract("td class=\"date\"><span>(.*?)</span>", body);
			message = extract("td class=\"time\"><div>(.*?)</div>", body);
			if (day != null) message = day + ", " + message;
		} else {
			user_id = extract("user_(\\d+)[\"\\s]", body);
			person = extract("td class=\"person\">(?:<span(?:\\s*style=\"display:\\s*none\")?>)?(.*?)(?:</span>)?</td>", body);
			message = extract("td class=\"body\"><div>(.*?)</div>", body);
		}
		
		return new Message(type, id, user_id, person, message);
	}
	
	private static String extract(String regex, String source) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source);
		if (matcher.find())
			return matcher.group(1);
		else
			return null;
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
}