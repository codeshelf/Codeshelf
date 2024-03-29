/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SlottedInventoryCsvBean.java,v 1.3 2013/04/14 16:47:38 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author jeffw
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class InventorySlottedCsvBean extends ImportCsvBeanABC {

	@NotNull
	@Size(min = 1)
	protected String	itemId;
	@NotNull
	@Size(min = 1)
	protected String	description;
	protected String	quantity;
	@NotNull
	@Size(min = 1)
	protected String	uom;
	protected String	inventoryDate;
	protected String	lotId;	
	@NotNull
	@Size(min = 1)
	protected String	locationId;
	protected String	cmFromLeft;
	protected String	gtin;

	public final String getGtin() {
		return strip(gtin);
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

	public final String getUom() {
		return strip(uom);
	}

	public final String getInventoryDate() {
		return strip(inventoryDate);
	}

	public final String getLotId() {
		return strip(lotId);
	}

	public final String getLocationId() {
		return strip(locationId);
	}
	
	public final String getCmFromLeft() {
		return strip(cmFromLeft);
	}

}
