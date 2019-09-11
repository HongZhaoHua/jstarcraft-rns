package com.jstarcraft.rns.model.exception;

/**
 * 推荐异常
 * 
 * @author Birdy
 *
 */
public class ModelException extends RuntimeException {

    private static final long serialVersionUID = 4072415788185880975L;

    public ModelException(String message) {
        super(message);
    }

    public ModelException(Throwable exception) {
        super(exception);
    }

    public ModelException(String message, Throwable exception) {
        super(message, exception);
    }

}
