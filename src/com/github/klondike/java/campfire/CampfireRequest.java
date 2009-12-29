package com.github.klondike.java.campfire;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import lgpl.haustein.Base64Encoder;

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
				request.addHeader("Content-type", "application/json");
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
		credsProvider.setCredentials(new AuthScope(domain(), (campfire.ssl ? 443 : 80)), credentials);
		
		DefaultHttpClient client = new DefaultHttpClient();
		client.setCredentialsProvider(credsProvider);
        
        try {
        	return client.execute(request);
		} catch (ClientProtocolException e) {
			throw new CampfireException(e, "ClientProtocolException while making request to: " + request.getURI().toString());
		} catch (IOException e) {
			throw new CampfireException(e, "IOException while making request to: " + request.getURI().toString());
		}
	}
    
    public static String responseBody(HttpResponse response) throws CampfireException {
		int statusCode = response.getStatusLine().getStatusCode();
		
		try {
	        if (statusCode >= 200 && statusCode < 300)
	        	return EntityUtils.toString(response.getEntity());
	        else
	        	throw new CampfireException("Bad status code: " + statusCode);
		} catch(IOException e) {
			throw new CampfireException(e, "IOException while reading body of HTTP response.");
		}
	}
	
	public String domain() {
		return campfire.subdomain + ".campfirenow.com";
	}
	
	public String url(String path) {
		return url(path, this.format);
	}
	
	public String url(String path, String format) {
		return (campfire.ssl ? "https" : "http") + "://" + domain() + path + format;
	}
	
	public boolean uploadFile(String path, FileInputStream stream, String extension, String mimeType) throws CampfireException {
		String filename = "from_phone." + extension;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "---------------------------XXX";
        
        try {
            URL connectURL = new URL(url(path, ".xml"));
            HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");

            String token = campfire.token + ":" + "X";
    		String encoding = Base64Encoder.encode(token);
    		conn.setRequestProperty("Authorization", "Basic " + encoding);
            
            conn.setRequestProperty("User-Agent", CampfireRequest.USER_AGENT);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            
            // file header
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            // OH MY GOD the space between the semicolon and "filename=" is ABSOLUTELY NECESSARY
            dos.writeBytes("Content-Disposition: form-data; name=\"upload\"; filename=\"" + filename + "\"" + lineEnd);
            dos.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
            dos.writeBytes("Content-Type: " + mimeType + lineEnd);
            dos.writeBytes(lineEnd);

            // insert file
            int bytesAvailable = stream.available();
            int maxBufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];
            int bytesRead = stream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = stream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = stream.read(buffer, 0, bufferSize);
            }
            
            // file closer
            dos.writeBytes(lineEnd);
            
            // send multipart form data necesssary after file data...            
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // close streams
            stream.close();
            dos.flush();
            dos.close();

            int responseCode = conn.getResponseCode();
            
            return responseCode == HttpStatus.SC_CREATED;
        } catch (IOException e) {
        	throw new CampfireException(e.getClass().getCanonicalName() + "\n" + e.getMessage());
        } 
		
	}
}