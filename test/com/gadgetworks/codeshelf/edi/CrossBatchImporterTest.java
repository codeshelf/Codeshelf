/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;

import org.junit.Assert;
import org.junit.Test;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.UomMaster.UomMasterDao;

/**
 * @author jeffw
 * 
 */
public class CrossBatchImporterTest extends EdiTestABC {

	private ItemMaster createItemMaster(final String inItemMasterId, final String inUom, final Facility inFacility) {
		ItemMaster result = null;

		UomMaster uomMaster = inFacility.getUomMaster(inUom);
		if (uomMaster == null) {
			uomMaster = new UomMaster();
			uomMaster.setUomMasterId(inUom);
			uomMaster.setParent(inFacility);
			mUomMasterDao.store(uomMaster);
			inFacility.addUomMaster(uomMaster);
		}

		result = new ItemMaster();
		result.setItemId(inItemMasterId);
		result.setParent(inFacility);
		result.setStandardUom(uomMaster);
		result.setActive(true);
		result.setUpdated(new Timestamp(System.currentTimeMillis()));
		mItemMasterDao.store(result);
		inFacility.addItemMaster(result);

		return result;
	}

	@Test
	public final void testCrossBatchImporter() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ ",C111,I111.1,100,ea\r\n" //
				+ ",C111,I111.2,200,ea\r\n" //
				+ ",C111,I111.3,300,ea\r\n" //
				+ ",C111,I111.4,400,ea\r\n" //
				+ ",C222,I222.1,100,ea\r\n" //
				+ ",C222,I222.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS1");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS1", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-CROSS1");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I111.1", "ea", facility);
		createItemMaster("I111.2", "ea", facility);
		createItemMaster("I111.3", "ea", facility);
		createItemMaster("I111.4", "ea", facility);
		createItemMaster("I222.1", "ea", facility);
		createItemMaster("I222.2", "ea", facility);

		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		// Make sure we created an order with the container's ID.
		OrderHeader order = facility.getOrderHeader("C111");
		Assert.assertNotNull(order);
		Assert.assertEquals(order.getOrderTypeEnum(), OrderTypeEnum.CROSS);

		// Make sure there's a contianer use and that its ID matches the order.
		ContainerUse use = order.getContainerUse();
		Assert.assertNotNull(use);
		Assert.assertEquals(use.getParent().getContainerId(), "C111");

		// Make sure there's four order items.
		Assert.assertEquals(order.getOrderDetails().size(), 4);

	}

	@Test
	public final void testCrossBatchOrderGroups() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C333,I333.1,100,ea\r\n" //
				+ "G1,C333,I333.2,200,ea\r\n" //
				+ "G1,C333,I333.3,300,ea\r\n" //
				+ "G1,C333,I333.4,400,ea\r\n" //
				+ "G1,C444,I444.1,100,ea\r\n" //
				+ "G1,C444,I444.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS2");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS2", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-CROSS2");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I333.1", "ea", facility);
		createItemMaster("I333.2", "ea", facility);
		createItemMaster("I333.3", "ea", facility);
		createItemMaster("I333.4", "ea", facility);
		createItemMaster("I444.1", "ea", facility);
		createItemMaster("I444.2", "ea", facility);

		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		OrderGroup group = facility.getOrderGroup("G1");
		Assert.assertNotNull(group);

		OrderHeader order = group.getOrderHeader("C333");
		Assert.assertNotNull(order);

	}

	@Test
	public final void testResendCrossBatchRemoveItem() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C555,I555.1,100,ea\r\n" //
				+ "G1,C555,I555.2,200,ea\r\n" //
				+ "G1,C555,I555.3,300,ea\r\n" //
				+ "G1,C555,I555.4,400,ea\r\n" //
				+ "G1,C666,I666.1,400,ea\r\n" //
				+ "G1,C666,I666.2,400,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS3");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS3", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-CROSS3");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I555.1", "ea", facility);
		createItemMaster("I555.2", "ea", facility);
		createItemMaster("I555.3", "ea", facility);
		createItemMaster("I555.4", "ea", facility);
		createItemMaster("I666.1", "ea", facility);
		createItemMaster("I666.2", "ea", facility);

		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C555,I555.1,100,ea\r\n" //
				+ "G1,C555,I555.2,200,ea\r\n" //
				+ "G1,C555,I555.4,400,ea\r\n" //
				+ "G1,C666,I666.1,400,ea\r\n" //
				+ "G1,C666,I666.2,400,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		// Make sure that order detail item I555.3 still exists, but has quantity 0.
		OrderHeader order = facility.getOrderHeader("C555");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I555.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 0);

	}

	@Test
	public final void testResendCrossBatchAddItem() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C777,I777.1,100,ea\r\n" //
				+ "G1,C777,I777.2,200,ea\r\n" //
				+ "G1,C777,I777.3,300,ea\r\n" //
				+ "G1,C777,I777.4,400,ea\r\n" //
				+ "G1,C888,I888.1,100,ea\r\n" //
				+ "G1,C888,I888.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS4");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS4", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-CROSS4");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I777.1", "ea", facility);
		createItemMaster("I777.2", "ea", facility);
		createItemMaster("I777.3", "ea", facility);
		createItemMaster("I777.4", "ea", facility);
		createItemMaster("I777.5", "ea", facility);
		createItemMaster("I888.1", "ea", facility);
		createItemMaster("I888.2", "ea", facility);

		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C777,I777.1,100,ea\r\n" //
				+ "G1,C777,I777.2,200,ea\r\n" //
				+ "G1,C777,I777.3,300,ea\r\n" //
				+ "G1,C777,I777.4,400,ea\r\n" //
				+ "G1,C777,I777.5,500,ea\r\n" //
				+ "G1,C888,I888.1,100,ea\r\n" //
				+ "G1,C888,I888.2,200,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		// Make sure that order detail item I666.3 still exists, but has quantity 0.
		OrderHeader order = facility.getOrderHeader("C777");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I777.5");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 500);

	}

	@Test
	public final void testResendCrossBatchAlterItems() {

		String csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C999,I999.1,100,ea\r\n" //
				+ "G1,C999,I999.2,200,ea\r\n" //
				+ "G1,C999,I999.3,300,ea\r\n" //
				+ "G1,C999,I999.4,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,200,ea\r\n";

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization organization = new Organization();
		organization.setDomainId("O-CROSS5");
		mOrganizationDao.store(organization);

		organization.createFacility("F-CROSS5", "TEST", PositionTypeEnum.METERS_FROM_PARENT.getName(), 0.0, 0.0);
		Facility facility = organization.getFacility("F-CROSS5");

		// We can't import cross batch orders for items not already in inventory or on outbound orders.
		createItemMaster("I999.1", "ea", facility);
		createItemMaster("I999.2", "ea", facility);
		createItemMaster("I999.3", "ea", facility);
		createItemMaster("I999.4", "ea", facility);
		createItemMaster("IAAA.1", "ea", facility);
		createItemMaster("IAAA.2", "ea", facility);

		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		// Now re-import the interchange with one order missing a single item.
		csvString = "orderGroupId,containerId,itemId,quantity,uom\r\n" //
				+ "G1,C999,I999.1,100,ea\r\n" //
				+ "G1,C999,I999.2,200,ea\r\n" //
				+ "G1,C999,I999.3,999,ea\r\n" //
				+ "G1,C999,I999.4,400,ea\r\n" //
				+ "G1,CAAA,IAAA.1,100,ea\r\n" //
				+ "G1,CAAA,IAAA.2,200,ea\r\n";

		csvArray = csvString.getBytes();

		stream = new ByteArrayInputStream(csvArray);
		reader = new InputStreamReader(stream);

		importer = new CrossBatchCsvImporter(mOrderGroupDao,
			mOrderHeaderDao,
			mOrderDetailDao,
			mContainerDao,
			mContainerUseDao,
			mUomMasterDao);
		importer.importCrossBatchesFromCsvStream(reader, facility);

		// Make sure that order detail item I999.3 still exists, but has quantity 0.
		OrderHeader order = facility.getOrderHeader("C999");
		Assert.assertNotNull(order);
		OrderDetail orderDetail = order.getOrderDetail("I999.3");
		Assert.assertNotNull(orderDetail);
		Assert.assertEquals(orderDetail.getQuantity().intValue(), 999);

	}
}
