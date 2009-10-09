package com.github.klondike.java.campfire;

public class CampfireFile {
	public String fileName;
	public String fileURL;
	public int fileID;
	
	public CampfireFile(String fileName, Integer fileID, String fileURL) {
		this.fileID = fileID;
		this.fileName = fileName;
		this.fileURL = fileURL;
	}
	
	public CampfireFile(String fileURL) {
		this.fileURL = fileURL;
		
		// /room/38896/uploads/898737/from_phone.jpg
		String bits[] = fileURL.split("/");
		// we're assuming that the URL format is always the same. So far, it always is.
		this.fileID = Integer.decode(bits[3]);
		this.fileName = bits[4];
	}
	
	public String GetFileExtension() {
		return this.fileName.substring(this.fileName.lastIndexOf("."));
	}
}