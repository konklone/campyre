package com.awesome.campfire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TextView;

public class MainMenu extends Activity {
    //public final static String   
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Resources res = getResources();
        
        String username = res.getString(R.string.campfire_username);
        String email = res.getString(R.string.campfire_username);
        String password = res.getString(R.string.campfire_username);
        String ssl = res.getString(R.string.campfire_username);
        String room_id = res.getString(R.string.campfire_username);
        String protocol = "http";
        
        String output = "Initialized";
        
        if (ssl.equals("true"))
        	protocol = "https";
        
        
    	String url = protocol + "://" + username + ".campfirenow.com";
    	BasicHttpParams params = (BasicHttpParams) new BasicHttpParams()
    		.setParameter("email", email)
    		.setParameter("password", password);
    	
    	HttpGet request = new HttpGet(url);
        request.setParams(params);
        
        try {
	        HttpClient client = new DefaultHttpClient();
	        HttpResponse response = client.execute(request);
	        int status = response.getStatusLine().getStatusCode();
	        if (status == HttpStatus.SC_OK)
	        	output = getText(response.getEntity().getContent());
	        else
	        	output = "Nope: " + status;
	        
    	} catch(ClientProtocolException e) {
    		output = e.getMessage();
    	} catch(IOException e) {
    		output = e.getMessage();
    	}
    	
        
        TextView debug = (TextView) findViewById(R.id.debug);
        debug.setText(output);
    }
    
    public static String getText(InputStream in) {
	    String text = "";
	    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	    StringBuilder sb = new StringBuilder();
	    String line = null;
	    
	    try {
		    while((line = reader.readLine()) != null) {
		    	sb.append(line + "\n");
		    }
		    text = sb.toString();
	    }
	    catch(Exception e) {}
	    finally {
		    try {in.close();}
		    catch(Exception e) {}
	    }
	    return text;
    }
}