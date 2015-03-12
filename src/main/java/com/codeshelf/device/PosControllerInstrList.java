package com.codeshelf.device;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PosControllerInstrList extends MessageABC{
	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	@SerializedName(value = "instructions")
	private List<PosControllerInstr>				mInstructions;

	public PosControllerInstrList() {}
	
	public PosControllerInstrList(List<PosControllerInstr> instructions) {
		this.mInstructions = instructions;
	}
}
