package com.codeshelf.ws.jetty.protocol.message;

import com.codeshelf.device.PosControllerInstr;

import lombok.Getter;

public class PosConControllerMessage extends MessageABC{	
	@Getter
	private String netGuidStr;
	
	@Getter
	private PosControllerInstr instruction;
	
	public PosConControllerMessage() {}
	
	public PosConControllerMessage(String netGuidStr, PosControllerInstr instruction){
		this.netGuidStr = netGuidStr;
		this.instruction = instruction;
	}
	
}
