/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvImportBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import lombok.Data;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.validation.NotNull;

/**
 * @author jeffw
 *
 */
@ToString
@Data
public class LocationAliasCsvImportBean extends CsvImportBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAliasCsvImportBean.class);

	@NotNull
	protected String	locationAlias;
	@NotNull
	protected String	mappedLocationId;

	public final String getLocationAlias() {
		return StringUtils.strip(locationAlias);
	}

	public final String getMappedLocationId() {
		return StringUtils.strip(mappedLocationId);
	}
}
