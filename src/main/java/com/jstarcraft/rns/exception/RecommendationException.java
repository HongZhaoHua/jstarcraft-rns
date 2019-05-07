package com.jstarcraft.rns.exception;

/**
 * 推荐异常
 * 
 * @author Birdy
 *
 */
public class RecommendationException extends RuntimeException {

	private static final long serialVersionUID = 4072415788185880975L;

	public RecommendationException(String message) {
		super(message);
	}

	public RecommendationException(Throwable exception) {
		super(exception);
	}

	public RecommendationException(String message, Throwable exception) {
		super(message, exception);
	}

}
