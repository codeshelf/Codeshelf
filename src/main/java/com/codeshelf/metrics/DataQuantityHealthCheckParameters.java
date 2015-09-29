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
	protected String	maxOrderDetail;
	@Getter
	@Setter
	protected String	maxWorkInstruction;
	@Getter
	@Setter
	protected String	maxOrder;
	@Getter
	@Setter
	protected String	maxContainerUse;

	
	@SuppressWarnings("unused")
	private static final Logger	LOGGER	= LoggerFactory.getLogger(DataQuantityHealthCheckParameters.class);
	

	public DataQuantityHealthCheckParameters() {
		super();
		maxOrderDetail = Integer.toString(MAX_ORDERDETAIL);
		maxWorkInstruction = Integer.toString(MAX_WORKINSTRUCTION);
		maxOrder = Integer.toString(MAX_ORDER);
		maxContainerUse = Integer.toString(MAX_CONTAINERUSE);		
	}
	
	public int getMaxOrderDetailValue(){
		return Integer.valueOf(this.getMaxOrderDetail());
	}

	public int getMaxWorkInstructionValue(){
		return Integer.valueOf(this.getMaxWorkInstruction());
	}
	public int getMaxOrderValue(){
		return Integer.valueOf(this.getMaxOrder());
	}
	public int getMaxContainerUseValue(){
		return Integer.valueOf(this.getMaxContainerUse());
	}
	
	@Override
	public String getParametersDescription() {
		return String.format("maxOrder: %s , maxOrderDetail: %s , maxWorkInstruction: %s , maxContainerUse: %s", maxOrder, maxOrderDetail, maxWorkInstruction, maxContainerUse);
	}


}