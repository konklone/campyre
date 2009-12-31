package com.github.klondike.java.campfire;

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.cookie.DateParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Room {
	public String id, name;
	public boolean full = false;
	public Campfire campfire;
	
	// For those times when you don't need a whole Room's details,
	// You just have the ID and need a Room function (e.g. uploading a file)
	public Room(Campfire campfire, String id) {
		this.campfire = campfire;
		this.id = id;
	}
	
	protected Room(Campfire campfire, JSONObject json) throws JSONException {
		this.campfire = campfire;
		this.id = json.getString("id");
		this.name = json.getString("name");
		if (json.has("full"))
			this.full = json.getBoolean("full");
	}
	
	public static Room find(Campfire campfire, String id) throws CampfireException {
		try {
			return new Room(campfire, new CampfireRequest(campfire).getOne(Campfire.roomPath(id), "room"));
		} catch(JSONException e) {
			throw new CampfireException(e, "Problem loading room from the API.");
		}
	}
	
	public static ArrayList<Room> all(Campfire campfire) throws CampfireException {
		ArrayList<Room> rooms = new ArrayList<Room>();
		try {
			JSONArray roomList = new CampfireRequest(campfire).getList(Campfire.roomsPath(), "rooms");
			int length = roomList.length();
			for (int i=0; i<length; i++)
				rooms.add(new Room(campfire, roomList.getJSONObject(i)));
		} catch(JSONException e) {
			throw new CampfireException(e, "Problem loading room list from the API.");
		}
		
		return rooms;
	}
	
	public boolean join() throws CampfireException {
		HttpResponse response = new CampfireRequest(campfire).post(Campfire.joinPath(id));
		int statusCode = response.getStatusLine().getStatusCode();
		
		switch(statusCode) {
		case HttpStatus.SC_OK:
			return true;
		case HttpStatus.SC_METHOD_NOT_ALLOWED:
			throw new CampfireException("It looks like your Campfire account uses SSL. Select \"Clear Credentials\" from the menu to log out and select SSL.");
		default:
			return false;
		}
	}
	
	public Message speak(String body) throws CampfireException {
		String type = (body.contains("\n")) ? "PasteMessage" : "TextMessage";
		String url = Campfire.speakPath(id);
		try {
			String request = new JSONObject().put("message", new JSONObject().put("type", type).put("body", body)).toString();
			HttpResponse response = new CampfireRequest(campfire).post(url, request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_CREATED) {
				String responseBody = CampfireRequest.responseBody(response);
				return new Message(new JSONObject(responseBody).getJSONObject("message"));
			} else
				return null;
		} catch(JSONException e) {
			throw new CampfireException(e, "Couldn't create JSON object while speaking.");
		} catch (DateParseException e) {
			throw new CampfireException(e, "Couldn't parse date from created message while speaking.");
		}
	}
	
	public boolean uploadImage(InputStream stream, String filename, String mimeType) throws CampfireException {
		return new CampfireRequest(campfire).uploadFile(Campfire.uploadPath(id), stream, filename, mimeType);
	}

	public String toString() {
		return name;
	}
}