package com.github.klondike.java.campfire;

import java.util.ArrayList;

import org.json.JSONArray;


public class Campfire {	
	public String subdomain, token;
	public boolean ssl;
		
	public Campfire(String subdomain, String token, boolean ssl) {
		this.subdomain = subdomain;
		this.token = token;
		this.ssl = ssl;
	}
	
	public boolean validate() throws CampfireException {
		// check /users/me.json
		return true;
	}
	
	public Room[] getRooms() throws CampfireException {
		ArrayList<Room> rooms = new ArrayList<Room>();
		
		JSONArray roomList = new CampfireRequest(this).getArray(roomsPath());
		
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