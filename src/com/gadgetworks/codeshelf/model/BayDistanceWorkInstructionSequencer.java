package com.gadgetworks.codeshelf.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Location;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

/**
 * Work sequencer that orders bays by distance on path, then tiers from top to bottom, and then slots from distance along path, or if non-slotted, position within the tier.
 * 
 */
public class BayDistanceWorkInstructionSequencer extends WorkInstructionSequencerABC {

	private static final Logger LOGGER = LoggerFactory.getLogger(BayDistanceWorkInstructionSequencer.class);

	public BayDistanceWorkInstructionSequencer() {
	
	}
	
	// --------------------------------------------------------------------------
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.WISequenceStrategy#sort(com.gadgetworks.codeshelf.model.domain.Facility, java.util.List)
	 */
	@Override
	public List<WorkInstruction> sort(Facility facility, List<WorkInstruction> inWiList) {

		preSortByPosAlongPath(inWiList); // Necessary for non-slotted so that sort within one location is good.
				
		// Now we need to sort and group the work instructions, so that the CHE can display them by working order.
		List<Location> bayList = new ArrayList<Location>();
		for (Path path : facility.getPaths()) {
			List<Location> baysOnPath = path.<Location> getLocationsByClass(Bay.class);
			for(Location locBay : baysOnPath) {
				String bayStr = locBay.getNominalLocationId();
				bayList.add(locBay);
			}
			// that was for debugging. quick way:
			// bayList.addAll(path.<Location> getLocationsByClass(Bay.class));
		}
		LOGGER.debug("Sequencing work instructions at "+facility.getDomainId());
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		// Cycle over all bays on the path.
		for (Location subLocation : bayList) {
			String subLocStr = subLocation.getNominalLocationId();
			for (Location workLocation : subLocation.getSubLocationsInWorkingOrder()) {
				String workLocStr = workLocation.getNominalLocationId();
				Iterator<WorkInstruction> wiIterator = inWiList.iterator();
				while (wiIterator.hasNext()) {
					WorkInstruction wi = wiIterator.next();
					Location wiLoc = wi.getLocation();
					String wiLocStr = wiLoc.getNominalLocationId();
					if (wiLoc.equals(workLocation)) {
						LOGGER.debug("Adding WI "+wi+" at "+workLocation);
						wiResultList.add(wi);
						// WorkInstructionSequencerABC sets the sort code and persists
						wiIterator.remove();
					}
				}
			}
		}
		return wiResultList;
	}
}
