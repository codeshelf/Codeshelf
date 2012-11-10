package com.gadgetworks.codeshelf.web.websocket;

import com.gadgetworks.codeshelf.web.websocket.exceptions.InvalidFrameException;

public interface Framedata {
	enum Opcode {
		CONTINIOUS,
		TEXT,
		BINARY,
		PING,
		PONG,
		CLOSING
		// more to come
	}

	boolean isFin();

	boolean getTransfereMasked();

	Opcode getOpcode();

	// TODO the separation of the application data and the extension data is yet to be done
	byte[] getPayloadData();

	void append(Framedata nextframe) throws InvalidFrameException;
}
