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
public class CrossBatchCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(CrossBatchCsvBean.class);

	protected String	orderGroupId;

	@NotNull
	@Size(min = 1)
	protected String	itemId;

	@NotNull
	@Size(min = 1)
	protected String	quantity;

	@NotNull
	@Size(min = 1)
	protected String	containerId;

	@NotNull
	@Size(min = 1)
	protected String	uom;

	public final String getOrderGroupId() {
		return strip(orderGroupId);
	}

	public final String getItemId() {
		return strip(itemId);
	}

	public final String getQuantity() {
		return strip(quantity);
	}

	public final String getContainerId() {
		return strip(containerId);
	}

	public final String getUom() {
		return strip(uom);
	}

}
