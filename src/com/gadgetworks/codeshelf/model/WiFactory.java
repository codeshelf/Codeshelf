/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2014, Codeshelf, Inc., All rights reserved
 *  $Id: Facility.java,v 1.82 2013/11/05 06:14:55 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.CheDeviceLogic;
import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum.WorkInstructionTypeNum;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.util.SequenceNumber;
import com.gadgetworks.flyweight.command.ColorEnum;

/**
 * This generates work instructions
 * First the new housekeeping work instructions. Then normal and shorts also.
 * 
 */
public class WiFactory {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(WiFactory.class);
	

	/**
	 * The API to create housekeeping work instruction
	 */
	public static WorkInstruction createHouseKeepingWi(WorkInstructionTypeEnum inType,
		Facility inFacility,
		WorkInstruction inPrevWi,
		WorkInstruction inNextWi) {
		
		// Let's declare that inPrevWi must be there, but inNextWi might be null (if for example there is QA function after the last one done).
		if (inPrevWi == null){
			LOGGER.error("unexpected null CHE in createHouseKeepingWi");
			return null;
		}
		// These values may come from the wi parameters
		Che ourChe = inPrevWi.getAssignedChe();
		if (ourChe == null) {
			LOGGER.error("unexpected null CHE in createHouseKeepingWi");
			return null;
		}
		Timestamp assignTime = inPrevWi.getAssigned();
		Container ourCntr = inPrevWi.getContainer();
		
		WorkInstruction resultWi = new WorkInstruction();
		resultWi.setParent(inFacility);
		resultWi.setOrderDetail(null);
		resultWi.setCreated(new Timestamp(System.currentTimeMillis()));
		resultWi.setLedCmdStream("[]"); // empty array
		setWorkInstructionLedPatternForHK(resultWi, inType, inPrevWi);

		long seq = SequenceNumber.generate();
		String wiDomainId = Long.toString(seq);
		resultWi.setDomainId(wiDomainId);
		resultWi.setTypeEnum(inType);
		resultWi.setStatusEnum(WorkInstructionStatusEnum.NEW); // perhaps there could be a general housekeep status as there is for short, 
		// but short denotes completion as short, even if it was short from the start and there was never a chance to complete or short.

		resultWi.setLocation(inFacility);
		resultWi.setLocationId(inFacility.getFullDomainId());
		resultWi.setItemMaster(null);
		resultWi.setDescription(getDescriptionForHK(inType));
		resultWi.setPickInstruction(getPickInstructionForHK(inType)); // This is normally the location name
		resultWi.setPosAlongPath(inPrevWi.getPosAlongPath()); // Need to matche this to the surrounding work instructions. Or else when che scans start location, these may filter out or reorder.

		resultWi.setPlanQuantity(0);
		resultWi.setPlanMinQuantity(0);
		resultWi.setPlanMaxQuantity(0);
		resultWi.setActualQuantity(0);

		// The container provides our map to the position controller. That is the only way the user can acknowledge the housekeeping command.
		resultWi.setContainer(ourCntr);
		resultWi.setAssignedChe(ourChe);
		resultWi.setAssigned(assignTime);

		try {
			WorkInstruction.DAO.store(resultWi);
			ourChe.addWorkInstruction(resultWi); 
		} catch (DaoException e) {
			LOGGER.error("createHouseKeepingWi", e);
		}

		return resultWi;
	}

	/**
	 * The main housekeeping description
	 */
	private static String getDescriptionForHK(WorkInstructionTypeEnum inType) {
		String returnStr = "";
		switch (inType) {
	
			case HK_REPEATPOS:
				returnStr = "Repeat Container";
				break;

			case HK_BAYCOMPLETE:
				returnStr = "Bay Change";
				break;

			default:
				returnStr = "Unknown Housekeeping Plan";
				LOGGER.error("getDescriptionForHK unknown case");
				break;
		}
		return returnStr;
	}
	
	/**
	 * Normally this is the location. What should show for housekeeping?
	 */
	private static String getPickInstructionForHK(WorkInstructionTypeEnum inType) {
		String returnStr = "";

		return returnStr;
	}
	
	/**
	 * Set an aisle led pattern on the inTargetWi; or do nothing
	 */
	private static void setWorkInstructionLedPatternForHK(WorkInstruction inTargetWi, WorkInstructionTypeEnum inType, WorkInstruction inPrevWi) {
		// The empty pattern is already initialized, so it is ok to do nothing and return if no aisle lighting should be done
		if (inPrevWi == null || inTargetWi == null) {
			LOGGER.error("setWorkInstructionLedPattern");
			return;
		}

		List<LedCmdGroup> ledCmdGroupList = getLedCmdGroupListForHK(inType, inTargetWi.getLocation());
		if (ledCmdGroupList.size() > 0)
			inTargetWi.setLedCmdStream(LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroupList));
	}
	
	/**
	 * ok to return null if no aisle lights. Only some kinds of housekeeps involve aisle lights.
	 */
	private static List<LedCmdGroup> getLedCmdGroupListForHK(WorkInstructionTypeEnum inType, ILocation inLocation) {
		return Collections.<LedCmdGroup>emptyList(); // returns empty immutable list
	}

}
