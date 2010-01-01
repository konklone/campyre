package com.github.klondike.java.campfire;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;


public class Campfire {
	// Change this to use your own user agent
	public static final String USER_AGENT = "android-campfire (http://github.com/Klondike/android-campfire)";
	
	public String subdomain, token;
	public String user_id = null;
	public boolean ssl = false;
		
	public Campfire(String subdomain, String token) {
		this.subdomain = subdomain;
		this.token = token;
	}
	
	public Campfire(String subdomain, String token, boolean ssl, String user_id) {
		this.subdomain = subdomain;
		this.token = token;
		this.ssl = ssl;
		this.user_id = user_id;
	}
	
	public void login() throws CampfireException {
		HttpResponse response = new CampfireRequest(this, false).get(mePath());
		int statusCode = response.getStatusLine().getStatusCode();
		// if API key is wrong, we'll get a 401 status code (HttpStatus.SC_UNAUTHORIZED)
		// if the Campfire needs SSL, we'll get a 302, so relogin as an SSL-enabled Campfire
		// if it gets a 200, then save the info from the response
		switch (statusCode) {
		case HttpStatus.SC_OK:
			try {
				JSONObject user = new JSONObject(CampfireRequest.responseBody(response)).getJSONObject("user");
				this.user_id = user.getString("id");
			} catch (JSONException e) {
				throw new CampfireException(e, "Couldn't load user details on login.");
			}
			break;
		case HttpStatus.SC_MOVED_TEMPORARILY:
			if (this.ssl) // not sure why this would happen, but I'm cautious about infinite loops
				throw new CampfireException("Unknown redirect error on login.");
			else {
				this.ssl = true;
				login();
			}
			break;
		case HttpStatus.SC_UNAUTHORIZED:
			throw new CampfireException("Invalid credentials.");
		default:
			throw new CampfireException("Unknown error code " + statusCode + " on login.");
		}
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