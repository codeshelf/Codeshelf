/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DdcInventoryCsvImportBean.java,v 1.5 2013/07/17 05:48:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class DdcInventoryCsvImportBean extends CsvImportBeanABC {

	@NotNull
	protected String	itemId;
	protected String	description;
	@NotNull
	protected String	ddcId;
	@NotNull
	protected String	quantity;
	@NotNull
	protected String	uom;

}
