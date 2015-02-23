package com.codeshelf.model;

public enum WorkInstructionSequencerType {
	BayDistance("BayDistance"),
	WorkSequence("WorkSequence");
	
	private final String name;
	
	private WorkInstructionSequencerType(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}

	public static WorkInstructionSequencerType parse(String sequenceKind) {
		for (WorkInstructionSequencerType enumValue : WorkInstructionSequencerType.values()) {
			if (enumValue.name.equalsIgnoreCase(sequenceKind)) {
				return enumValue;
			}
		}
		return null;
	}
}


