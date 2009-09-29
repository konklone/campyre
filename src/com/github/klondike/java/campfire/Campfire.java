package com.github.klondike.java.campfire;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;


public class Campfire {
	public static final boolean DEBUG = true;
	
	public String subdomain, email, password;
	public boolean ssl;
	
	// used by CampfireRequest
	public String session;
	public String lastResponseBody;
	
	
	public Campfire(String subdomain, String email, String password, boolean ssl) {
		this.subdomain = subdomain;
		this.email = email;
		this.password = password;
		this.ssl = ssl;
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
	
	public boolean uploadFile(String room_id, FileInputStream stream) throws CampfireException {
		String filename = "from_phone.jpg";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        try {
            URL connectURL = new URL(uploadUrl(room_id));
            HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Connection","Keep-Alive");
            conn.setRequestProperty("Content-Type","multipart/form-data, boundary="+boundary);
            conn.setRequestProperty("Cookie", session);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            
            // submit header
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"submit\"" + lineEnd);
            dos.writeBytes(lineEnd);
            // insert submit
            dos.writeBytes("Upload");
            // submit closer
            dos.writeBytes(lineEnd);
            
            // file header
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            // OH MY GOD the space between the semicolon and "filename=" is ABSOLUTELY NECESSARY
            dos.writeBytes("Content-Disposition: form-data; name=\"upload\"; filename=\"" + filename + "\"" + lineEnd);
            dos.writeBytes("Content-Transfer-Encoding: binary" + lineEnd);
            dos.writeBytes("Content-Type: image/jpg" + lineEnd);
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

            InputStream is = conn.getInputStream();
            int ch;

            StringBuffer b = new StringBuffer();
            while((ch = is.read()) != -1) {
            	b.append((char) ch);
            }

            String s = b.toString();
            dos.close();
            return (s.contains("waitForMessage"));
        } catch (IOException e) {
        	throw new CampfireException(e.getMessage());
        } 
		
	}
	
	public boolean uploadFile(String room_id, String path) throws CampfireException {
		try {
			return uploadFile(room_id, new FileInputStream(path));
		} catch (FileNotFoundException e) {
			throw new CampfireException("File not found: " + path);
		}
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
		if (ssl)
			return "https";
		else
			return "http";
	}
}