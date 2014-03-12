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
public class CrossBatchCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(CrossBatchCsvBean.class);

	protected String	orderGroupId;

	@NotNull
	protected String	itemId;

	@NotNull
	protected String	quantity;

	@NotNull
	protected String	containerId;

	@NotNull
	protected String	uom;

	public final String getOrderGroupId() {
		return StringUtils.strip(orderGroupId);
	}

	public final String getItemId() {
		return StringUtils.strip(itemId);
	}

	public final String getQuantity() {
		return StringUtils.strip(quantity);
	}

	public final String getContainerId() {
		return StringUtils.strip(containerId);
	}

	public final String getUom() {
		return StringUtils.strip(uom);
	}

}
