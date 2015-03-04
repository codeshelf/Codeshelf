package com.codeshelf.ws.jetty.protocol.message;

import lombok.Getter;

import com.codeshelf.device.PosControllerInstr;

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
