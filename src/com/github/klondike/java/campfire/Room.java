package com.github.klondike.java.campfire;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;

import android.os.PatternMatcher;

public class Room {
	public String id, name;
	public boolean joined;
	public String body;
	
	private Campfire campfire;
	
	public String membershipKey;
	public String userId;
	public String lastCacheId;
	public String timestamp;
	public long idleSince; // kept in seconds
	
	
	public Room(Campfire campfire, String id) {
		this.campfire = campfire;
		this.id = id;
		this.name = null;
		this.joined = false;
		this.body = null;
		
		this.membershipKey = null;
		this.userId = null;
		this.lastCacheId = null;
		this.timestamp = null;
	}
	
	/* Main methods */
	
	public boolean join() throws CampfireException {
		CampfireRequest request = new CampfireRequest(campfire);
		HttpResponse response = request.get(roomUrl());
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			try {
				this.body = EntityUtils.toString(response.getEntity());
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			
			this.name = extractBody("<h1 id=\"room_name\">(.*?)</h1>"); 
			this.membershipKey = extractBody("\"membershipKey\":\\s?\"([a-z0-9]+)\"");
			this.userId = extractBody("\"userID\":\\s?(\\d+)");
			this.lastCacheId = extractBody("\"lastCacheID\":\\s?(\\d+)");
			this.timestamp = extractBody("\"timestamp\":\\s?(\\d+)");
			this.idleSince = System.currentTimeMillis() * 1000;
			
			this.joined = true;
			return true;
		} else
			return false;
	}
	
	public String toString() {
		return name;
	}
	
	private String extract(String regex, String source) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source);
		matcher.find();
		return matcher.group(1);
	}
	
	private String extractBody(String regex) {
		return extract(regex, this.body);
	}
	
	public boolean speak(String message) throws CampfireException {
		CampfireRequest request = new CampfireRequest(campfire, true);
		
		request.addParam("message", message);
		request.addParam("t", System.currentTimeMillis() + "");
        
		if (message.contains("\n") == true)
        	request.addParam("paste", "1");
        
        HttpResponse response = request.post(speakUrl());
        
		return (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	}
	
	public boolean uploadFile(FileInputStream stream) throws CampfireException {
		String filename = "from_phone.jpg";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        try {
            URL connectURL = new URL(uploadUrl());
            HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");

            conn.setRequestProperty("Connection","Keep-Alive");
            conn.setRequestProperty("Content-Type","multipart/form-data, boundary="+boundary);
            conn.setRequestProperty("Cookie", campfire.session);

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
	
	/* Routes */
	
	public String roomUrl() {
		return campfire.rootUrl() + "room/" + id;
	}
	
	public String speakUrl() {
		return roomUrl() + "/speak";
	}
	
	public String uploadUrl() {
		return campfire.rootUrl() + "upload.cgi/room/" + id + "/uploads/new";
	}
}