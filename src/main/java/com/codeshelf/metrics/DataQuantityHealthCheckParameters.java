package com.codeshelf.metrics;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.service.ParameterSetBeanABC;

public class DataQuantityHealthCheckParameters extends ParameterSetBeanABC {
	// This follows our model of all groovy beans are strings

	// default values
	final static int			MAX_ORDERDETAIL		= 90000;
	final static int			MAX_WORKINSTRUCTION	= 50000;
	final static int			MAX_ORDER			= 40000;
	final static int			MAX_CONTAINERUSE	= 40000;

	@Getter
	@Setter
	protected String			maxOrderDetail;
	@Getter
	@Setter
	protected String			maxWorkInstruction;
	@Getter
	@Setter
	protected String			maxOrder;
	@Getter
	@Setter
	protected String			maxContainerUse;

	private static final Logger	LOGGER				= LoggerFactory.getLogger(DataQuantityHealthCheckParameters.class);

	public DataQuantityHealthCheckParameters() {
		super();
		maxOrderDetail = Integer.toString(MAX_ORDERDETAIL);
		maxWorkInstruction = Integer.toString(MAX_WORKINSTRUCTION);
		maxOrder = Integer.toString(MAX_ORDER);
		maxContainerUse = Integer.toString(MAX_CONTAINERUSE);
	}

	public int getMaxOrderDetailValue() {
		try {
			return Integer.valueOf(this.getMaxOrderDetail());
		} catch (NumberFormatException e) {
			LOGGER.warn("bad value for maxOrderDetail: {}", getMaxOrderDetail());
			return MAX_ORDERDETAIL;
		}
	}

	public int getMaxWorkInstructionValue() {
		try {
			return Integer.valueOf(this.getMaxWorkInstruction());
		} catch (NumberFormatException e) {
			LOGGER.warn("bad value for maxWorkInstructionl: {}", getMaxWorkInstruction());
			return MAX_WORKINSTRUCTION;
		}
	}

	public int getMaxOrderValue() {
		try {
			return Integer.valueOf(this.getMaxOrder());
		} catch (NumberFormatException e) {
			LOGGER.warn("bad value for maxWorkOrder: {}", getMaxOrder());
			return MAX_ORDER;
		}
	}

	public int getMaxContainerUseValue() {
		try {
			return Integer.valueOf(this.getMaxContainerUse());
		} catch (NumberFormatException e) {
			LOGGER.warn("bad value for maxContainerUse: {}", getMaxContainerUse());
			return MAX_CONTAINERUSE;
		}
	}

	/**
	 * Notice the intent is to return the values that will actually be used.
	 */
	@Override
	public String getParametersDescription() {
		return String.format("maxOrder: %d , maxOrderDetail: %d , maxWorkInstruction: %d , maxContainerUse: %d",
			getMaxOrder(),
			getMaxOrderDetailValue(),
			getMaxWorkInstructionValue(),
			getMaxContainerUseValue());
	}

}