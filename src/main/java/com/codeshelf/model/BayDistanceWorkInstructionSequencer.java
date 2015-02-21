package com.codeshelf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * Work sequencer that orders bays by distance on path, then tiers from top to bottom, and then slots from distance along path, or if non-slotted, position within the tier.
 * 
 */
public class BayDistanceWorkInstructionSequencer extends WorkInstructionSequencerABC {

	private static final Logger LOGGER = LoggerFactory.getLogger(BayDistanceWorkInstructionSequencer.class);
	//TODO utilize location comparator
	@SuppressWarnings("unused")
	private Comparator<Location> locationComparator;

	public BayDistanceWorkInstructionSequencer() {
	
	}
	
	public BayDistanceWorkInstructionSequencer(Comparator<Location> locationComparator) {
		this.locationComparator = locationComparator;
	}
	
	// --------------------------------------------------------------------------
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.codeshelf.model.WISequenceStrategy#sort(com.codeshelf.model.domain.Facility, java.util.List)
	 */
	@Override
	public List<WorkInstruction> sort(Facility facility, List<WorkInstruction> inWiList) {
		List<WorkInstruction> workingWiList = new ArrayList<>(inWiList);
		/*
		Collections.sort(workingWiList, Ordering.from(new PreferredSequenceComparator())
										   .compound(new PosAlongPathComparator())); // Necessary for non-slotted so that sort within one location is good.
		 */
		
		preSortByPosAlongPath(workingWiList);
				
		// Now we need to sort and group the work instructions, so that the CHE can display them by working order.
		List<Location> bayList = new ArrayList<Location>();
		for (Path path : facility.getPaths()) {
			List<Location> baysOnPath = path.<Location> getLocationsByClass(Bay.class);
			for(Location locBay : baysOnPath) {
				//String bayStr = locBay.getNominalLocationId();
				bayList.add(locBay);
			}
			// that was for debugging. quick way:
			// bayList.addAll(path.<Location> getLocationsByClass(Bay.class));
		}
		LOGGER.debug("Sequencing work instructions at "+facility.getDomainId());
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		// Cycle over all bays on the path.
		for (Location subLocation : bayList) {
			//String subLocStr = subLocation.getNominalLocationId();
			for (Location workLocation : subLocation.getSubLocationsInWorkingOrder()) {
				//String workLocStr = workLocation.getNominalLocationId();
				Iterator<WorkInstruction> wiIterator = workingWiList.iterator();
				while (wiIterator.hasNext()) {
					WorkInstruction wi = wiIterator.next();
					Location wiLoc = wi.getLocation();
					//String wiLocStr = wiLoc.getNominalLocationId();
					if (workLocation.equals(wiLoc)) {
						LOGGER.debug("Adding WI "+wi+" at "+workLocation);
						wiResultList.add(wi);
						// WorkInstructionSequencerABC sets the sort code and persists
						wiIterator.remove();
					}
				}
			}
		}
		
		
		//Add all missed instructions with a preferred sequence
		for (WorkInstruction instruction : inWiList){
			if(instruction.getWorkSequence() != null && !wiResultList.contains(instruction)) {
				wiResultList.add(instruction);
			}
		}
		//Sort by preferred sequence
		Collections.sort(wiResultList, Ordering.from(new WorkSequenceComparator()));

		return wiResultList;
	}

	public class DomainIdComparator implements Comparator<WorkInstruction> {

		@Override
		public int compare(WorkInstruction left, WorkInstruction right) {
			return left.getDomainId().compareTo(right.getDomainId());
		}

	}

	public class WorkSequenceComparator implements Comparator<WorkInstruction> {

		public int compare(WorkInstruction left, WorkInstruction right) {
			Integer leftWorkSequence = left.getWorkSequence();
			Integer rightWorkSequence = right.getWorkSequence();
			
			return ComparisonChain.start()
				.compare(leftWorkSequence, rightWorkSequence, Ordering.natural().nullsLast())
				.result();
		}

	}


}
