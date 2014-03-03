/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SlottedInventoryCsvImportBean.java,v 1.3 2013/04/14 16:47:38 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@Data
@ToString
public class SlottedInventoryCsvImportBean extends CsvImportBeanABC {

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
}
