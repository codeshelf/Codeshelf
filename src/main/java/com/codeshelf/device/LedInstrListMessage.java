package com.codeshelf.device;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import com.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@ToString
public class LedInstrListMessage extends MessageABC{
	@Accessors(prefix = "m")
	@Getter @Setter @Expose
	@SerializedName(value = "instructions")
	List<LightLedsMessage> mInstructions;

	public LedInstrListMessage() {}
	
	public LedInstrListMessage(List<LightLedsMessage> instructions) {
		this.mInstructions = instructions;
	}
	
	public LedInstrListMessage(LightLedsMessage instruction) {
		this.mInstructions = Lists.newArrayList();
		mInstructions.add(instruction);
	}
}