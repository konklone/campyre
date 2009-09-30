package com.github.klondike.java.campfire;

/**
 * This class is going to handle each type of possible room event, pivoting on a "type" field.
 * Not all fields will be filled in for each type of event, but that's okay for right now.
 * @author eric
 *
 */
public class RoomEvent {

	public String id, type;
	public String user_id, person;
	public String message;
	
	
	// RoomEvent knows how to construct itself from the HTML returned from Campfire
	public RoomEvent(String original) {
		this.type = "message";
		
		this.id = "id";
		this.user_id = "user_id";
		this.person = "person";
		this.message = original;
	}
	
	public boolean isMessage() {
		return this.type.equals("message"); 
	}
}