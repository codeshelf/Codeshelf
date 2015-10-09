package com.codeshelf.metrics;

import lombok.Getter;
import lombok.Setter;

import com.codeshelf.service.ParameterSetBeanABC;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(getterVisibility=Visibility.PUBLIC_ONLY, fieldVisibility=Visibility.NONE)
public class DataQuantityHealthCheckParameters extends ParameterSetBeanABC {
	// This follows our model of all groovy beans are strings

	// default values
	final static int			MAX_ORDERDETAIL		= 200000;
	final static int			MAX_WORKINSTRUCTION	= 200000;
	final static int			MAX_ORDER			= 200000;
	final static int			MAX_CONTAINERUSE	= 200000;

	@Getter
	@Setter
	@JsonIgnore
	protected String			maxOrderDetail;
	@Getter
	@Setter
	@JsonIgnore
	protected String			maxWorkInstruction;
	@Getter
	@Setter
	@JsonIgnore
	protected String			maxOrder;
	@Getter
	@Setter
	@JsonIgnore
	protected String			maxContainerUse;

	public DataQuantityHealthCheckParameters() {
		super();
		maxOrderDetail = Integer.toString(MAX_ORDERDETAIL);
		maxWorkInstruction = Integer.toString(MAX_WORKINSTRUCTION);
		maxOrder = Integer.toString(MAX_ORDER);
		maxContainerUse = Integer.toString(MAX_CONTAINERUSE);
	}

	public int getMaxOrderDetailValue() {
		return getCleanValue("maxOrderDetail", getMaxOrderDetail(), MAX_ORDERDETAIL);
	}

	public int getMaxWorkInstructionValue() {
		return getCleanValue("maxWorkInstruction", getMaxWorkInstruction(), MAX_WORKINSTRUCTION);
	}

	public int getMaxOrderValue() {
		return getCleanValue("maxWorkOrder", getMaxOrder(), MAX_ORDER);
	}

	public int getMaxContainerUseValue() {
		return getCleanValue("maxContainerUse", getMaxContainerUse(), MAX_CONTAINERUSE);
	}

	/**
	 * Notice the intent is to return the values that will actually be used.
	 */
	@Override
	public String getParametersDescription() {
		return String.format("maxOrder: %d; , maxOrderDetail: %d; , maxWorkInstruction: %d; , maxContainerUse: %d;",
			getMaxOrderValue(),
			getMaxOrderDetailValue(),
			getMaxWorkInstructionValue(),
			getMaxContainerUseValue());
	}

}