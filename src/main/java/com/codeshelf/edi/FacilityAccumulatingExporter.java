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

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;

/**
 * Built first for PFSWeb, this accumulates complete work instruction beans for sending later as small files organized by order.
 * Currently only a memory list, lost upon server restart.
 * Later, change to a persistent list of the serialized bean to survive server restart.
 */
public class FacilityAccumulatingExporter  implements FacilityEdiExporter {
	@Getter
	private EdiExportAccumulator accumulator; 

	private WiBeanStringifier	stringifier;

	private EdiExportTransport	exportService;
	
	private static final Logger	LOGGER	= LoggerFactory.getLogger(FacilityAccumulatingExporter.class);
	
	public FacilityAccumulatingExporter(EdiExportAccumulator accumulator, WiBeanStringifier stringifier, EdiExportTransport exportService) {
		this.accumulator = accumulator;
		this.stringifier = stringifier; 
		this.exportService = exportService;
	}
	
	public void notifyWiComplete(OrderHeader inOrder, Che inChe, WorkInstruction inWi) {
		accumulator.addWorkInstruction(inWi);
			//TODO disabling blow by blow
			//String exportStr = stringifier.stringifyWorkInstruction(inWi);
			//exportService.notifyWiComplete(inOrder, inChe, exportStr);
	}

	
	public void notifyOrderOnCart(OrderHeader inOrder, Che inChe) {
		String exportStr = stringifier.stringifyOrderOnCart(inOrder, inChe);
		LOGGER.info(exportStr);
		exportService.transportOrderOnCartAdded(inOrder, inChe, exportStr);
	}

	/**
	 * If this host needs it this way, send off the order with accumulated work instructions
	 * This is initially tailored to PFSWeb data interchange, mimicking Dematic cart
	 */
	public ExportReceipt notifyOrderCompleteOnCart(OrderHeader inOrder, Che inChe) {
		ArrayList<WorkInstructionCsvBean> orderCheList = accumulator.getAndRemoveWiBeansFor(inOrder.getOrderId(), inChe.getDomainId());
		// This list has "complete" work instruction beans. The particular customer's EDI may need strange handling.
		String exportStr = stringifier.stringifyOrderCompleteOnCart(inOrder, inChe, orderCheList);
		LOGGER.info(exportStr);
		return exportService.transportOrderOnCartFinished(inOrder, inChe, exportStr);
	}

	public void notifyOrderRemoveFromCart(OrderHeader inOrder, Che inChe) {
		// TODO Auto-generated method stub
		
	}




}
