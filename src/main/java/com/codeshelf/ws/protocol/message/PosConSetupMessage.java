package com.codeshelf.ws.protocol.message;

public class PosConSetupMessage extends DeviceMessageABC{
	
	public PosConSetupMessage() {}
	
	public PosConSetupMessage(String netGuidStr) {
		setNetGuidStr(netGuidStr);
	}
}
