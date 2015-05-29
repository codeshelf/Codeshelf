package com.codeshelf.ws.protocol.message;

import java.util.List;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class ScriptMessage extends MessageABC{
	@Getter
	private UUID id;
	
	@Getter
	private List<String> lines;
	
	@Setter @Getter
	private String response;
	
	@Getter
	private boolean success = true;
	
	@Getter
	private String error;
	
	public ScriptMessage() {}
	
	public ScriptMessage(UUID id, List<String> lines) {
		this.id = id;
		this.lines = lines;
	}
	
	public void setMessageError(String error){
		success = false;
		this.error = error;
	}
	
	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
