package com.codeshelf.ws.protocol.message;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

public class PickScriptMessage extends MessageABC{
	@Getter
	private UUID id;
	
	@Getter
	private String script;
	
	
	@Setter @Getter
	private String responseMessage;
	
	public PickScriptMessage() {}
	
	public PickScriptMessage(UUID id, String script) {
		this.id = id;
		this.script = script;
	}
	
	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}
