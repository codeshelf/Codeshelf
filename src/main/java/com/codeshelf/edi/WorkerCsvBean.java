/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jon Ranstrom
 * Copyright Codeshelf 2015
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class WorkerCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(WorkerCsvBean.class);

	@NotNull
	@Size(min = 2)
	protected String	badgeId;

	protected String	firstName;

	protected String	lastName;

	protected String	workGroupName;

	protected String	humanResourcesId; 

	public final String getBadgeId() {
		return strip(badgeId);
	}

	public final String getlastName() {
		return strip(lastName);
	}
	public final String getWorkGroupName() {
		return strip(workGroupName);
	}
	public final String getHumanResourcesId() {
		return strip(humanResourcesId);
	}
	public final String getFirstName() {
		return strip(firstName);
	}
}
