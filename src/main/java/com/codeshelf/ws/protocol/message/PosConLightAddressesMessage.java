package com.codeshelf.ws.protocol.message;

public class PosConLightAddressesMessage extends DeviceMessageABC{
	public PosConLightAddressesMessage() {}
	
	public PosConLightAddressesMessage(String netGuidStr) {
		setNetGuidStr(netGuidStr);
	}
}
