package com.gadgetworks.codeshelf.ws.jetty.protocol.message;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;

@ToString
public class LightLedsMessage extends MessageABC {
	
	@SuppressWarnings("unused")
	private static final Logger		LOGGER									= LoggerFactory.getLogger(LightLedsMessage.class);
	
	@Getter @Setter
	String ledCommands;
	

	@Getter @Setter
	String netGuidStr;

	@Getter @Setter
	int durationSeconds;

	public LightLedsMessage() {	
	}
	public LightLedsMessage(String inGuidStr, int inDurationSeconds, String inCommands) {	
		ledCommands = inCommands;
		netGuidStr = inGuidStr;
		durationSeconds = inDurationSeconds;
	}
	
	// Just a checker. Seems we are failing on this. Normal use is to pass in the getLedCommands().
	public static boolean verifyCommandString(String inCommandString) {
		List<LedCmdGroup> dsLedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(inCommandString);
		return LedCmdGroupSerializer.verifyLedCmdGroupList(dsLedCmdGroups);

	}

}
