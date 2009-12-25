package com.github.klondike.java.campfire;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CampfireRequest {
	public static final String USER_AGENT = "android-campfire (http://github.com/Klondike/android-campfire";
	private String format = ".json";
	
	private Campfire campfire;
	
	public CampfireRequest(Campfire campfire) {
		this.campfire = campfire;
	}
	
	public HttpResponse post(String url) throws CampfireException {
		HttpPost request = new HttpPost(url);
		
		request.addHeader("User-Agent", USER_AGENT);
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		
		try {
        	//request.setEntity(new UrlEncodedFormEntity(params));
    		
    		DefaultHttpClient client = new DefaultHttpClient();
    		client.setRedirectHandler(new NoRedirectHandler());
            
    		HttpResponse response = client.execute(request);
        	return response;
		} catch(Exception e) {
        	throw new CampfireException(e);
        }
	}
	
	public HttpResponse get(String url) throws CampfireException {
		HttpGet request = new HttpGet(url);
		
		request.addHeader("User-Agent", USER_AGENT);
		
		try {
        	DefaultHttpClient client = new DefaultHttpClient();
        	client.setRedirectHandler(new NoRedirectHandler());
            
            HttpResponse response = client.execute(request);
        	return response;
        } catch(Exception e) {
        	throw new CampfireException(e);
        }
	}
	
	public JSONObject getOne(String path) throws CampfireException {
		String json = getJSON(path);
		try {
			return new JSONObject(json);
		} catch (JSONException e) {
			throw new CampfireException(e);
		}
	}
	
	public JSONArray getArray(String path) throws CampfireException {
		String json = getJSON(path);
		try {
			return new JSONArray(json);
		} catch (JSONException e) {
			throw new CampfireException(e);
		}
	}
	
	public String getJSON(String path) throws CampfireException {
		HttpGet request = new HttpGet(url(path));
        request.addHeader("User-Agent", USER_AGENT);
		
        DefaultHttpClient client = new DefaultHttpClient();
        try {
	        HttpResponse response = client.execute(request);
	        int statusCode = response.getStatusLine().getStatusCode();
	        
	        if (statusCode == HttpStatus.SC_OK) {
	        	String body = EntityUtils.toString(response.getEntity());
	        	return body;
	        } else {
	        	throw new CampfireException("Bad status code");
	        }
        } catch (Exception e) {
	    	throw new CampfireException(e);
	    }
	}
	
	public String domain() {
		return (campfire.ssl ? "https" : "http") + "://" + campfire.subdomain + ".campfirenow.com";
	}
	
	public String url(String path) {
		return domain() + path + format;
	}
}

class NoRedirectHandler extends DefaultRedirectHandler {
	
	@Override
	public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
		return false;
	}
	
}