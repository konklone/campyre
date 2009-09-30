package com.github.klondike.java.campfire;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class CampfireRequest {
	private static final String USER_AGENT = "android-campfire (http://github.com/Klondike/android-campfire";
	
	private Campfire campfire;
	private List<NameValuePair> params;
	public boolean ajax;
	
	public CampfireRequest(Campfire campfire) {
		this.campfire = campfire;
		this.ajax = false;
		this.params = new ArrayList<NameValuePair>();
	}
	
	public CampfireRequest(Campfire campfire, boolean ajax) {
		this.campfire = campfire;
		this.ajax = ajax;
		this.params = new ArrayList<NameValuePair>();
	}
	
	public void addParam(String key, String value) {
		params.add(new BasicNameValuePair(key, value));
	}
	
	public HttpResponse post(String url) throws CampfireException {
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