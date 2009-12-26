package com.github.klondike.java.campfire;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
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
	
	public JSONObject getOne(String path, String key) throws CampfireException, JSONException {
		return new JSONObject(responseBody(get(path))).getJSONObject(key);
	}
	
	public JSONArray getList(String path, String key) throws CampfireException, JSONException {
		return new JSONObject(responseBody(get(path))).getJSONArray(key);
	}
	
	public HttpResponse get(String path) throws CampfireException {
        return makeRequest(new HttpGet(url(path)));
	}
	
	public HttpResponse post(String path) throws CampfireException {
		return post(path, null);
	}
	
	public HttpResponse post(String path, String body) throws CampfireException {
		HttpPost request = new HttpPost(url(path));
		
		if (body != null) {
			try {
				request.setEntity(new StringEntity(body));
			} catch(UnsupportedEncodingException e) {
				throw new CampfireException(e, "Unsupported encoding on posting to: " + path);
			}
		}
		
		return makeRequest(request);
	}
        
    public HttpResponse makeRequest(HttpUriRequest request) throws CampfireException {
    	request.addHeader("User-Agent", USER_AGENT);
    	
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
    
    public static String responseBody(HttpResponse response) throws CampfireException {
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
	
	public String domain() {
		return campfire.subdomain + ".campfirenow.com";
	}
	
	public String url(String path) {
		return (campfire.ssl ? "https" : "http") + "://" + domain() + path + format;
	}
}

class NoRedirectHandler extends DefaultRedirectHandler {
	
	@Override
	public boolean isRedirectRequested(HttpResponse response, HttpContext context) {
		return false;
	}
	
}