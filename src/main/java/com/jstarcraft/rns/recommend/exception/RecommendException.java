package com.jstarcraft.rns.recommend.exception;

/**
 * 推荐异常
 * 
 * @author Birdy
 *
 */
public class RecommendException extends RuntimeException {

	private static final long serialVersionUID = 4072415788185880975L;

	public RecommendException(String message) {
		super(message);
	}

	public RecommendException(Throwable exception) {
		super(exception);
	}

	public RecommendException(String message, Throwable exception) {
		super(message, exception);
	}

}
