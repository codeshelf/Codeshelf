/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvOrderImportBean.java,v 1.1 2012/09/24 08:23:47 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author jeffw
 *
 */
@ToString
public class CsvOrderImportBean {
	@Getter
	@Setter
	private String	orderId;

	@Getter
	@Setter
	private String	orderDetailId;

	@Getter
	@Setter
	private String	sku;

	@Getter
	@Setter
	private String	description;

	@Getter
	@Setter
	private String	quantity;

	@Getter
	@Setter
	private String	uom;

	@Getter
	@Setter
	private String	orderDate;
}
