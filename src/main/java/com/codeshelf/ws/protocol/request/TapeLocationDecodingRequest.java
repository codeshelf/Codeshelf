package com.codeshelf.ws.protocol.request;

import lombok.Getter;

public class TapeLocationDecodingRequest extends DeviceRequest{
	@Getter
	private String tapeId;
	
	public TapeLocationDecodingRequest() {}
	
	public TapeLocationDecodingRequest(String cheId, String tapeId) {
		setDeviceId(cheId);
		this.tapeId = tapeId;
	}
}
