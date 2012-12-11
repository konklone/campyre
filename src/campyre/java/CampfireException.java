package campyre.java;

public class CampfireException extends Exception {
    private static final long serialVersionUID = -2623309261327198087L;

    public CampfireException(String msg) {
    	super(msg);
    }

    public CampfireException(Exception e, String msg) {
    	super(msg, e);
    }
}
