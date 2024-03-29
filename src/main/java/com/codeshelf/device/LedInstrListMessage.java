package com.codeshelf.device;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import com.codeshelf.ws.protocol.message.LightLedsInstruction;
import com.codeshelf.ws.protocol.message.MessageABC;
import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ToString
public class LedInstrListMessage extends MessageABC{
	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	@SerializedName(value = "instructions")
	List<LightLedsInstruction> mInstructions;

	public LedInstrListMessage() {}
	
	public LedInstrListMessage(List<LightLedsInstruction> instructions) {
		this.mInstructions = instructions;
	}
	
	public LedInstrListMessage(LightLedsInstruction instruction) {
		this.mInstructions = Lists.newArrayList();
		mInstructions.add(instruction);
	}

	@Override
	public String getDeviceIdentifier() {
		return null;
	}
}