/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DdcInventoryCsvImportBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
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
	private String	ddcId;
	private String	quantity;
	private String	uom;
}
