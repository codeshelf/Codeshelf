/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class OrderCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(OrderCsvBean.class);

	protected String	orderGroupId;
	@NotNull
	protected String	orderId;
	@NotNull
	protected String	itemId;
	@NotNull
	protected String	description;
	@NotNull
	protected String	quantity;
	@NotNull
	protected String	uom;
	protected String	orderDate;
	protected String	dueDate;
	protected String	destinationId;
	protected String	pickStrategy;
	protected String	preAssignedContainerId;
	protected String	shipmentId;
	protected String	customerId;
	protected String	workSequence;

	public final String getOrderGroupId() {
		return StringUtils.strip(orderGroupId);
	}

	public final String getOrderId() {
		return StringUtils.strip(orderId);
	}

	public final String getItemId() {
		return StringUtils.strip(itemId);
	}

	public final String getDescription() {
		return StringUtils.strip(description);
	}

	public final String getQuantity() {
		return StringUtils.strip(quantity);
	}

	public final String getUom() {
		return StringUtils.strip(uom);
	}

	public final String getOrderDate() {
		return StringUtils.strip(orderDate);
	}

	public final String getDueDate() {
		return StringUtils.strip(dueDate);
	}

	public final String getDestinationId() {
		return StringUtils.strip(destinationId);
	}

	public final String getPickStrategy() {
		return StringUtils.strip(pickStrategy);
	}

	public final String getPreAssignedContainerId() {
		return StringUtils.strip(preAssignedContainerId);
	}

	public final String getShipmentId() {
		return StringUtils.strip(shipmentId);
	}

	public final String getCustomerId() {
		return StringUtils.strip(customerId);
	}

	public final String getWorkSequence() {
		return StringUtils.strip(workSequence);
	}

}
