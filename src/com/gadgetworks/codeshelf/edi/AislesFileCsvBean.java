/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderCsvBean.java,v 1.2 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ranstrom
 *
 */

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AislesFileCsvBean extends ImportCsvBeanABC {

	static final Logger	LOGGER	= LoggerFactory.getLogger(LocationAliasCsvBean.class);

	@NotNull
	@Size(min = 2)
	protected String	binType;
	@NotNull
	@Size(min = 1)
	protected String	nominalDomainId;

	protected String	lengthCm;
	protected String	slotsInTier;
	protected String	controllerLed;
	protected String	anchorX;
	protected String	anchorY;
	protected String	ledCountInTier;
	protected String	tierFloorCm;
	protected String	orientXorY;
	protected String	depthCm;
	protected String	pickFaceEndX; // might be computable later
	protected String	pickFaceEndY; // might be computable later

	/* Using stripNull instead of strip. If user file is missing some fields, 
	 * will return empty string instead of null, which may will throw. */
	
	public final String getBinType() {
		return stripNull(binType);
	}
	public final String getNominalDomainID() {
		return stripNull(nominalDomainId);
	}
	public final String getLengthCm() {
		return stripNull(lengthCm);
	}
	public final String getSlotsInTier() {
		return stripNull(slotsInTier);
	}
	public final String getControllerLed() {
		return stripNull(controllerLed);
	}
	public final String getAnchorX() {
		return stripNull(anchorX);
	}
	public final String getAnchorY() {
		return stripNull(anchorY);
	}
	public final String getLedCountInTier() {
		return stripNull(ledCountInTier);
	}
	public final String getTierFloorCm() {
		return stripNull(tierFloorCm);
	}
	public final String getOrientXorY() {
		return stripNull(orientXorY);
	}
	public final String getDepthCm() {
		return stripNull(depthCm);
	}
	public final String getPickFaceEndX() {
		return stripNull(pickFaceEndX);
	}
	public final String getPickFaceEndY() {
		return stripNull(pickFaceEndY);
	}

}
