package com.codeshelf.model;

public enum WorkInstructionSequencerType {
	BayDistance,
	BayDistanceTopLast;
	
	 @Override
	  public String toString() {
	    switch(this) {
	      case BayDistance: return "BayDistance";
	      case BayDistanceTopLast: return "BayDistanceTopLast";
	      default: throw new IllegalArgumentException();
	    }
	  }
}


