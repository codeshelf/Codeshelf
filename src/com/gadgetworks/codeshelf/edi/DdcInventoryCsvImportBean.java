/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DdcInventoryCsvImportBean.java,v 1.3 2013/04/14 02:39:39 jeffw Exp $
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
	private String	ddcId;
	private String	quantity;
	private String	uom;
}
