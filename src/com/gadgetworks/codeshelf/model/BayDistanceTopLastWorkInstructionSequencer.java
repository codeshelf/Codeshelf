package com.gadgetworks.codeshelf.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

/**
 * Work sequencer that orders bays by distance on path, then tiers from top to bottom, and then slots from distance along path
 * 
 */
public class BayDistanceTopLastWorkInstructionSequencer extends WorkInstructionSequencerABC {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(BayDistanceTopLastWorkInstructionSequencer.class);

	public BayDistanceTopLastWorkInstructionSequencer() {
	
	}
	
	// --------------------------------------------------------------------------
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.WISequenceStrategy#sort(com.gadgetworks.codeshelf.model.domain.Facility, java.util.List)
	 */
	@Override
	public List<WorkInstruction> sort(Facility facility, List<WorkInstruction> inWiList) {
		
		// preSortByPosAlongPath(inWiList); // Necessary for non-slotted so that sort within one location is good.
		
		// Now we need to sort and group the work instructions, so that the CHE can display them by working order.
		List<ISubLocation<?>> bayList = new ArrayList<ISubLocation<?>>();
		for (Path path : facility.getPaths()) {
			bayList.addAll(path.<ISubLocation<?>> getLocationsByClass(Bay.class));
		}
		LOGGER.debug("Sequencing work instructions at "+facility.getDomainId());
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		// Cycle over all bays on the path skipping the top tiers
		//LOGGER.debug("Processing lower tiers...");
		for (ISubLocation<?> subLocation : bayList) {
			List<ILocation<?>> tiers = subLocation.getSubLocationsInWorkingOrder();
			int numTiers = tiers.size();
			if (numTiers>0) {
				// remember the first tier which is the top one
				ILocation<?> lastTier = tiers.get(0);
				// loop through tiers skipping the last one
				for (ILocation<?> workLocation : tiers) {
					if (workLocation.equals(lastTier)) {
						// skip last tier for later processing
						// LOGGER.debug("Skipping tier "+workLocation);
						continue;
					}
					// LOGGER.debug("Processing tier "+workLocation);
					Iterator<WorkInstruction> wiIterator = inWiList.iterator();
					while (wiIterator.hasNext()) {
						WorkInstruction wi = wiIterator.next();
						if (wi.getLocation().equals(workLocation)) {
							LOGGER.debug("Adding WI "+wi+" at "+workLocation);
							wiResultList.add(wi);
							wi.setGroupAndSortCode(String.format("%04d", wiResultList.size()));
							WorkInstruction.DAO.store(wi);
							wiIterator.remove();
						}
					}
				}
			}
		}
		// now cycle through top tiers
		LOGGER.debug("Processing top tier...");
		for (ISubLocation<?> subLocation : bayList) {
			List<ILocation<?>> tiers = subLocation.getSubLocationsInWorkingOrder();
			int numTiers = tiers.size();
			if (numTiers>0) {
				// remember the first tier which is the top one
				ILocation<?> lastTier = tiers.get(0);
				// loop through tiers skipping the last one
				for (ILocation<?> workLocation : tiers) {
					if (!workLocation.equals(lastTier)) {
						// skip tier, if not top one
						// LOGGER.debug("Skipping tier "+workLocation);
						continue;
					}
					// LOGGER.debug("Processing tier "+workLocation);
					Iterator<WorkInstruction> wiIterator = inWiList.iterator();
					while (wiIterator.hasNext()) {
						WorkInstruction wi = wiIterator.next();
						if (wi.getLocation().equals(workLocation)) {
							LOGGER.debug("Adding WI "+wi+" at "+workLocation);
							wiResultList.add(wi);
							wi.setGroupAndSortCode(String.format("%04d", wiResultList.size()));
							WorkInstruction.DAO.store(wi);
							wiIterator.remove();
						}
					}
				}
			}
		}
		return wiResultList;
	}
}
