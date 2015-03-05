package com.codeshelf.ws.jetty.protocol.message;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.device.PosControllerInstr;

public class PosConControllerMessage extends MessageABC{	
	@Getter
	private String netGuidStr;
	
	@Getter @Setter
	private boolean removeAll = false;
	
	@Getter @Setter
	private List<Byte> removePos;
	
	@Getter
	private PosControllerInstr instruction;
	
	public PosConControllerMessage() {}
	
	public PosConControllerMessage(String netGuidStr, PosControllerInstr instruction, boolean removeAll, List<Byte> removePos){
		this.netGuidStr = netGuidStr;
		this.instruction = instruction;
		this.removeAll = removeAll;
		this.removePos = removePos;
	}
}
