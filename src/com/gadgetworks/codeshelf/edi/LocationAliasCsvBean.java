/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jeffw
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class LocationAliasCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAliasCsvBean.class);

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
