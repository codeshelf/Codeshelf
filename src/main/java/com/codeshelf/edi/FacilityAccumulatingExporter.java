/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;



import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;

/**
 * Built first for PFSWeb, this accumulates complete work instruction beans for sending later as small files organized by order.
 * Currently only a memory list, lost upon server restart.
 * Later, change to a persistent list of the serialized bean to survive server restart.
 */
public class FacilityAccumulatingExporter  implements FacilityEdiExporter {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(FacilityAccumulatingExporter.class);

	@Getter
	private EdiExportAccumulator accumulator; 

	private WiBeanStringifier	stringifier;

	private EdiExportTransport	exportService;

	private Retryer<ExportReceipt> retryer; 
	
	public FacilityAccumulatingExporter(EdiExportAccumulator accumulator, WiBeanStringifier stringifier, EdiExportTransport exportService) {
		this.accumulator = accumulator;
		this.stringifier = stringifier; 
		this.exportService = exportService;
		this.retryer = RetryerBuilder.<ExportReceipt>newBuilder()
		        .retryIfExceptionOfType(IOException.class)
		        .withWaitStrategy(WaitStrategies.fibonacciWait(100, 2, TimeUnit.MINUTES))
		        .withStopStrategy(StopStrategies.stopAfterAttempt(20))
		        .build();

	}
	
	public void exportWiFinished(OrderHeader inOrder, Che inChe, WorkInstruction inWi) {
		accumulator.addWorkInstruction(inWi);
			//TODO disabling blow by blow
			//String exportStr = stringifier.stringifyWorkInstruction(inWi);
			//exportService.notifyWiComplete(inOrder, inChe, exportStr);
	}

	
	public void exportOrderOnCartAdded(final OrderHeader inOrder, final Che inChe) {
		final String exportStr = stringifier.stringifyOrderOnCartAdded(inOrder, inChe);
		try {
			retryer.call(new Callable<ExportReceipt>() {
					@Override
					public ExportReceipt call() throws IOException {
						try {
							exportService.transportOrderOnCartAdded(inOrder, inChe, exportStr);
							LOGGER.info("Sent orderOnCartAdded {}", exportStr);
							return null;
						} catch(RuntimeException e) {
							LOGGER.error("Unable to send orderOnCartAdded message {}", exportStr, e);
							return null;
						}
					}
			});
		} catch (ExecutionException | RetryException e) {
			LOGGER.error("Exception while exportingOrderOnCartAdded {} {} with transport {}", inOrder, inChe, exportService, e);
		}
	}

	public void exportOrderOnCartRemoved(OrderHeader inOrder, Che inChe) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * If this host needs it this way, send off the order with accumulated work instructions
	 * This is initially tailored to PFSWeb data interchange, mimicking Dematic cart
	 */
	public ExportReceipt exportOrderOnCartFinished(final OrderHeader inOrder, final Che inChe) {
		ArrayList<WorkInstructionCsvBean> orderCheList = accumulator.getAndRemoveWiBeansFor(inOrder.getOrderId(), inChe.getDomainId());
		// This list has "complete" work instruction beans. The particular customer's EDI may need strange handling.
		final String exportStr = stringifier.stringifyOrderOnCartFinished(inOrder, inChe, orderCheList);
		try {
			return retryer.call(new Callable<ExportReceipt>() {
					@Override
					public ExportReceipt call() throws IOException {
						try {
							ExportReceipt receipt=  exportService.transportOrderOnCartFinished(inOrder, inChe, exportStr);
							LOGGER.info("Sent orderOnCartFinished {}", exportStr);
							return receipt;
						} catch(RuntimeException e) {
							LOGGER.error("Unable to send orderOnCartFinished message {}", exportStr, e);
							return null;
						}
					}
			});
		} catch (ExecutionException | RetryException e) {
			LOGGER.error("Exception while exportingOrderOnCartFinished {} {} with transport {}", inOrder, inChe, exportService, e);
			return null;
		}

	}





}
