package com.codeshelf.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.service.PropertyService;

public class WorkInstructionSequencerFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkInstructionSequencerFactory.class);

	public static WorkInstructionSequencerABC createSequencer(Facility facility) {
		String sequenceKind = PropertyService.getInstance().getPropertyFromConfig(facility, DomainObjectProperty.WORKSEQR);
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
