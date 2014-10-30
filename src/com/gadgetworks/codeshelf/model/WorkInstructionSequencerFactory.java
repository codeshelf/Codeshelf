package com.gadgetworks.codeshelf.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkInstructionSequencerFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkInstructionSequencerFactory.class);

	public static WorkInstructionSequencerABC createSequencer(WorkInstructionSequencerType type) {
		LOGGER.info("Using " + type.toString() + " sequencer");

		if (type==WorkInstructionSequencerType.BayDistance) {
			return new BayDistanceWorkInstructionSequencer();
		}
		else if (type==WorkInstructionSequencerType.BayDistanceTopLast) {
			return new BayDistanceTopLastWorkInstructionSequencer();
		}
		LOGGER.error("Sequencer type "+type+" is not supported");
		return null;
	}
}
