/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: InventoryImporterTest.java,v 1.8 2013/04/07 21:34:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.edi;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.gadgetworks.codeshelf.application.IUtil;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.Database;
import com.gadgetworks.codeshelf.model.dao.H2SchemaManager;
import com.gadgetworks.codeshelf.model.dao.IDatabase;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.model.dao.MockDao;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Facility.FacilityDao;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Organization;
import com.gadgetworks.codeshelf.model.domain.Organization.OrganizationDao;
import com.gadgetworks.codeshelf.model.domain.PersistentProperty;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC;
import com.gadgetworks.codeshelf.model.domain.SubLocationABC.SubLocationDao;
import com.gadgetworks.codeshelf.model.domain.UomMaster;

/**
 * @author jeffw
 *
 */
public class InventoryImporterTest {

	private static IUtil			mUtil;
	private static ISchemaManager	mSchemaManager;
	private static IDatabase		mDatabase;

	@BeforeClass
	public final static void setup() {

		try {
			mUtil = new IUtil() {

				public void setLoggingLevelsFromPrefs(Organization inOrganization, ITypedDao<PersistentProperty> inPersistentPropertyDao) {
				}

				public String getVersionString() {
					return "";
				}

				public String getApplicationLogDirPath() {
					return ".";
				}

				public String getApplicationDataDirPath() {
					return ".";
				}

				public void exitSystem() {
					System.exit(-1);
				}
			};

			Class.forName("org.h2.Driver");
			mSchemaManager = new H2SchemaManager(mUtil, "codeshelf", "codeshelf", "codeshelf", "CODESHELF", "localhost", "");
			mDatabase = new Database(mSchemaManager, mUtil);

			mDatabase.start();
		} catch (ClassNotFoundException e) {
		}
	}

	@Test
	public void testInventoryImporterFromCsvStream() {

		String csvString = "itemId,description,quantity,uomId,locationId,lotId,inventoryDate\r\n" //
				+ "3001,Widget,100,each,B1,111,2012-09-26 11:31:01\r\n" //
				+ "4550,Gadget,450,case,B2,222,2012-09-26 11:31:01\r\n" //
				+ "3007,Dealybob,300,case,B3,333,2012-09-26 11:31:02\r\n" //
				+ "2150,Thingamajig,220,case,B4,444,2012-09-26 11:31:03\r\n" //
				+ "2170,Doodad,125,each,B5,555,2012-09-26 11:31:03";

		byte csvArray[] = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Organization.DAO = new OrganizationDao();
		Organization organization = new Organization();
		organization.setDomainId("O1");
		Organization.DAO.store(organization);

		Facility.DAO = new FacilityDao();
		Facility facility = new Facility();
		facility.setParent(organization);
		facility.setDomainId("F1");
		facility.setPosTypeEnum(PositionTypeEnum.METERS_FROM_PARENT);
		facility.setPosX(0.0);
		facility.setPosY(0.0);
		Facility.DAO.store(facility);

		SubLocationABC.DAO = new SubLocationDao();

		MockDao<OrderGroup> orderGroupDao = new MockDao<OrderGroup>();
		MockDao<OrderHeader> orderHeaderDao = new MockDao<OrderHeader>();
		MockDao<OrderDetail> orderDetailDao = new MockDao<OrderDetail>();
		MockDao<Container> containerDao = new MockDao<Container>();
		ITypedDao<ContainerUse> containerUseDao = new MockDao<ContainerUse>();
		MockDao<ItemMaster> itemMasterDao = new MockDao<ItemMaster>();
		MockDao<Item> itemDao = new MockDao<Item>();
		ITypedDao<UomMaster> uomMasterDao = new MockDao<UomMaster>();

		CsvImporter importer = new CsvImporter(orderGroupDao, orderHeaderDao, orderDetailDao, containerDao, containerUseDao, itemMasterDao, itemDao, uomMasterDao);
		importer.importInventoryFromCsvStream(reader, facility);

		Item item = facility.getItem("3001");
		Assert.assertNotNull(item);

		ItemMaster itemMaster = item.getItemMaster();
		Assert.assertNotNull(itemMaster);

	}
}
