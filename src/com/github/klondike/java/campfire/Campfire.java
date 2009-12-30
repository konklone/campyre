package com.github.klondike.java.campfire;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
		HttpResponse response = new CampfireRequest(this).get(mePath());
		int statusCode = response.getStatusLine().getStatusCode();
		// if API key is wrong, we'll get a 401 status code (HttpStatus.SC_UNAUTHORIZED)
		// if the Campfire needs SSL, we'll get a _____, so refetch it at that URL
		// if it gets a 200, then save the info from the response
		if (statusCode == HttpStatus.SC_OK) {
			JSONObject user = new JSONObject(CampfireRequest.responseBody(response)).getJSONObject("user");
			this.user_id = user.getString("id");
			return true;
		} else
			return false;
	}
	
	public static String mePath() {
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