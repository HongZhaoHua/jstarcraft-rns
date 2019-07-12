package com.jstarcraft.rns.search.exception;

/**
 * 搜索异常
 * 
 * @author Birdy
 */
public class SearchException extends RuntimeException {

    private static final long serialVersionUID = 4344432216739386116L;

    public SearchException() {
        super();
    }

    public SearchException(String message, Throwable exception) {
        super(message, exception);
    }

    public SearchException(String message) {
        super(message);
    }

    public SearchException(Throwable exception) {
        super(exception);
    }

}
