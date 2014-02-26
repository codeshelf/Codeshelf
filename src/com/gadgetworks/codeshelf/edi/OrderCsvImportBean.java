/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvImportBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class OrderCsvImportBean extends CsvImportBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(OrderCsvImportBean.class);

	protected String	orderGroupId;
	@NotNull
	protected String	orderId;
	@NotNull
	protected String	orderDetailId;
	@NotNull
	protected String	itemId;
	@NotNull
	protected String	description;
	@NotNull
	protected String	quantity;
	@NotNull
	protected String	uom;
	protected String	orderDate;
	@NotNull
	protected String	dueDate;
	protected String	destinationId;
	protected String	pickStrategy;
	protected String	preAssignedContainerId;
	protected String	shipmentId;
	protected String	customerId;
	protected String	workSequence;

}
