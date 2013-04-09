/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: SlottedInventoryCsvImportBean.java,v 1.1 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jeffw
 *
 */
@Data
@ToString
public class SlottedInventoryCsvImportBean {
	private String	itemId;
	private String	description;
	private String	quantity;
	private String	uomId;
	private String	inventoryDate;
	private String	lotId;
	private String	locationId;
}
