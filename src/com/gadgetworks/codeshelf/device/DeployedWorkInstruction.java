/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2013, Jeffrey B. Williams, All rights reserved
 *  $Id: DeployedWorkInstruction.java,v 1.1 2013/03/03 23:27:21 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.device;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author jeffw
 *
 */
public class DeployedWorkInstruction {

	// Container
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mContainer;

	
	// Location
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mLocation;

	// SKU
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mSkuId;

	// Quantity
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private Integer					mQuantity;
	
	// Aisle controller ID.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mAisleController;
	
	// Aisle controller command.
	@Accessors(prefix = "m")
	@Getter
	@Setter
	private String					mAisleControllerCmd;
	
	public DeployedWorkInstruction() {
	}

}
