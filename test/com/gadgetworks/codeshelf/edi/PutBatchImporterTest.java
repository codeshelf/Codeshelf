/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;

/**
 * @author jeffw
 * 
 */
public class PutBatchImporterTest extends EdiTestABC {

	@Test
	public final void testPutBatchImporter() {

		String csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ ",C111,I111.1,Item 111.1 Desc,100,ea\r\n" //
				+ ",C111,I111.2,Item 111.2 Desc,200,ea\r\n" //
				+ ",C111,I111.3,Item 111.3 Desc,300,ea\r\n" //
				+ ",C111,I111.4,Item 111.4 Desc,400,ea\r\n" //
				+ ",C222,I222.1,Item 222.1 Desc,100,ea\r\n" //
				+ ",C222,I222.2,Item 222.2 Desc,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-PUT1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-PUT1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-PUT1");

		ICsvPutBatchImporter importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		// Make sure we created an order with the container's ID.
		OrderHeader order = facility.getOrderHeader("C111");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.WONDERWALL);

		// Make sure there's a contianer use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);

	}

	@Test
	public final void testPutBatchOrderGroups() {

		String csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ "G1,C333,I333.1,Item 333.1 Desc,100,ea\r\n" //
				+ "G1,C333,I333.2,Item 333.2 Desc,200,ea\r\n" //
				+ "G1,C333,I333.3,Item 333.3 Desc,300,ea\r\n" //
				+ "G1,C333,I333.4,Item 333.4 Desc,400,ea\r\n" //
				+ "G1,C444,I444.1,Item 444.1 Desc,100,ea\r\n" //
				+ "G1,C444,I444.2,Item 444.2 Desc,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-PUT2");
		mOrganizationDao.store(organization);

		organization.createFacility("F-PUT2", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-PUT2");

		ICsvPutBatchImporter importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		OrderGroup group = facility.getOrderGroup("G1");
		Assert.assertNotNull(group);

		OrderHeader order = group.getOrderHeader("C333");
		Assert.assertNotNull(order);

	}

	@Test
	public final void testResendPutBatchRemoveItem() {

		String csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ "G1,C555,I555.1,Item 555.1 Desc,100,ea\r\n" //
				+ "G1,C555,I555.2,Item 555.2 Desc,200,ea\r\n" //
				+ "G1,C555,I555.3,Item 555.3 Desc,300,ea\r\n" //
				+ "G1,C555,I555.4,Item 555.4 Desc,400,ea\r\n" //
				+ "G1,C666,I666.1,Item 666.1 Desc,400,ea\r\n" //
				+ "G1,C666,I666.2,Item 666.2 Desc,400,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-PUT3");
		mOrganizationDao.store(organization);

		organization.createFacility("F-PUT3", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-PUT3");

		ICsvPutBatchImporter importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ "G1,C555,I555.1,Item 555.1 Desc,100,ea\r\n" //
				+ "G1,C555,I555.2,Item 555.2 Desc,200,ea\r\n" //
				+ "G1,C555,I555.4,Item 555.4 Desc,400,ea\r\n" //
				+ "G1,C666,I666.1,Item 666.1 Desc,400,ea\r\n" //
				+ "G1,C666,I666.2,Item 666.2 Desc,400,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		// Make sure that order detail item I555.3 still exists, but has quantity 0.
		OrderHeader order = facility.getOrderHeader("C555");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I555.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 0);

	}

	@Test
	public final void testResendPutBatchAddItem() {

		String csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ "G1,C777,I777.1,Item 777.1 Desc,100,ea\r\n" //
				+ "G1,C777,I777.2,Item 777.2 Desc,200,ea\r\n" //
				+ "G1,C777,I777.3,Item 777.3 Desc,300,ea\r\n" //
				+ "G1,C777,I777.4,Item 777.4 Desc,400,ea\r\n" //
				+ "G1,C888,I888.1,Item 888.1 Desc,100,ea\r\n" //
				+ "G1,C888,I888.2,Item 888.2 Desc,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-PUT4");
		mOrganizationDao.store(organization);

		organization.createFacility("F-PUT4", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-PUT4");

		ICsvPutBatchImporter importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ "G1,C777,I777.1,Item 777.1 Desc,100,ea\r\n" //
				+ "G1,C777,I777.2,Item 777.2 Desc,200,ea\r\n" //
				+ "G1,C777,I777.3,Item 777.3 Desc,300,ea\r\n" //
				+ "G1,C777,I777.4,Item 777.4 Desc,400,ea\r\n" //
				+ "G1,C777,I777.5,Item 777.5 Desc,500,ea\r\n" //
				+ "G1,C888,I888.1,Item 888.1 Desc,100,ea\r\n" //
				+ "G1,C888,I888.2,Item 888.2 Desc,200,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		// Make sure that order detail item I666.3 still exists, but has quantity 0.
		OrderHeader order = facility.getOrderHeader("C777");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I777.5");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 500);

	}
	
	@Test
	public final void testResendPutBatchAlterItems() {

		String csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ "G1,C999,I999.1,Item 999.1 Desc,100,ea\r\n" //
				+ "G1,C999,I999.2,Item 999.2 Desc,200,ea\r\n" //
				+ "G1,C999,I999.3,Item 999.3 Desc,300,ea\r\n" //
				+ "G1,C999,I999.4,Item 999.4 Desc,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,Item AAA.1 Desc,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,Item AAA.2 Desc,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-PUT5");
		mOrganizationDao.store(organization);

		organization.createFacility("F-PUT5", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-PUT5");

		ICsvPutBatchImporter importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,description,quantity,uom\r\n" //
				+ "G1,C999,I999.1,Item 999.1 Desc,100,ea\r\n" //
				+ "G1,C999,I999.2,Item 999.2 Desc,200,ea\r\n" //
				+ "G1,C999,I999.3,Item 999.3 Desc,999,ea\r\n" //
				+ "G1,C999,I999.4,Item 999.4 Desc,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,Item AAA.1 Desc,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,Item AAA.2 Desc,200,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		importer = new PutBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mItemMasterDao,
			mUomMasterDao);
		importer.importPutBatchesFromCsvStream(reader, facility);

		// Make sure that order detail item I999.3 still exists, but has quantity 0.
		OrderHeader order = facility.getOrderHeader("C999");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I999.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 999);

	}
}
