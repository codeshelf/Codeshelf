package com.codeshelf.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.PropertyBehavior;
import com.codeshelf.model.domain.Facility;

public class WorkInstructionSequencerFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkInstructionSequencerFactory.class);

	public static WorkInstructionSequencerABC createSequencer(Facility facility) {
		String sequenceKind = PropertyBehavior.getProperty(facility, FacilityPropertyType.WORKSEQR);
		WorkInstructionSequencerType sequenceKindEnum = WorkInstructionSequencerType.parse(sequenceKind);
		LOGGER.info("Using " + sequenceKindEnum + " sequencer");

		if (WorkInstructionSequencerType.BayDistance.equals(sequenceKindEnum)) {
			return new BayDistanceWorkInstructionSequencer();
		}
		else if (WorkInstructionSequencerType.WorkSequence.equals(sequenceKindEnum)) {
			return new WorkSequenceWorkInstructionSequencer();
		}
		LOGGER.error("Sequencer type "+sequenceKindEnum +" is not supported");
		return null;
	}
}
