package com.codeshelf.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.manager.Tenant;
import com.codeshelf.model.domain.Bay;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.PropertyService;

public class HousekeepingInjector {
	// For multi-tenancy, this must convert from a static usage object to having one HousekeepingInjector per facility.

	private static final Logger	LOGGER	= LoggerFactory.getLogger(HousekeepingInjector.class);

	public enum BayChangeChoice {
		BayChangeNone,
		BayChangeBayChange,
		BayChangePathSegmentChange,
		BayChangeExceptSamePathDistance
	}

	public enum RepeatPosChoice {
		RepeatPosNone,
		RepeatPosContainerOnly,
		RepeatPosContainerAndCount
	}

	private HousekeepingInjector() {

	}

	public static RepeatPosChoice getRepeatPosChoice(Tenant tenant,Facility inFacility) {
		String repeatValue = PropertyService.getInstance().getPropertyFromConfig(tenant,inFacility, DomainObjectProperty.RPEATPOS);		
		// These should be in the canonical form. See DomainObjectProperty toCanonicalForm().
		if (repeatValue.equals("None"))
			return RepeatPosChoice.RepeatPosNone;
		else if (repeatValue.equals("ContainerOnly"))
			return RepeatPosChoice.RepeatPosContainerOnly;
		else if (repeatValue.equals("ContainerAndCount"))
			return RepeatPosChoice.RepeatPosContainerAndCount;
		else {
			LOGGER.error("unexpected value in getRepeatPosChoice");
			return RepeatPosChoice.RepeatPosNone;
		}
	}


	public static BayChangeChoice getBayChangeChoice(Tenant tenant,Facility inFacility) {
		String bayValue = PropertyService.getInstance().getPropertyFromConfig(tenant,inFacility, DomainObjectProperty.BAYCHANG);
		// These should be in the canonical form. See DomainObjectProperty toCanonicalForm().
		if (bayValue.equals("None"))
			return BayChangeChoice.BayChangeNone;
		else if (bayValue.equals("BayChange"))
			return BayChangeChoice.BayChangeBayChange;
		else if (bayValue.equals("PathSegmentChange"))
			return BayChangeChoice.BayChangePathSegmentChange;
		else if (bayValue.equals("BayChangeExceptAcrossAisle"))
			return BayChangeChoice.BayChangeExceptSamePathDistance;
		else {
			LOGGER.error("unexpected value in getBayChangeChoice");
			return BayChangeChoice.BayChangeNone;
		}
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

	// helper function
	@SuppressWarnings({})
	private static boolean isDifferentNotNullBay(Location inLoc1, Location inLoc2) {
		Location bay1 = inLoc1.getParentAtLevel(Bay.class);
		Location bay2 = inLoc2.getParentAtLevel(Bay.class);
		if (bay1 != null && bay2 != null && !bay1.equals(bay2))
			return true;
		return false;
	}

	// helper function
	private static boolean hasDifferentNotNullPathSegment(Location inLoc1, Location inLoc2) {
		PathSegment segment1 = inLoc1.getAssociatedPathSegment();
		PathSegment segment2 = inLoc2.getAssociatedPathSegment();
		if (segment1 != null && segment2 != null && !segment1.equals(segment2))
			return true;
		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Three choices of behavior
	 */
	@SuppressWarnings({})
	private static boolean wantBayChangeBetween(BayChangeChoice inBayChangeChoice,
		WorkInstruction inPrevWi,
		WorkInstruction inNextWi) {
		if (inBayChangeChoice == BayChangeChoice.BayChangeNone)
			return false;

		try {
			// This can be tricky. Crossbatch put WI may have multiple locations. Initial implementation will not be completely right if the multiple locations span across bays.
			// In our model, the WI.location field in this case is the arbitrary "first" location of all the locations for the outbound order.
			Location loc1 = inPrevWi.getLocation();
			Location loc2 = inNextWi.getLocation();
			if (loc1 == null || loc2 == null) {
				LOGGER.error("null WI location in wisNeedHouseKeepingBetween");
				return false;
			} else if (inBayChangeChoice == BayChangeChoice.BayChangeBayChange) {
				if (isDifferentNotNullBay(loc1, loc2))
					return true;
			} else if (inBayChangeChoice == BayChangeChoice.BayChangePathSegmentChange) {
				if (hasDifferentNotNullPathSegment(loc1, loc2))
					return true;
			} else if (inBayChangeChoice == BayChangeChoice.BayChangeExceptSamePathDistance) {
				if (isDifferentNotNullBay(loc1, loc2)) {
					// tentatively return true; Only not true if same path segment and bays on opposite side of same aisle
					if (!hasDifferentNotNullPathSegment(loc1, loc2)) {
						// As a surrogate, if each bay has the same domain ID, then assume the same distance along path.
						Location bay1 = loc1.getParentAtLevel(Bay.class);
						Location bay2 = loc2.getParentAtLevel(Bay.class);
						if (bay1 != null && bay2 != null && bay1.getDomainId().equals(bay2.getDomainId()))
							return false;
						return true;
					}
				}
			} else
				LOGGER.error("unimplemented case in wantBayChangeBetween");
			// Code above is complicated. Uncaught throw here (probably NPE) would result in aborted work instruction computation
		} catch (Exception e) {
			LOGGER.error("wantBayChangeBetween", e);
		}

		return false;
	}

	// --------------------------------------------------------------------------
	/**
	 * Three choices of behavior
	 */
	private static boolean wantRepeatContainerBetween(RepeatPosChoice inRepeatPosChoice,
		WorkInstruction inPrevWi,
		WorkInstruction inNextWi) {
		if (inRepeatPosChoice == RepeatPosChoice.RepeatPosNone)
			return false;

		try {
			if (inPrevWi.getContainer().equals(inNextWi.getContainer())) {
				if (inRepeatPosChoice == RepeatPosChoice.RepeatPosContainerOnly)
					return true;
				else if (inRepeatPosChoice == RepeatPosChoice.RepeatPosContainerAndCount) {
					if (inPrevWi.getPlanQuantity().equals(inNextWi.getPlanQuantity()))
						return true;
				} else
					LOGGER.error("unimplemented case in wantRepeatContainerBetween");
			}
			// Code above is complicated. Uncaught throw here (probably NPE) would result in aborted work instruction computation
		} catch (Exception e) {
			LOGGER.error("wantRepeatContainerBetween", e);
		}
		return false;
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
	private static List<WorkInstructionTypeEnum> wisNeedHouseKeepingBetween(WorkInstruction inPrevWi,
		WorkInstruction inNextWi,
		BayChangeChoice inBayChangeChoice,
		RepeatPosChoice inRepeatPosChoice) {
		List<WorkInstructionTypeEnum> returnList = null;

		if (inPrevWi == null)
			return null;
		else if (inNextWi == null) {
			LOGGER.error("null value in wisNeedHouseKeepingBetween");
			return null;
		} else {
			// If both repeatContainer and bayChange, this does bay change only. DEV-478
			if (wantBayChangeBetween(inBayChangeChoice, inPrevWi, inNextWi)) {
				returnList = addHouseKeepEnumToList(returnList, WorkInstructionTypeEnum.HK_BAYCOMPLETE);
			} else if (wantRepeatContainerBetween(inRepeatPosChoice, inPrevWi, inNextWi)) {
				returnList = addHouseKeepEnumToList(returnList, WorkInstructionTypeEnum.HK_REPEATPOS);
			}
		}
		return returnList;
	}

	// --------------------------------------------------------------------------
	/**
	 * A small, public special case for DEV-477 wrapped-route that may need to add a bay change.  RepeatPos might be possible, but not so important.
	 */
	/*
	public static List<WorkInstruction> addHouseKeepingIfNecessary(Facility inFacility,
		WorkInstruction inPrevWi,
		WorkInstruction inNextWi,
		List<WorkInstruction> inWiList) {

		List<WorkInstruction> returnList = inWiList;

		if (inPrevWi == null)
			return null;
		else if (inNextWi == null) {
			LOGGER.error("null value in wisNeedHouseKeepingBetween");
			return null;
		} else {
			// If both repeatContainer and bayChange, do bay change only. DEV-478
			if (wantBayChangeBetween(getBayChangeChoice(), inPrevWi, inNextWi)) {
				WorkInstruction houseKeepingWi = WiFactory.createHouseKeepingWi(WorkInstructionTypeEnum.HK_BAYCOMPLETE,
					inFacility,
					inPrevWi,
					inNextWi);
				if (houseKeepingWi != null)
					returnList.add(houseKeepingWi);

			} else if (wantRepeatContainerBetween(getRepeatPosChoice(), inPrevWi, inNextWi)) {
				WorkInstruction houseKeepingWi = WiFactory.createHouseKeepingWi(WorkInstructionTypeEnum.HK_REPEATPOS,
					inFacility,
					inPrevWi,
					inNextWi);
				if (houseKeepingWi != null)
					returnList.add(houseKeepingWi);
			}
		}
		return returnList;
	}
	*/

	// --------------------------------------------------------------------------
	/**
	 * This will add any necessary housekeeping WIs. And then add the sort and group codes and save each WI.
	 * @param inFacility
	 * @param inSortedWiList
	 * @return
	 */
	public static List<WorkInstruction> addHouseKeepingAndSaveSort(Tenant tenant,Facility inFacility, List<WorkInstruction> inSortedWiList) {
		List<WorkInstruction> wiResultList = new ArrayList<WorkInstruction>();
		WorkInstruction lastWi = null;
		for (WorkInstruction wi : inSortedWiList) {
			List<WorkInstructionTypeEnum> theHousekeepingTypeList = wisNeedHouseKeepingBetween(lastWi,
				wi,
				getBayChangeChoice(tenant,inFacility),
				getRepeatPosChoice(tenant,inFacility));
			// returns null if nothing to do. If non-null, then at lease one in the list.
			if (theHousekeepingTypeList != null) {
				for (WorkInstructionTypeEnum theType : theHousekeepingTypeList) {
					WorkInstruction houseKeepingWi = WiFactory.createHouseKeepingWi(tenant,theType, inFacility, lastWi, wi);
					if (houseKeepingWi != null) {
						wiResultList.add(houseKeepingWi);
						LOGGER.info("adding housekeeping WI type " + houseKeepingWi.getDescription());
					} else
						LOGGER.debug("null returned from getNewHousekeepingWiOfType");
				}
			}
			wiResultList.add(wi);
			lastWi = wi;
		}
		// At this point, some or none added. wiResultList is ordered. Time to add the sort codes.
		if (wiResultList.size() > 0)
			wiResultList = WorkInstructionSequencerABC.setSortCodesByCurrentSequence(tenant,wiResultList);
		return wiResultList;
	}

}
