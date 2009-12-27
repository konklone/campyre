package com.github.klondike.java.campfire;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Campfire {	
	public String subdomain, token;
	public String user_id;
	public boolean ssl;
		
	public Campfire(String subdomain, String token, boolean ssl) {
		this.subdomain = subdomain;
		this.token = token;
		this.ssl = ssl;
		this.user_id = null;
	}
	
	public Campfire(String subdomain, String token, boolean ssl, String user_id) {
		this.subdomain = subdomain;
		this.token = token;
		this.ssl = ssl;
		this.user_id = user_id;
	}
	
	public boolean login() throws CampfireException, JSONException {
		HttpResponse response = new CampfireRequest(this).get(checkPath());
		// if API key is wrong, we'll get a 401 status code (HttpStatus.SC_UNAUTHORIZED)
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			JSONObject user = new JSONObject(CampfireRequest.responseBody(response)).getJSONObject("user");
			this.user_id = user.getString("id");
			return true;
		} else
			return false;
	}
	
	public Room[] getRooms() throws CampfireException, JSONException {
		JSONArray roomList = new CampfireRequest(this).getList(roomsPath(), "rooms");
		ArrayList<Room> rooms = new ArrayList<Room>();
		
		int length = roomList.length();
		for (int i=0; i<length; i++)
			rooms.add(new Room(this, roomList.getJSONObject(i)));
		
		return rooms.toArray(new Room[0]);
	}
	
	public static String checkPath() {
		return "/users/me";
	}
	
	public static String roomPath(String room_id) {
		return "/room/" + room_id;
	}
	
	public static String roomsPath() {
		return "/rooms";
	}
	
	public static String userPath(String user_id) {
		return "/users/" + user_id;
	}
	
	public static String joinPath(String room_id) {
		return roomPath(room_id) + "/join";
	}
	
	public static String speakPath(String room_id) {
		return roomPath(room_id) + "/speak";
	}
	
	public static String uploadPath(String room_id) {
		return roomPath(room_id) + "/uploads";
	}
	
}