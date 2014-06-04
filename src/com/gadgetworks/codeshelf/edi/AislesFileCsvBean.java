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
	protected String	tubeLightKind;
	protected String	pickFaceEndX;
	protected String	pickFaceEndY;

	public final String getBinType() {
		return strip(binType);
	}
	public final String getNominalDomainID() {
		return strip(nominalDomainId);
	}
	public final String getLengthCm() {
		return strip(lengthCm);
	}
	public final String getSlotsInTier() {
		return strip(slotsInTier);
	}
	public final String getControllerLed() {
		return strip(controllerLed);
	}
	public final String getAnchorX() {
		return strip(anchorX);
	}
	public final String getAnchorY() {
		return strip(anchorY);
	}
	public final String getTubeLightKind() {
		return strip(tubeLightKind);
	}
	public String getPickFaceEndX() {
		return strip(pickFaceEndX);
	}
	public String getPickFaceEndY() {
		return strip(pickFaceEndY);
	}

}
