package com.codeshelf.ws.protocol.message;

public class PosConShowAddresses extends DeviceMessageABC{
	public PosConShowAddresses() {}
	
	public PosConShowAddresses(String netGuidStr) {
		setNetGuidStr(netGuidStr);
	}
}
