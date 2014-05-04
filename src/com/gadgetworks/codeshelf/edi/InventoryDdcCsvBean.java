/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DdcInventoryCsvBean.java,v 1.5 2013/07/17 05:48:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;

/**
 * @author jeffw
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class InventoryDdcCsvBean extends ImportCsvBeanABC {

	@NotNull
	@Size(min = 1)
	protected String	itemId;
	protected String	description;
	@NotNull
	@Size(min = 1)
	protected String	ddcId;	
	@NotNull
	@Size(min = 1)
	protected String	quantity;	
	@NotNull
	@Size(min = 1)
	protected String	uom;

	public final String getItemId() {
		return strip(itemId);
	}

	public final String getDescription() {
		return strip(description);
	}

	public final String getDdcId() {
		return strip(ddcId);
	}

	public final String getQuantity() {
		return strip(quantity);
	}

	public final String getUom() {
		return strip(uom);
	}

}
