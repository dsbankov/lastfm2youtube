package com.codingintune.lastfm2youtube;

public class YouTubeServiceException extends Exception {

	private static final long serialVersionUID = 7142862776626348890L;

	public YouTubeServiceException(Throwable e) {
		super("youtube api is not responding... i've probably exceeded my quota. try again after approximately 24 hours.", e);
	}

}
