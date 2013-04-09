/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvImportBean.java,v 1.1 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class OrderCsvImportBean {
	private String	orderId;
	private String	orderDetailId;
	private String	itemId;
	private String	description;
	private String	quantity;
	private String	uomId;
	private String	orderDate;
	private String	dueDate;
	private String	orderGroupId;
	private String	destinationId;
	private String	pickStrategy;
	private String	preAssignedContainerId;
	private String	shipmentId;
	private String	customerId;
	private String	workSequence;
}
