/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: CsvInventoryImportBean.java,v 1.1 2012/10/22 07:38:07 jeffw Exp $
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
public class CsvInventoryImportBean {
	@Getter
	@Setter
	private String	itemId;

	@Getter
	@Setter
	private String	description;

	@Getter
	@Setter
	private String	quantity;

	@Getter
	@Setter
	private String	uomId;

	@Getter
	@Setter
	private String	inventoryDate;
	
	@Getter
	@Setter
	private String	lotId;
	
	@Getter
	@Setter
	private String	locationId;
}
