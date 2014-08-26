package com.gadgetworks.codeshelf.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

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
public class BayDistanceTopLastWorkInstructionSequencer implements WorkInstructionSequencer {
	
	public BayDistanceTopLastWorkInstructionSequencer() {
	
	}
	
	// --------------------------------------------------------------------------
	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see com.gadgetworks.codeshelf.model.WISequenceStrategy#sort(com.gadgetworks.codeshelf.model.domain.Facility, java.util.List)
	 */
	@Override
	public List<WorkInstruction> sort(Facility facility, List<WorkInstruction> crosswallWiList) {
		// Now we need to sort and group the work instructions, so that the CHE can display them by working order.
		List<ISubLocation<?>> bayList = new ArrayList<ISubLocation<?>>();
		for (Path path : facility.getPaths()) {
			bayList.addAll(path.<ISubLocation<?>> getLocationsByClass(Bay.class));
		}
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		// Cycle over all bays on the path skipping the top tiers
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
						continue;
					}
					Iterator<WorkInstruction> wiIterator = crosswallWiList.iterator();
					while (wiIterator.hasNext()) {
						WorkInstruction wi = wiIterator.next();
						if (wi.getLocation().equals(workLocation)) {
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
						continue;
					}
					Iterator<WorkInstruction> wiIterator = crosswallWiList.iterator();
					while (wiIterator.hasNext()) {
						WorkInstruction wi = wiIterator.next();
						if (wi.getLocation().equals(workLocation)) {
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
