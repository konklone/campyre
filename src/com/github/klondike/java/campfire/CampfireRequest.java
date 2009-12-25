package com.github.klondike.java.campfire;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
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
	
	public JSONObject getOne(String path) throws CampfireException, JSONException {
		return new JSONObject(getContent(path));
	}
	
	public JSONArray getList(String path, String key) throws CampfireException, JSONException {
		String json = getContent(path);
		return new JSONObject(json).getJSONArray(key);
	}
	
	public String getContent(String path) throws CampfireException {
		HttpResponse response = getResponse(path);
		int statusCode = response.getStatusLine().getStatusCode();
		
		try {
	        if (statusCode == HttpStatus.SC_OK)
	        	return EntityUtils.toString(response.getEntity());
	        else
	        	throw new CampfireException("Bad status code: " + statusCode);
		} catch(IOException e) {
			throw new CampfireException(e);
		}
	}
	
	// for testing
	public static String toString(HttpResponse response) throws CampfireException {
		int statusCode = response.getStatusLine().getStatusCode();
		
		try {
	        if (statusCode == HttpStatus.SC_OK)
	        	return EntityUtils.toString(response.getEntity());
	        else
	        	throw new CampfireException("Bad status code: " + statusCode);
		} catch(IOException e) {
			throw new CampfireException(e);
		}
	}
	
	public HttpResponse getResponse(String path) throws CampfireException {
		HttpGet request = new HttpGet(url(path));
        request.addHeader("User-Agent", USER_AGENT);
        
		// set auth credentials
		Credentials credentials = new UsernamePasswordCredentials(campfire.token, "X");
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(domain(), 80), credentials);
 
		DefaultHttpClient client = new DefaultHttpClient();
		client.setCredentialsProvider(credsProvider);
        
        try {
			return client.execute(request);
		} catch (ClientProtocolException e) {
			throw new CampfireException(e);
		} catch (IOException e) {
			throw new CampfireException(e);
		}
	}
	
	public String domain() {
		return campfire.subdomain + ".campfirenow.com";
	}
	
	public String url(String path) {
		return (campfire.ssl ? "https" : "http") + "://" + domain() + path + format;
	}
	
	
	/* TO BE REPLACED */
	
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
}

class NoRedirectHandler extends DefaultRedirectHandler {
	
	@Override
	public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
		return false;
	}
	
}