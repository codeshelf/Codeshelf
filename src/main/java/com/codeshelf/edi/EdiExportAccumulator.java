/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;



import java.util.ArrayList;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.WorkInstruction;

/**
 * Built first for PFSWeb, this accumulates complete work instruction beans for sending later as small files organized by order.
 * Currently only a memory list, lost upon server restart.
 * Later, change to a persistent list of the serialized bean to survive server restart.
 */
public class EdiExportAccumulator {
	@Getter
	ArrayList<WorkInstructionCsvBean> wiBeanList = new ArrayList<WorkInstructionCsvBean>();
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiExportAccumulator.class);
	
	public EdiExportAccumulator() {
	}
	
	/**
	 * A means to reset and get rid of leftover garbage.
	 */
	public void clearAll() {
		wiBeanList.clear();
	}
	
	/**
	 * A simple diagnostic to help identify leftover garbage.
	 */
	public void reportSize(){
		LOGGER.info("{} work instructions in list", wiBeanList.size());
		// Would be nice to name "in {} orders, on {} che"
	}

	
	public void addWorkInstruction(WorkInstruction inWi) {
		WorkInstructionCsvBean wiBean = new WorkInstructionCsvBean(inWi);
		wiBeanList.add(wiBean);
	}
	
	/**
	 * In the same order as the main list, return a sublist of beans for this orderId
	 */
	public ArrayList<WorkInstructionCsvBean> getAndRemoveWiBeansFor(String inOrderId){
		ArrayList<WorkInstructionCsvBean> returnList = new ArrayList<WorkInstructionCsvBean>();
		if (inOrderId == null || inOrderId.isEmpty()){
			LOGGER.error("Bad call to getAndRemoveWiBeansFor orderId");
			return returnList;			
		}
		for (WorkInstructionCsvBean bean: wiBeanList){
			if (inOrderId.equals(bean.getOrderId())) {
				returnList.add(bean);
			}
		}
		for (WorkInstructionCsvBean bean: returnList){
			wiBeanList.remove(bean);
		}
		return returnList;
	}

	/**
	 * In the same order as the main list, return a sublist of beans for this orderId and Che
	 */
	public ArrayList<WorkInstructionCsvBean> getAndRemoveWiBeansFor(String inOrderId, String inCheId){
		ArrayList<WorkInstructionCsvBean> returnList = new ArrayList<WorkInstructionCsvBean>();
		if (inOrderId == null || inOrderId.isEmpty() || inCheId == null || inCheId.isEmpty()){
			LOGGER.error("Bad call to getAndRemoveWiBeansFor orderId and cheId");
			return returnList;			
		}
		for (WorkInstructionCsvBean bean: wiBeanList){
			if (inOrderId.equals(bean.getOrderId()) && inCheId.equals(bean.getCheId())) {
				returnList.add(bean);
			}
		}
		for (WorkInstructionCsvBean bean: returnList){
			wiBeanList.remove(bean);
		}
		return returnList;
	}
	
}
