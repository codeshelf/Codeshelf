package com.gadgetworks.codeshelf.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class HousekeepingInjector {

	private static final Logger LOGGER = LoggerFactory.getLogger(HousekeepingInjector.class);
	
	private static boolean kWantHK_REPEATPOS = true;
	private static boolean kWantHK_BAYCOMPLETE = true;

	private HousekeepingInjector() {

	}
	
	private static boolean shouldCreateThisHK(WorkInstructionTypeEnum inType){
		switch (inType) {			
			case HK_REPEATPOS:
				return kWantHK_REPEATPOS;
			case HK_BAYCOMPLETE:
				return kWantHK_BAYCOMPLETE;
			default:
				LOGGER.error("shouldCreateThisHK unknown case");
				return false;
		}		
	}
	
	public static boolean getShouldCreateThisHK(WorkInstructionTypeEnum inType) {
		switch (inType) {			
			case HK_REPEATPOS:
				return kWantHK_REPEATPOS;
			case HK_BAYCOMPLETE:
				return kWantHK_BAYCOMPLETE;
			default:
				LOGGER.error("getShouldCreateThisHK unknown case");
				return false;
		}		
	}

	public static void setCreateThisHK(WorkInstructionTypeEnum inType, boolean inValue){
		switch (inType) {			
			case HK_REPEATPOS:
				kWantHK_REPEATPOS = inValue;
			case HK_BAYCOMPLETE:
				kWantHK_BAYCOMPLETE = inValue;
			default:
				LOGGER.error("setCreateThisHK unknown case");
		}		
	}
	
	public static void restoreHKDefaults(){
		kWantHK_REPEATPOS = true;
		kWantHK_BAYCOMPLETE = true;
	}
	public static void turnOffHK(){
		kWantHK_REPEATPOS = false;
		kWantHK_BAYCOMPLETE = false;
	}

	public static WorkInstructionSequencerABC createSequencer(WorkInstructionSequencerType type) {
		if (type==WorkInstructionSequencerType.BayDistance) {
			return new BayDistanceWorkInstructionSequencer();
		}
		else if (type==WorkInstructionSequencerType.BayDistanceTopLast) {
			return new BayDistanceTopLastWorkInstructionSequencer();
		}
		LOGGER.error("Sequencer type "+type+" is not supported");
		return null;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Helper function. Adds to existing list, or creates the list if null
	 * @param inList
	 * @param inEnum
	 * @return
	 */
	private static List<WorkInstructionTypeEnum> addHouseKeepEnumToList(List<WorkInstructionTypeEnum> inList,
		WorkInstructionTypeEnum inEnum) {
		if (inList != null) {
			inList.add(inEnum);
			return inList;
		} else {
			List<WorkInstructionTypeEnum> returnList = new ArrayList<WorkInstructionTypeEnum>();
			returnList.add(inEnum);
			return returnList;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Are the workInstructions coming/going to the same cart position? If so, the user may be confused about the button press. We want an intervening housekeeping WI
	 * Somewhat similar story for bay-change. And later for aisle/path segment change.
	 * Returns null if no WI needed. The enum of the right type if a housekeep is needed. Notice that it returns a list. It can return two or more separate housekeeps to insert.
	 * @param prevWi
	 * @param nextWi
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<WorkInstructionTypeEnum> wisNeedHouseKeepingBetween(WorkInstruction inPrevWi, WorkInstruction inNextWi) {
		List<WorkInstructionTypeEnum> returnList = null;

		if (inPrevWi == null)
			return null;
		else if (inNextWi == null) {
			LOGGER.error("null value in wisNeedHouseKeepingBetween");
			return null;
		} else {

			// container is associated to cart position. User cares about same cart position twice in a row.
			if (kWantHK_REPEATPOS && inPrevWi.getContainer().equals(inNextWi.getContainer())) {
				// Nothing we can do on server side if multiple items will be recorded to same cart position.
				returnList = addHouseKeepEnumToList(returnList, WorkInstructionTypeEnum.HK_REPEATPOS);
			}

			if (kWantHK_BAYCOMPLETE) {
				// This can be tricky. Crossbatch put WI may have multiple locations. Initial implementation will not be completely right if the multiple locations span across bays.
				// In our model, the WI.location field in this case is the arbitrary "first" location of all the locations for the outbound order.
				ILocation loc1 = inPrevWi.getLocation();
				ILocation loc2 = inNextWi.getLocation();
				if (loc1 == null || loc2 == null)
					LOGGER.error("unanticipated case in wisNeedHouseKeepingBetween");
				else {
					ILocation bay1 = loc1.getParentAtLevel(Bay.class);
					ILocation bay2 = loc2.getParentAtLevel(Bay.class);
					if (bay1 != null && bay2 != null && !bay1.equals(bay2))
						returnList = addHouseKeepEnumToList(returnList, WorkInstructionTypeEnum.HK_BAYCOMPLETE);
				}
			}
		}
		return returnList;
	}

	// --------------------------------------------------------------------------
	/**
	 * This will add any necessary housekeeping WIs. And then add the sort and group codes and save each WI.
	 * @param inSortedWiList
	 * @return
	 */
	public static List<WorkInstruction> addHouseKeepingAndSaveSort(Facility inFacility, List<WorkInstruction> inSortedWiList) {
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		WorkInstruction lastWi = null;
		for (WorkInstruction wi : inSortedWiList) {
			List<WorkInstructionTypeEnum> theHousekeepingTypeList = wisNeedHouseKeepingBetween(lastWi, wi);
			// returns null if nothing to do. If non-null, then at lease one in the list.
			if (theHousekeepingTypeList != null) {
				for (WorkInstructionTypeEnum theType : theHousekeepingTypeList) {
					WorkInstruction houseKeepingWi = WiFactory.createHouseKeepingWi(theType, inFacility, lastWi, wi);
					if (houseKeepingWi != null) {
						wiResultList.add(houseKeepingWi);
						LOGGER.info("adding housekeeping WI type " + houseKeepingWi.getDescription());
					}
					else
						LOGGER.debug("null returned from getNewHousekeepingWiOfType");
				}
			}
			wiResultList.add(wi);
			lastWi = wi;
		}
		// At this point, some or none added. wiResultList is ordered. Time to add the sort codes.
		int count = 0;
		for (WorkInstruction wi : wiResultList) {
			count++;
			wi.setGroupAndSortCode(String.format("%04d", count));
			WorkInstruction.DAO.store(wi);
		}

		return wiResultList;
	}

}