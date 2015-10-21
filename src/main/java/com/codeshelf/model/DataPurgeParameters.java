package com.codeshelf.model;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.service.ParameterSetBeanABC;

public class DataPurgeParameters extends ParameterSetBeanABC {
	// This follows our model of all groovy beans are strings

	// default values
	final static int	PURGEAFTERDAYS			= 20;
	final static int	WORKINSTRUCTION_BATCH	= 50; // applies to work instructions, WorkerEvent, event resolution, and work instruction export bean
	final static int	ORDER_BATCH				= 5;
	final static int	CONTAINER_BATCH			= 50;
	final static int	EDIHISTORY_BATCH		= 50; // applies to import receipts and export messages

	@Getter
	@Setter
	protected String	purgeAfterDays;
	@Getter
	@Setter
	protected String	workInstructionBatch;
	@Getter
	@Setter
	protected String	orderBatch;
	@Getter
	@Setter
	protected String	containerBatch;
	@Getter
	@Setter
	protected String	ediHistoryBatch;

	public DataPurgeParameters() {
		super();
		purgeAfterDays = Integer.toString(PURGEAFTERDAYS);
		workInstructionBatch = Integer.toString(WORKINSTRUCTION_BATCH);
		orderBatch = Integer.toString(ORDER_BATCH);
		containerBatch = Integer.toString(CONTAINER_BATCH);
		ediHistoryBatch = Integer.toString(EDIHISTORY_BATCH);
	}

	public int getPurgeAfterDaysValue() {
		return getCleanValue("purgeAfterDays", getPurgeAfterDays(), PURGEAFTERDAYS);
	}

	public int getWorkInstructionBatchValue() {
		return getCleanValue("workInstructionBatch", getWorkInstructionBatch(), WORKINSTRUCTION_BATCH);
	}

	public int getOrderBatchValue() {
		return getCleanValue("orderBatch", getOrderBatch(), ORDER_BATCH);
	}

	public int getContainerBatchValue() {
		return getCleanValue("containerBatch", getContainerBatch(), CONTAINER_BATCH);
	}

	public int getEdiHistoryBatchValue() {
		return getCleanValue("workerEventBatch", getEdiHistoryBatch(), EDIHISTORY_BATCH);
	}

	/**
	 * Notice the intent is to return the values that will actually be used.
	 */
	@Override
	public String getParametersDescription() {
		return String.format("purgeAfterDays: %d; , workInstructionBatch: %d; , orderBatch: %d; , containerBatch: %d; , workerEventBatch: %d;",
			getPurgeAfterDaysValue(),
			getWorkInstructionBatchValue(),
			getOrderBatchValue(),
			getContainerBatchValue(),
			getEdiHistoryBatchValue());
	}

	/**
	 * Giving the sanitized values, rather than the raw values. Could go either way.
	 */
	@Override
	public String toString() {
		return getParametersDescription();
	}

}