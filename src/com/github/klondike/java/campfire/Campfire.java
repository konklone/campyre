package com.github.klondike.java.campfire;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class Campfire {
	public static final boolean DEBUG = true;
	
	public String username, email, password;
	public boolean ssl;
	
	// used by CampfireRequest
	public String session;
	public String lastResponseBody;
	
	
	public Campfire(String username, String email, String password, boolean ssl) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.ssl = ssl;
	}
	
	public boolean login() throws CampfireException {
    	CampfireRequest request = new CampfireRequest(this);
        
    	// prepare post
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("email_address", email));
        params.add(new BasicNameValuePair("password", password));
        
        HttpResponse response = request.post(loginUrl(), params);
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
        	
        	return true;
        } else {
        	return false;
        }
    	
	}
	
	public boolean speak(String message, String room_id) throws CampfireException {
		CampfireRequest request = new CampfireRequest(this, true);
		
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("message", message));
        params.add(new BasicNameValuePair("t", System.currentTimeMillis() + ""));
        
        HttpResponse response = request.post(speakUrl(room_id), params);
        
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
	
	public boolean joinRoom(String room_id) throws CampfireException {
		CampfireRequest request = new CampfireRequest(this);
		HttpResponse response = request.get(roomUrl(room_id));
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
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
	
	public String speakUrl(String room_id) {
		return rootUrl() + "room/" + room_id + "/speak";
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
	
	private Campfire campfire;
	public boolean ajax;
	
	public CampfireRequest(Campfire campfire) {
		this.campfire = campfire;
		this.ajax = false;
	}
	
	public CampfireRequest(Campfire campfire, boolean ajax) {
		this.campfire = campfire;
		this.ajax = ajax;
	}
	
	public HttpResponse post(String url, List<NameValuePair> params) throws CampfireException {
		HttpPost request = new HttpPost(url);
		
		request.addHeader("User-Agent", USER_AGENT);
		if (campfire.session != null)
			request.addHeader("Cookie", campfire.session);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		
		if (this.ajax) {
			request.addHeader("X-Requested-With", "XMLHttpRequest");
	        request.addHeader("X-Prototype-Version", "1.5.1.1");
		}
		
		try {
        	request.setEntity(new UrlEncodedFormEntity(params));
    		
    		DefaultHttpClient client = new DefaultHttpClient();
    		client.setRedirectHandler(new NoRedirectHandler());
            
    		HttpResponse response = client.execute(request);
        	if (Campfire.DEBUG)
        		campfire.lastResponseBody = EntityUtils.toString(response.getEntity());
        	return response;
		} catch(Exception e) {
        	throw new CampfireException(e);
        }
	}
	
	public HttpResponse get(String url) throws CampfireException {
		HttpGet request = new HttpGet(url);
		
		request.addHeader("User-Agent", USER_AGENT);
		if (campfire.session != null)
			request.addHeader("Cookie", campfire.session);
		
		try {
        	DefaultHttpClient client = new DefaultHttpClient();
        	client.setRedirectHandler(new NoRedirectHandler());
            
            HttpResponse response = client.execute(request);
        	if (Campfire.DEBUG)
        		campfire.lastResponseBody = EntityUtils.toString(response.getEntity());
        	return response;
        } catch(Exception e) {
        	throw new CampfireException(e);
        }
	}
}