package com.gadgetworks.codeshelf.ws.jetty.protocol.message;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

@ToString
public class LightLedsMessage extends MessageABC {
	
	@SuppressWarnings("unused")
	private static final Logger		LOGGER									= LoggerFactory.getLogger(LightLedsMessage.class);
	
	@Getter @Setter
	String ledCommands;
	

	@Getter @Setter
	String netGuidStr;


	@Getter @Setter
	short channel;
	
	@Getter @Setter
	int durationSeconds;
	
	public LightLedsMessage() {	
	}
	
	public LightLedsMessage(String inGuidStr, short inChannel, int inDurationSeconds, List<LedCmdGroup> inCommands) {	
		ledCommands = LedCmdGroupSerializer.serializeLedCmdString(inCommands);
		netGuidStr = inGuidStr;
		channel = inChannel;
		durationSeconds = inDurationSeconds;
	}
	
	// Just a checker. Seems we are failing on this. Normal use is to pass in the getLedCommands().
	public static boolean verifyCommandString(String inCommandString) {
		List<LedCmdGroup> dsLedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(inCommandString);
		return LedCmdGroupSerializer.verifyLedCmdGroupList(dsLedCmdGroups);

	}

	public LightLedsMessage merge(LightLedsMessage ledMessage) {
		Preconditions.checkArgument(this.netGuidStr.equals(ledMessage.netGuidStr), "controller guids not the same");
		Preconditions.checkArgument(this.channel == ledMessage.channel, "channel not the same");
		Preconditions.checkArgument(this.durationSeconds == ledMessage.durationSeconds, "durationSeconds not the same");
		List<LedCmdGroup> thisLedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledCommands);
		List<LedCmdGroup> otherLedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledMessage.ledCommands);
		List<LedCmdGroup> combinedGroup = Lists.newArrayList();
		combinedGroup.addAll(thisLedCmdGroups);
		combinedGroup.addAll(otherLedCmdGroups);
		return new LightLedsMessage(this.netGuidStr, this.channel, this.durationSeconds, combinedGroup);
	}

}
