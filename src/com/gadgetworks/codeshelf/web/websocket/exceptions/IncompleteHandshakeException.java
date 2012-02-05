package com.gadgetworks.codeshelf.web.websocket.exceptions;

public class IncompleteHandshakeException extends RuntimeException {

	public IncompleteHandshakeException() {
		super();
	}

	public IncompleteHandshakeException(String message, Throwable cause) {
		super(message, cause);
	}

	public IncompleteHandshakeException(String message) {
		super(message);
	}

	public IncompleteHandshakeException(Throwable cause) {
		super(cause);
	}

}
