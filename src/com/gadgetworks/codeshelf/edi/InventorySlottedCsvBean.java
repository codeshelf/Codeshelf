/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SlottedInventoryCsvBean.java,v 1.3 2013/04/14 16:47:38 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@Data
@ToString
public class InventorySlottedCsvBean extends ImportCsvBeanABC {

	@NotNull
	protected String	itemId;
	@NotNull
	protected String	description;
	@NotNull
	protected String	quantity;
	@NotNull
	protected String	uom;
	@NotNull
	protected String	inventoryDate;
	protected String	lotId;
	@NotNull
	protected String	locationId;

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

	public final String getInventoryDate() {
		return StringUtils.strip(inventoryDate);
	}

	public final String getLotId() {
		return StringUtils.strip(lotId);
	}

	public final String getLocationId() {
		return StringUtils.strip(locationId);
	}
}
