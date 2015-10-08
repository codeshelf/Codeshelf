/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;



import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.util.CompareNullChecker;

/**
 * Built first for PFSWeb, this accumulates complete work instruction beans for sending later as small files organized by order.
 * Currently only a memory list, lost upon server restart.
 * Later, change to a persistent list of the serialized bean to survive server restart.
 */
public class EdiExportQueue {
	private ArrayList<WorkInstructionCsvBean> wiBeanList = new ArrayList<WorkInstructionCsvBean>();
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(EdiExportQueue.class);
	
	public EdiExportQueue() {
	}
	
	/**
	 * A means to reset and get rid of leftover garbage.
	 */
	public void clearAll() {
		for (WorkInstructionCsvBean bean : wiBeanList) {
			removeWI(bean);
		}
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
		if (!inWi.isHousekeeping()){
			WorkInstructionCsvBean.staticGetDao().store(wiBean);
		}
		wiBeanList.add(wiBean);		
	}
	
	public void restoreWorkInstructionBeanFromDB(WorkInstructionCsvBean savedBean) {
		wiBeanList.add(savedBean);		
	}

	
	/**
	 * Comparator to order the beans by timeComplete, then itemId
	 * Note: it might be possible in some test runs that sometimes orders are done at same time and sometimes off by a second, therefore changing the sort.
	 * If so, we can change this.
	 */
	private class WiBeanComparator implements Comparator<WorkInstructionCsvBean> {
		// Sort the beans in a determinant manner. Ideally, by order of completion.

		@Override
		public int compare(WorkInstructionCsvBean bean1, WorkInstructionCsvBean bean2) {
			// The bean has the timestamp as a string. Should work well enough. However, sometimes the complete or short time is equivalent.
			// Not great. But remember, we want determinancy first. Doubt any host will care too much about the order.
			int value = CompareNullChecker.compareNulls(bean1.getCompleted(), bean2.getCompleted());
			if (value != 0)
				return value;
			
			value = bean1.getCompleted().compareTo(bean2.getCompleted());
			if (value != 0)
				return value;
			
			// secondary sort: item. Should be only one item for one order.
			String item1Name = bean1.getItemId();
			String item2Name = bean2.getItemId();
			value = CompareNullChecker.compareNulls(item1Name, item2Name);
			if (value != 0)
				return value;
			value = item1Name.compareTo(item2Name);
			if (value == 0){
				LOGGER.error("strange case in WiBeanComparator-- not unique by time and item");
			}
				return value;
		}
	}

	
	/**
	 * In the same order as the main list, return a sublist of beans for this orderId
	 * Sort it, so that our tests are not intermittent
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
			removeWI(bean);
		}
		Collections.sort(returnList, new WiBeanComparator());
		return returnList;
	}

	/**
	 * In the same order as the main list, return a sublist of beans for this orderId and Che
	 * Sort it, so that our tests are not intermittent
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
			removeWI(bean);
		}
		Collections.sort(returnList, new WiBeanComparator());
		return returnList;
	}
	
	private void removeWI(WorkInstructionCsvBean bean) {
		UUID savedPersistentId = bean.getPersistentId();
		if (savedPersistentId != null) {
			bean.setActive(false);
			bean.setUpdated(new Timestamp(System.currentTimeMillis()));
			WorkInstructionCsvBean.staticGetDao().store(bean);
		}
		wiBeanList.remove(bean);
	}
	
}
