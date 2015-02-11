/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class OutboundOrderCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(OutboundOrderCsvBean.class);

	protected String	orderGroupId;
	
	@NotNull
	@Size(min = 1)
	protected String	orderId;
	protected String	orderDetailId;
	@NotNull
	@Size(min = 1)
	protected String	itemId;
	@NotNull
	@Size(min = 1)
	protected String	description;
	@NotNull
	@Size(min = 1)
	protected String	quantity;
	protected String	minQuantity;
	protected String	maxQuantity;
	@NotNull
	@Size(min = 1)
	protected String	uom;
	protected String	orderDate;
	protected String	dueDate;
	protected String	destinationId;
	protected String	pickStrategy;
	protected String	preAssignedContainerId;
	protected String	shipperId;
	protected String	customerId;
	protected String	workSequence;
	// new fields for location-based pick DEV-571
	protected String	upc;
	protected String	operationType;
	protected String	locationId;
	protected String	cmFromLeft;

	public final String getOrderGroupId() {
		return strip(orderGroupId);
	}

	public final String getOrderId() {
		return strip(orderId);
	}

	public final String getOrderDetailsId() {
		return strip(orderDetailId);
	}

	public final String getItemId() {
		return strip(itemId);
	}

	public final String getDescription() {
		return strip(description);
	}

	public final String getQuantity() {
		return strip(quantity);
	}

	public final String getMinQuantity() {
		return strip(minQuantity);
	}

	public final String getMaxQuantity() {
		return strip(maxQuantity);
	}

	public final String getUom() {
		return strip(uom);
	}

	public final String getOrderDate() {
		return strip(orderDate);
	}

	public final String getDueDate() {
		return strip(dueDate);
	}

	public final String getDestinationId() {
		return strip(destinationId);
	}

	public final String getPickStrategy() {
		return strip(pickStrategy);
	}

	public final String getPreAssignedContainerId() {
		return strip(preAssignedContainerId);
	}

	public final String getShipperId() {
		return strip(shipperId);
	}

	public final String getCustomerId() {
		return strip(customerId);
	}

	public final String getWorkSequence() {
		return strip(workSequence);
	}
	// new fields for location-based pick DEV-571
	public final String getUpc() {
		return strip(upc);
	}
	public final String getOperationType() {
		return strip(operationType);
	}
	public final String getLocationId() {
		return strip(locationId);
	}
	public final String getCmFromLeft() {
		return strip(cmFromLeft);
	}

}
