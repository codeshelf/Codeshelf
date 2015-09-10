/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2015, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *  
 *******************************************************************************/
package com.codeshelf.edi;



import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.ExportMessageFuture.OrderOnCartAddedExportMessage;
import com.codeshelf.edi.ExportMessageFuture.OrderOnCartFinishedExportMessage;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExportMessage;
import com.codeshelf.model.domain.ExportReceipt;
import com.codeshelf.model.domain.ExportReceipt.FailExportReceipt;
import com.codeshelf.model.domain.ExportReceipt.UnhandledExportReceipt;
import com.codeshelf.persistence.SideTransaction;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.FileExportReceipt;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.service.AbstractCodeshelfExecutionThreadService;
import com.codeshelf.util.EvictingBlockingQueue;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategy;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Built first for PFSWeb, this accumulates complete work instruction beans for sending later as small files organized by order.
 * Currently only a memory list, lost upon server restart.
 * Later, change to a persistent list of the serialized bean to survive server restart.
 */
public class FacilityAccumulatingExporter  extends AbstractCodeshelfExecutionThreadService implements FacilityEdiExporter {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(FacilityAccumulatingExporter.class);

	private static final ExportMessageFuture	POISON	= new ExportMessageFuture(null, null,null);

	@Getter
	private EdiExportAccumulator accumulator; 

	@Getter
	@Setter
	private WiBeanStringifier	stringifier;

	@Getter
	@Setter
	private EdiExportTransport	ediExportTransport;

	private Retryer<ExportReceipt> retryer;

	//private BlockingQueue<WorkEvent>	workQueue; 
	private EvictingBlockingQueue<ExportMessageFuture>	messageQueue; 
	
//	private final long RELATIVE_EPOCH = new DateTime(2015, 9, 01, 0, 0).getMillis();

	private Cache<ExportMessageFuture, ExportReceipt>	receiptCache;
	
	public FacilityAccumulatingExporter(EdiExportAccumulator accumulator, WiBeanStringifier stringifier, EdiExportTransport exportService) {
		super();
		this.accumulator = accumulator;
		this.stringifier = stringifier; 
		this.ediExportTransport = exportService;
		this.retryer = RetryerBuilder.<ExportReceipt>newBuilder()
		        .retryIfExceptionOfType(IOException.class)
		        .withWaitStrategy(WaitStrategies.fibonacciWait(100, 2, TimeUnit.MINUTES))
		        .withStopStrategy(new StopStrategy() {

					@SuppressWarnings("rawtypes")
					@Override
					public boolean shouldStop(Attempt failedAttempt) {
						return (!isRunning());
					}})
				.withRetryListener(new RetryListener() {

					@SuppressWarnings("hiding")
					@Override
					public <ExportReceipt> void onRetry(Attempt<ExportReceipt> attempt) {
						if (attempt.hasException()) {
							String attemptToString = String.format("Attempt num: %d, time since first attempt (ms): %d", attempt.getAttemptNumber(), attempt.getDelaySinceFirstAttempt());
							LOGGER.warn("Retrying edi export {} {}", FacilityAccumulatingExporter.this, attemptToString, attempt.getExceptionCause());
						}//else was successful
					}
					
				})
		        .build();
		this.messageQueue = new EvictingBlockingQueue<ExportMessageFuture>(250);
		this.receiptCache = CacheBuilder.newBuilder()
				.maximumSize(50)
				/*
				.weigher(new Weigher<ExportMessage, ExportReceipt>() {

					@Override
					public int weigh(ExportMessage key, ExportReceipt value) {
						// the newer it is the lighter it is (larger "epoch" will be more negative)
						return -1 * Ints.saturatedCast(RELATIVE_EPOCH - key.getDateTime().getMillis());
					}
					
				})*/
				.build();
	}
	
	@Override
	protected void startUp() throws Exception {
		
	}

	@Override
	protected void triggerShutdown() {
		this.messageQueue.drainTo(new ArrayList<ExportMessageFuture>());
		this.messageQueue.offer(POISON);
	}


	@Override
	protected void run() throws InterruptedException {
		TenantPersistenceService persistenceService = TenantPersistenceService.getInstance();
		while(isRunning() && !Thread.currentThread().isInterrupted()) {
			persistenceService.beginTransaction();
			ExportMessageFuture message = null;
			try {
				message = this.messageQueue.poll(30, TimeUnit.SECONDS);
				if (message == null) {
					continue; //test guards again
				}
				if (POISON.equals(message)) {
					return;
				}
				persistMessageCompletion(message);
				ExportReceipt receipt = processMessage(message);
				storeReceipt(message, receipt);
				message.setReceipt(receipt);
			} catch(RuntimeException e) {
				LOGGER.warn("Unable to process message {}", message, e);
			} finally {
				persistenceService.commitTransaction();
			}
		}
	}

	private ExportReceipt processMessage(final ExportMessageFuture message) {
		ExportReceipt receipt;
		try {
			receipt = retryer.call(new Callable<ExportReceipt>() {
				@Override
				public ExportReceipt call() throws IOException {
					if (message instanceof OrderOnCartAddedExportMessage) {
						FileExportReceipt receipt =  getEdiExportTransport().transportOrderOnCartAdded(message.getOrder(), message.getChe(), message.getContents());
						LOGGER.info("Sent orderOnCartAdded {}", message);
						return receipt;
					} else if (message instanceof OrderOnCartFinishedExportMessage){
						FileExportReceipt receipt=  getEdiExportTransport().transportOrderOnCartFinished(message.getOrder(), message.getChe(), message.getContents());
						LOGGER.info("Sent orderOnCartFinished {}", message);
						return receipt;
					} else {
						return new UnhandledExportReceipt();
					}
				}
			});
		} catch (ExecutionException e) {
			LOGGER.error("Aborting sender retry for message {}", message, e);
			receipt = new FailExportReceipt(e.getCause());
		}
		catch (RetryException e) {
			LOGGER.error("Aborting sender retry for message {}", message, e);
			receipt = new FailExportReceipt(e);
		}
		
		return receipt;

	}

	private void storeReceipt(ExportMessageFuture message, ExportReceipt receipt) {
		receiptCache.put(message, receipt);
		
	}

	public void exportWiFinished(OrderHeader inOrder, Che inChe, WorkInstruction inWi) {
		accumulator.addWorkInstruction(inWi);
			//TODO disabling blow by blow
			//String exportStr = stringifier.stringifyWorkInstruction(inWi);
			//exportService.notifyWiComplete(inOrder, inChe, exportStr);
	}
	
	public ListenableFuture<ExportReceipt> exportOrderOnCartAdded(final OrderHeader inOrder, final Che inChe) {
		final String exportStr = stringifier.stringifyOrderOnCartAdded(inOrder, inChe);
		ExportMessageFuture exportMessage = new OrderOnCartAddedExportMessage(inOrder, inChe, exportStr);
		persistMessage(inOrder.getFacility(), exportMessage);
		enqueue(exportMessage);
		return exportMessage;
	}

	public void exportOrderOnCartRemoved(OrderHeader inOrder, Che inChe) {
//		messageQueue.offer(new ExportMessage(inOrder, inChe, exportStr));
		// TODO Auto-generated method stub
		
	}

	/**
	 * If this host needs it this way, send off the order with accumulated work instructions
	 * This is initially tailored to PFSWeb data interchange, mimicking Dematic cart
	 */
	public ListenableFuture<ExportReceipt> exportOrderOnCartFinished(final OrderHeader inOrder, final Che inChe) {
		ArrayList<WorkInstructionCsvBean> orderCheList = accumulator.getAndRemoveWiBeansFor(inOrder.getOrderId(), inChe.getDomainId());
		// This list has "complete" work instruction beans. The particular customer's EDI may need strange handling.
		final String exportStr = stringifier.stringifyOrderOnCartFinished(inOrder, inChe, orderCheList);
		ExportMessageFuture exportMessage = new OrderOnCartFinishedExportMessage(inOrder, inChe, exportStr);
		persistMessage(inOrder.getFacility(), exportMessage);
		enqueue(exportMessage);
		return exportMessage;
	}
	
	private boolean enqueue(ExportMessageFuture message) {
		LOGGER.info("Enqueued message {} with contents {}", message, message.getContents());
		return messageQueue.offer(message);
	}
	
	private void persistMessage(Facility facility, ExportMessageFuture message){
		final ExportMessage exportMessage = new ExportMessage(facility, message);
		try {
			new SideTransaction<Void>() {
				@Override
				public Void task(Session session) {
					session.save(exportMessage);
					return null;
				}
			}.run();
			message.setPersistentId(exportMessage.getPersistentId());
		} catch (Exception e) {
			LOGGER.warn("Encountered error saving ExportMessage to DB: {}", e);
		}
	}
	
	private void persistMessageCompletion(ExportMessageFuture message){
		UUID persistentId = message.getPersistentId();
		if (persistentId != null) {
			ExportMessage exportMessage = ExportMessage.staticGetDao().findByPersistentId(message.getPersistentId());
			if (exportMessage != null) {
				exportMessage.setActive(false);
				ExportMessage.staticGetDao().store(exportMessage);
			}
		}
	}
}
