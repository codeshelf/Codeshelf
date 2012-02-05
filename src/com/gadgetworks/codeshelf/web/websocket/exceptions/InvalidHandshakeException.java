package com.gadgetworks.codeshelf.web.websocket.exceptions;

public class InvalidHandshakeException extends InvalidDataException {

	public InvalidHandshakeException() {
	}

	public InvalidHandshakeException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public InvalidHandshakeException(String arg0) {
		super(arg0);
	}

	public InvalidHandshakeException(Throwable arg0) {
		super(arg0);
	}

}
