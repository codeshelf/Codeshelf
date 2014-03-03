/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvImportBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class PutBatchCsvImportBean extends CsvImportBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(PutBatchCsvImportBean.class);

	@NotNull
	protected String	itemId;

	protected String	orderBatchId;

	@NotNull
	protected String	quantity;

	@NotNull
	protected String	containerId;

	@NotNull
	protected String	description;

	@NotNull
	protected String	uom;

	public final String getItemId() {
		return StringUtils.strip(itemId);
	}

	public final String getOrderBatchId() {
		return StringUtils.strip(orderBatchId);
	}

	public final String getQuantity() {
		return StringUtils.strip(quantity);
	}

	public final String getContainerid() {
		return StringUtils.strip(containerId);
	}

	public final String getDescription() {
		return StringUtils.strip(description);
	}

	public final String getUom() {
		return StringUtils.strip(uom);
	}

}
