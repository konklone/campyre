package com.github.klondike.java.campfire;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

public class Room {
	private Campfire campfire;
	private String id;
	
	public Room(Campfire campfire, String id) {
		this.campfire = campfire;
		this.id = id;
	}
	
	/* Main methods */
	
	public boolean join() throws CampfireException {
		CampfireRequest request = new CampfireRequest(campfire);
		HttpResponse response = request.get(roomUrl());
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
	
	public boolean speak(String message) throws CampfireException {
		CampfireRequest request = new CampfireRequest(campfire, true);
		
		request.addParam("message", message);
		request.addParam("t", System.currentTimeMillis() + "");
        
		if (message.contains("\n") == true)
        	request.addParam("paste", "1");
        
        HttpResponse response = request.post(speakUrl());
        
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
	
	/* Routes */
	
	public String roomUrl() {
		return campfire.rootUrl() + "room/" + id;
	}
	
	public String speakUrl() {
		return roomUrl() + "/speak";
	}
	
	public String uploadUrl() {
		return campfire.rootUrl() + "upload.cgi/room/" + id + "/uploads/new";
	}
}