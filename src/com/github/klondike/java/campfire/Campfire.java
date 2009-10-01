package com.github.klondike.java.campfire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;


public class Campfire {	
	public String subdomain, email, password;
	public boolean ssl;
	
	// used by CampfireRequest
	public String session;
	
	
	public Campfire(String subdomain, String email, String password, boolean ssl) {
		this.subdomain = subdomain;
		this.email = email;
		this.password = password;
		this.ssl = ssl;
		this.session = null;
	}
	
	public Campfire(String subdomain, String email, String password, boolean ssl, String session) {
		this.subdomain = subdomain;
		this.email = email;
		this.password = password;
		this.ssl = ssl;
		this.session = session;
	}
	
	public String login() throws CampfireException {
    	CampfireRequest request = new CampfireRequest(this);
    	
    	request.addParam("email_address", email);
    	request.addParam("password", password);
        
        HttpResponse response = request.post(loginUrl());
        int status = response.getStatusLine().getStatusCode();
        
        Header locationHeader = response.getFirstHeader("location");
        String location = "";
        if (locationHeader != null) 
        	location = locationHeader.getValue();
        
        if (status == HttpStatus.SC_MOVED_TEMPORARILY && location.equals(rootUrl())) {
        	// store session cookie, quick!
        	Header cookieHeader = response.getFirstHeader("set-cookie");
        	if (cookieHeader == null)
        		throw new CampfireException("I think I logged in, but I got no cookie to set.");	        	
        	session = cookieHeader.getValue();
        	
        	return session;
        } else {
        	return null;
        }
	}
	
	public Room[] getRooms() throws CampfireException {
		ArrayList<Room> rooms = new ArrayList<Room>();
		
		CampfireRequest request = new CampfireRequest(this);
		HttpResponse response = request.get(rootUrl());
		String body;
		try {
			body = EntityUtils.toString(response.getEntity());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
		
		Pattern pattern = Pattern.compile("<a href=\"" + rootUrl() + "room/(\\d+)\">(.*?)</a>");
		Matcher matcher = pattern.matcher(body);
		while (matcher.find()) {
			String id = matcher.group(1);
			String name = matcher.group(2);
			rooms.add(new Room(this, id, name));
		}
		
		return rooms.toArray(new Room[0]);
	}
	
	public boolean loggedIn() {
		return session != null;
	}
	
	public String rootUrl() {
		return protocol() + "://" + subdomain + ".campfirenow.com/";
	}
	
	public String loginUrl() {
		return rootUrl() + "login";
	}
	
	public String roomUrl(String room_id) {
		return rootUrl() + "room/" + room_id;
	}
	
	public String speakUrl(String room_id) {
		return rootUrl() + "room/" + room_id + "/speak";
	}
	
	public String uploadUrl(String room_id) {
		return rootUrl() + "upload.cgi/room/" + room_id + "/uploads/new";
	}
	
	public String protocol() {
		return ssl ? "https" : "http";
	}
}