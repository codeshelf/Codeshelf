/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DdcInventoryCsvImportBean.java,v 1.4 2013/05/04 03:00:06 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class DdcInventoryCsvImportBean {
	private String	itemId;
	private String	itemDetailId;
	private String	description;
	private String	ddcId;
	private String	quantity;
	private String	uom;
}
