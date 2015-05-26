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
	
	@Setter @Getter
	private boolean success = true;
	
	public ScriptMessage() {}
	
	public ScriptMessage(UUID id, List<String> lines) {
		this.id = id;
		this.lines = lines;
	}
	
	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
