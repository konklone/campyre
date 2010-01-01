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
	public ArrayList<User> initialUsers = null;
	
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
		
		if (json.has("users")) {
			initialUsers = new ArrayList<User>();
			JSONArray users = json.getJSONArray("users");
			int length = users.length();
			for (int i=0; i<length; i++)
				initialUsers.add(new User(campfire, users.getJSONObject(i)));
		}
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
	
	// convenience function
	public void join() throws CampfireException {
		joinRoom(campfire, id);
	}
	
	public static void joinRoom(Campfire campfire, String roomId) throws CampfireException {
		HttpResponse response = new CampfireRequest(campfire).post(Campfire.joinPath(roomId));
		int statusCode = response.getStatusLine().getStatusCode();
		
		switch(statusCode) {
		case HttpStatus.SC_OK:
			return; // okay!
		case HttpStatus.SC_METHOD_NOT_ALLOWED:
			throw new CampfireException("Error joining room - Campfire connection not using SSL, and should be.");
		default:
			throw new CampfireException("Unknown error trying to join the room.");
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
				throw new CampfireException("Campfire error, message was not sent.");
		} catch(JSONException e) {
			throw new CampfireException(e, "Couldn't create JSON object while speaking.");
		} catch (DateParseException e) {
			throw new CampfireException(e, "Couldn't parse date from created message while speaking.");
		}
	}
	
	public void uploadImage(InputStream stream, String filename, String mimeType) throws CampfireException {
		new CampfireRequest(campfire).uploadFile(Campfire.uploadPath(id), stream, filename, mimeType);
	}

	public String toString() {
		return name;
	}
}