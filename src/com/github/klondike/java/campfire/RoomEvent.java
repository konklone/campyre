package com.github.klondike.java.campfire;

/**
 * This class is going to handle each type of possible room event, pivoting on a "type" field.
 * Not all fields will be filled in for each type of event, but that's okay for right now.
 * @author eric
 *
 */
public class RoomEvent {
	private static final int MESSAGE = 0;
	private static final int PASTE = 1;
	private static final int ENTRY = 2;
	
	public int type;
	public String id, user_id, person;
	public String message;
	
	
	// RoomEvent knows how to construct itself from the HTML returned from Campfire
	public RoomEvent(String original) {
		this.type = MESSAGE;
		
		this.id = "id";
		this.user_id = "user_id";
		this.person = "person";
		this.message = original;
	}
	
	public String toString() {
		return message;
	}
}