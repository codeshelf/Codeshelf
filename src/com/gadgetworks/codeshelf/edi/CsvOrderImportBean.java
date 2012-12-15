/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvOrderImportBean.java,v 1.4 2012/12/15 02:25:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jeffw
 *
 */
@ToString
public class CsvOrderImportBean {
	@Getter
	@Setter
	private String	orderId;

	@Getter
	@Setter
	private String	orderDetailId;

	@Getter
	@Setter
	private String	itemId;

	@Getter
	@Setter
	private String	description;

	@Getter
	@Setter
	private String	quantity;

	@Getter
	@Setter
	private String	uomId;

	@Getter
	@Setter
	private String	orderDate;
	
	@Getter
	@Setter
	private String	dueDate;
	
	@Getter
	@Setter
	private String	accountId;
	
	@Getter
	@Setter
	private String	orderGroupId;

	@Getter
	@Setter
	private String	destinationId;
	
	@Getter
	@Setter
	private String	pickStrategy;
	
	@Getter
	@Setter
	private String	preAssignedContainerId;

	@Getter
	@Setter
	private String	shipmentId;
	
	@Getter
	@Setter
	private String	customerId;
}
