/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2014, Codeshelf, All rights reserved
 *  Author Jon Ranstrom
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;



import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.DomainTestABC;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
// domain objects needed
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.Slot;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.Vertex;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.UomMaster;


/**
 * 
 * 
 */
public class CrossBatchRunTest extends EdiTestABC {

	@Test
	public final void doNothing() {
		Assert.assertTrue(true);
	}

}
