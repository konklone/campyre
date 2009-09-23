package com.github.klondike.java.campfire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class Campfire {
	private static final String USER_AGENT = "android-campfire (http://github.com/Klondike/android-campfire";
	
	public String username, email, password;
	public boolean ssl;
	
	private String session;
	public String lastResponseBody;
	
	
	public Campfire(String username, String email, String password, boolean ssl) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.ssl = ssl;
	}
	
	public boolean login() throws CampfireException {
    	try {
	    	HttpPost request = new HttpPost(loginUrl());
	        
	    	// prepare post
	        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
	        params.add(new BasicNameValuePair("email_address", email));
	        params.add(new BasicNameValuePair("password", password));
	        request.setEntity(new UrlEncodedFormEntity(params));
	        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
	        
	        request.addHeader("User-Agent", USER_AGENT);
        
	        DefaultHttpClient client = new DefaultHttpClient();
	        client.setRedirectHandler(new NoRedirectHandler());
	        HttpResponse response = client.execute(request);
	        
	        int status = response.getStatusLine().getStatusCode();
	        Header locationHeader = response.getFirstHeader("location");
	        String location = "";
	        if (locationHeader != null) 
	        	location = locationHeader.getValue();
	        
	        lastResponseBody = EntityUtils.toString(response.getEntity());
	        
	        if (status == HttpStatus.SC_MOVED_TEMPORARILY && location.equals(rootUrl())) {
	        	// store session cookie, quick!
	        	Header cookieHeader = response.getFirstHeader("set-cookie");
	        	if (cookieHeader == null)
	        		throw new CampfireException("I think I logged in, but I got no cookie to set.");	        	
	        	session = cookieHeader.getValue();
	        	
	        	return true;
	        } else {
	        	return false;
	        }
    	} catch(Exception e) {
    		throw new CampfireException(e);
    	}
	}
	
	public boolean joinRoom(String room_id) throws CampfireException {
		CampfireRequest request = new CampfireRequest(session);
        HttpResponse response = request.get(roomUrl(room_id));
        
        int status = response.getStatusLine().getStatusCode();
        try {
        	lastResponseBody = EntityUtils.toString(response.getEntity());
        } catch(IOException e) {
        	throw new CampfireException(e);
        }
	        
	    return (status == HttpStatus.SC_OK);
	}
	
	public String rootUrl() {
		return protocol() + "://" + username + ".campfirenow.com/";
	}
	
	public String loginUrl() {
		return rootUrl() + "login";
	}
	
	public String roomUrl(String room_id) {
		return rootUrl() + "room/" + room_id;
	}
	
	public String protocol() {
        if (ssl)
        	return "https";
        else
        	return "http";
	}
}

/**
 * Tiny redirect handler you can give to an HTTP client to stop it from following redirects. 
 * This can be used to distinguish between successful and unsuccessful login attempts,
 * by looking at the status code and the Location header.
 *
 */
class NoRedirectHandler extends DefaultRedirectHandler {
	
	@Override
	public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
		return false;
	}
	
}

class CampfireRequest {
	private static final String USER_AGENT = "android-campfire (http://github.com/Klondike/android-campfire";
	
	private String session;
	private HttpUriRequest request;
	
	public CampfireRequest(String session) {
		this.session = session;
	}
	
	public HttpResponse get(String url) throws CampfireException {
		request = new HttpGet(url);
		addHeaders();
        DefaultHttpClient client = new DefaultHttpClient();
        client.setRedirectHandler(new NoRedirectHandler());
        try {
        	return client.execute(request);
        } catch(Exception e) {
    		throw new CampfireException(e);
    	}
	}
	
	private void addHeaders() {
		request.addHeader("User-Agent", USER_AGENT);
		if (session != null)
			request.addHeader("Cookie", session);
	}
}