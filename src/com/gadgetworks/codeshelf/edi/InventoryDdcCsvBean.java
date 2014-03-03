/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DdcInventoryCsvBean.java,v 1.5 2013/07/17 05:48:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import org.apache.commons.lang.StringUtils;

import lombok.Data;
import lombok.ToString;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class InventoryDdcCsvBean extends ImportCsvBeanABC {

	@NotNull
	protected String	itemId;
	protected String	description;
	@NotNull
	protected String	ddcId;
	@NotNull
	protected String	quantity;
	@NotNull
	protected String	uom;

	public final String getItemId() {
		return StringUtils.strip(itemId);
	}

	public final String getDescription() {
		return StringUtils.strip(description);
	}

	public final String getDdcId() {
		return StringUtils.strip(ddcId);
	}

	public final String getQuantity() {
		return StringUtils.strip(quantity);
	}

	public final String getUom() {
		return StringUtils.strip(uom);
	}

}
