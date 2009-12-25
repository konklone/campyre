package com.github.klondike.java.campfire;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;


public class Campfire {	
	public String subdomain, token;
	public boolean ssl;
		
	public Campfire(String subdomain, String token, boolean ssl) {
		this.subdomain = subdomain;
		this.token = token;
		this.ssl = ssl;
	}
	
	public boolean validate() throws CampfireException {
		HttpResponse response = new CampfireRequest(this).getResponse(checkPath());
		// if API key is wrong, we'll get a 401 status code (HttpStatus.SC_UNAUTHORIZED)
		return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
	}
	
	public Room[] getRooms() throws CampfireException, JSONException {
		JSONArray roomList = new CampfireRequest(this).getList(roomsPath(), "rooms");
		ArrayList<Room> rooms = new ArrayList<Room>();
		
		int length = roomList.length();
		for (int i=0; i<length; i++)
			rooms.add(new Room(this, roomList.getJSONObject(i)));
		
		return rooms.toArray(new Room[0]);
	}
	
	public String checkPath() {
		return "/users/me";
	}
	
	public String roomPath(String room_id) {
		return "/room/" + room_id;
	}
	
	public String roomsPath() {
		return "/rooms";
	}
	
}