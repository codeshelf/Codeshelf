package com.codeshelf.ws.protocol.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class CheDisplayMessage extends MessageABC {
	
	@Getter @Setter
	String line1, line2, line3, line4;
	

	@Getter @Setter
	String netGuidStr;
		
	public CheDisplayMessage() {	
	}
	
	public CheDisplayMessage(String inGuidStr, String inLine1, String inLine2, String inLine3, String inLine4) {	
		netGuidStr = inGuidStr;
		line1 = inLine1==null?"":inLine1;
		line2 = inLine2==null?"":inLine2;
		line3 = inLine3==null?"":inLine3;
		line4 = inLine4==null?"":inLine4;
	}

	@Override
	public String getDeviceIdentifier() {
		return getNetGuidStr();
	}
}
