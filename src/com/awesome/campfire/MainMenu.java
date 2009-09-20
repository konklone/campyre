package com.awesome.campfire;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

public class MainMenu extends Activity {
    //public final static String   
	
	private String session;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Resources res = getResources();
        
        String username = res.getString(R.string.campfire_username);
        String email = res.getString(R.string.campfire_email);
        String password = res.getString(R.string.campfire_password);
        String ssl = res.getString(R.string.campfire_ssl);
        String room_id = res.getString(R.string.campfire_room_id);
        String protocol = "http";
        
        String output = "Initialized";
        
        if (ssl.equals("true"))
        	protocol = "https";
        
        
    	String baseUrl = protocol + "://" + username + ".campfirenow.com/";
    	
    	String url = baseUrl + "login";
    	
    	try {
	    	HttpPost request = new HttpPost(url);
	        
	        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
	        params.add(new BasicNameValuePair("email_address", email));
	        params.add(new BasicNameValuePair("password", password));
	        request.setEntity(new UrlEncodedFormEntity(params));
	               
	        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
	        request.addHeader("User-Agent", "android-campfire (http://github.com/Klondike/android-campfire");
        
	        DefaultHttpClient client = new DefaultHttpClient();
	        client.setRedirectHandler(new NoRedirectHandler());
	        HttpResponse response = client.execute(request);
	        
	        int status = response.getStatusLine().getStatusCode();
	        Header locationHeader = response.getFirstHeader("location");
	        String location = "";
	        if (locationHeader != null) location = locationHeader.getValue();
	        
	        if (status == HttpStatus.SC_MOVED_TEMPORARILY && location.equals(baseUrl)) {
	        	output = "Logged in successfully, redirected to:";
	        	output += "\n" + location;
	        	
	        	Header cookieHeader = response.getFirstHeader("set-cookie");
	        	if (cookieHeader != null)
	        		session = cookieHeader.getValue();
        	
	        } else {
	        	output = "Not logged in, using email [" + email + "]- response body:\n";
	        	output += EntityUtils.toString(response.getEntity());
	        }
    	} catch(ClientProtocolException e) {
    		output += "\n\nCPError: " + e.getMessage();
    	} catch(IOException e) {
    		output += "\n\nIOError: " + e.getMessage();
    	}
    	
        
        TextView debug = (TextView) findViewById(R.id.debug);
        debug.setText(output);
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