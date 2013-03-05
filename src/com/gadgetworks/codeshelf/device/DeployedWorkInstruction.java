/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: DeployedWorkInstruction.java,v 1.2 2013/03/05 00:05:01 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import com.gadgetworks.flyweight.command.ColorEnum;

/**
 * @author jeffw
 *
 */
@ToString
public class DeployedWorkInstruction {

	// Container
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String		mContainerId;

	// Location
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String		mLocation;

	// SKU
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String		mSkuId;

	// Quantity
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Integer		mQuantity;

	// Aisle controller ID.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String		mAisleController;

	// Aisle controller command.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String		mAisleControllerCmd;

	// The light color to use.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private ColorEnum	mColor;

	public DeployedWorkInstruction() {
	}

}
