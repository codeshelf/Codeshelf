package com.codeshelf.ws.jetty.protocol.message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.LedSample;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

@ToString
public class LightLedsInstruction {
	
	@SuppressWarnings("unused")
	private static final Logger		LOGGER									= LoggerFactory.getLogger(LightLedsInstruction.class);
	
	@Getter @Setter
	String ledCommands;
	

	@Getter @Setter
	String netGuidStr;


	@Getter @Setter
	short channel;
	
	@Getter @Setter
	int durationSeconds;
	
	public LightLedsInstruction() {	
	}
	
	public LightLedsInstruction(String inGuidStr, short inChannel, int inDurationSeconds, List<LedCmdGroup> inCommands) {	
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

	public LightLedsInstruction merge(LightLedsInstruction ledInstruction) {
		Preconditions.checkArgument(this.netGuidStr.equals(ledInstruction.netGuidStr), "controller guids not the same");
		Preconditions.checkArgument(this.channel == ledInstruction.channel, "channel not the same");
		Preconditions.checkArgument(this.durationSeconds == ledInstruction.durationSeconds, "durationSeconds not the same");
		List<LedCmdGroup> thisLedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledCommands);
		List<LedCmdGroup> otherLedCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledInstruction.ledCommands);
		List<LedCmdGroup> combinedGroup = Lists.newArrayList();
		combinedGroup.addAll(thisLedCmdGroups);
		combinedGroup.addAll(otherLedCmdGroups);
		LightLedsInstruction newInstruction = new LightLedsInstruction(this.netGuidStr, this.channel, this.durationSeconds, combinedGroup);
		newInstruction.removeDuplicates();
		return newInstruction;
	}

	private void removeDuplicates(){
		List<LedCmdGroup> ledCmdGroups = LedCmdGroupSerializer.deserializeLedCmdString(ledCommands);
		List<LedCmdGroup> groupsToRemove = Lists.newArrayList();
		Set<LedSample> uniqueSamples = new HashSet<LedSample>(); 
		for (LedCmdGroup cmdGroup : ledCmdGroups) {
			List<LedSample> sampleList = cmdGroup.getLedSampleList();
			List<LedSample> samplesToRemove = Lists.newArrayList();
			for (LedSample sample : sampleList) {
				if (uniqueSamples.contains(sample)) {
					samplesToRemove.add(sample);
				} else {
					uniqueSamples.add(sample);
				}
			}
			for (LedSample sample : samplesToRemove) {
				sampleList.remove(sample);
			}
			if (sampleList.isEmpty()) {
				groupsToRemove.add(cmdGroup);
			}
		}
		for (LedCmdGroup group : groupsToRemove) {
			ledCmdGroups.remove(group);
		}
		setLedCommands(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroups));
	}
}
