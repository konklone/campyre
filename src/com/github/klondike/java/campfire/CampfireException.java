package com.github.klondike.java.campfire;

public class CampfireException extends Exception {
    private static final long serialVersionUID = -2623309261327198087L;
    
    public CampfireException(Exception e) {
    	super(e);
    }
    
    public CampfireException(String msg) {
    	super(msg);
    }
}
