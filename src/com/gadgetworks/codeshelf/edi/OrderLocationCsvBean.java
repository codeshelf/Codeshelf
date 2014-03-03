/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class OrderLocationCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(OrderLocationCsvBean.class);

	protected String	orderId;

	protected String	locationId;
	
	public final String getOrderId() {
		return StringUtils.strip(orderId);
	}
	
	public final String getLocationId() {
		return StringUtils.strip(locationId);
	}

}
