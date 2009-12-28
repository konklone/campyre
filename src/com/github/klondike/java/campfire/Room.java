package com.github.klondike.java.campfire;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.impl.cookie.DateParseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Room {
	public String id, name;
	public boolean full = false;
	public Campfire campfire;
	
	// For those times when you don't need a whole Room's details,
	// You just have the ID and need a Room function (e.g. uploading a file)
	public Room(Campfire campfire, String id) {
		this.campfire = campfire;
		this.id = id;
	}
	
	protected Room(Campfire campfire, JSONObject json) throws JSONException {
		this.campfire = campfire;
		this.id = json.getString("id");
		this.name = json.getString("name");
		if (json.has("full"))
			this.full = json.getBoolean("full");
	}
	
	public static Room find(Campfire campfire, String id) throws CampfireException {
		try {
			return new Room(campfire, new CampfireRequest(campfire).getOne(Campfire.roomPath(id), "room"));
		} catch(JSONException e) {
			throw new CampfireException(e, "Problem loading Room from the API.");
		}
	}
	
	public static ArrayList<Room> all(Campfire campfire) throws CampfireException, JSONException {
		JSONArray roomList = new CampfireRequest(campfire).getList(Campfire.roomsPath(), "rooms");
		ArrayList<Room> rooms = new ArrayList<Room>();
		
		int length = roomList.length();
		for (int i=0; i<length; i++)
			rooms.add(new Room(campfire, roomList.getJSONObject(i)));
		
		return rooms;
	}
	
	public boolean join() throws CampfireException {
		HttpResponse response = new CampfireRequest(campfire).post(Campfire.joinPath(id));
		int statusCode = response.getStatusLine().getStatusCode();
		
		switch(statusCode) {
		case HttpStatus.SC_OK:
			return true;
		case HttpStatus.SC_METHOD_NOT_ALLOWED:
			throw new CampfireException("It looks like your Campfire account uses SSL. Select \"Clear Credentials\" from the menu to log out and select SSL.");
		default:
			return false;
		}
	}
	
	public Message speak(String body) throws CampfireException {
		String type = (body.contains("\n")) ? "PasteMessage" : "TextMessage";
		String url = Campfire.speakPath(id);
		try {
			String request = new JSONObject().put("message", new JSONObject().put("type", type).put("body", body)).toString();
			HttpResponse response = new CampfireRequest(campfire).post(url, request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode == HttpStatus.SC_CREATED) {
				String responseBody = CampfireRequest.responseBody(response);
				return new Message(new JSONObject(responseBody).getJSONObject("message"));
			} else
				return null;
		} catch(JSONException e) {
			throw new CampfireException(e, "Couldn't create JSON object while speaking.");
		} catch (DateParseException e) {
			throw new CampfireException(e, "Couldn't parse date from created message while speaking.");
		}
	}
	

	public boolean uploadFile(FileInputStream stream) throws CampfireException {
		String filename = "from_phone.jpg";
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        try {
            URL connectURL = new URL(Campfire.uploadPath(id));
            HttpURLConnection conn = (HttpURLConnection) connectURL.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");

            conn.setRequestProperty("User-Agent", CampfireRequest.USER_AGENT);
            conn.setRequestProperty("Connection","Keep-Alive");
            conn.setRequestProperty("Content-Type","multipart/form-data, boundary="+boundary);

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
            dos.close();

            InputStream is = conn.getInputStream();
            int ch;

            StringBuffer b = new StringBuffer();
            while((ch = is.read()) != -1) {
            	b.append((char) ch);
            }

            String s = b.toString();
            return (s.contains("waitForMessage"));
        } catch (IOException e) {
        	throw new CampfireException(e.getClass().getCanonicalName() + "\n" + e.getMessage());
        } 
		
	}

	public String toString() {
		return name;
	}
}