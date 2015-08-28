/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: OrderImporterTest.java,v 1.11 2013/04/11 07:42:45 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.edi;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Gtin;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ThreadUtils;

/**
  *
 */
public class OutboundOrdersWithGtinTest extends ServerTest {
	private static final Logger	LOGGER	= LoggerFactory.getLogger(OutboundOrdersWithGtinTest.class);

	/*
	 * If multiple gtins are created for an item we may return the incorrect gtin. We do not check if a gtin exists
	 * for an item before creating a new one. Probably good to do in the future if this becomes an issue, however,
	 * multiple gtins for identical items does not make sense.
	 */
	@Test
	public final void testGtin() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();

		LOGGER.info("1: import orders");
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,90,each,Group1,1"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item2,,100,each,Group1,2"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,100,each,Group1,3"
				+ // Create new gtin for existing item. Not well supported
				"\r\n2,2,201,12/03/14 12:00,12/31/14 12:00,Item3,,90,each,Group1,4"
				+ "\r\n2,2,202,12/03/14 12:00,12/31/14 12:00,Item3,,90,cs,Group1,4"
				+ // Repeat gtin for diff UOM
				"\r\n2,2,203,12/03/14 12:00,12/31/14 12:00,Item4,,90,each,Group1,5"
				+ "\r\n2,2,204,12/03/14 12:00,12/31/14 12:00,Item5,,90,each,Group1,5" + // Repeat gtin for different item
				"\r\n2,2,205,12/03/14 12:00,12/31/14 12:00,Item5,,90,cs,Group1,6"; // Create new gtin for new UOM
		beginTransaction();
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		facility = facility.reload();

		LOGGER.info("2: look for order 1");
		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		Assert.assertNotNull(h1);
		OrderDetail d1_1 = h1.getOrderDetail("101");
		Gtin d1_1_gtin = d1_1.getItemMaster().getGtinForUom(d1_1.getUomMaster());
		Assert.assertEquals("1", d1_1_gtin.getDomainId());

		/*
		 * This tests unsupported functionality of multiple GTINs for the same item
		 * 2 or 3 would be "correct" answers
		 */
		/*
		OrderDetail d1_2 = h1.getOrderDetail("103");
		Gtin d1_2_gtin = d1_2.getItemMaster().getGtinForUom(d1_2.getUomMaster());
		Assert.assertEquals("2", d1_2_gtin.getDomainId());
		*/

		OrderHeader h2 = OrderHeader.staticGetDao().findByDomainId(facility, "2");
		OrderDetail d2_1 = h2.getOrderDetail("201");
		Gtin d2_1_gtin = d2_1.getItemMaster().getGtinForUom(d2_1.getUomMaster());
		Assert.assertNull(d2_1_gtin); // the gtin that was for this got converted to different uom. 

		OrderDetail d2_2 = h2.getOrderDetail("202");
		Gtin d2_2_gtin = d2_2.getItemMaster().getGtinForUom(d2_2.getUomMaster());
		Assert.assertNotNull(d2_2_gtin);
		Assert.assertEquals("4", d2_2_gtin.getDomainId()); // this was the converted one.

		OrderDetail d2_3 = h2.getOrderDetail("203");
		Gtin d2_3_gtin = d2_3.getItemMaster().getGtinForUom(d2_3.getUomMaster());
		Assert.assertEquals("5", d2_3_gtin.getDomainId());

		OrderDetail d2_4 = h2.getOrderDetail("204");
		Gtin d2_4_gtin = d2_4.getItemMaster().getGtinForUom(d2_4.getUomMaster());
		Assert.assertNull(d2_4_gtin);

		OrderDetail d2_5 = h2.getOrderDetail("205");
		Gtin d2_5_gtin = d2_5.getItemMaster().getGtinForUom(d2_5.getUomMaster());
		Assert.assertEquals("6", d2_5_gtin.getDomainId());

		commitTransaction();
	}

	/**
	 * Reimport with variations 4 times. See assorted warnings.
	 * In v20, yielding 13 WARN and one ERROR
	 */
	@Test
	public final void testGtinReimport() throws IOException {
		// initial order import
		Facility facility = setUpSimpleNoSlotFacility();		

		LOGGER.info("--1: import orders. Normal item ids");
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,Group1,gtin-1-each"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,each,Group1,gtin0000002";
		beginTransaction();
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		// check gtin
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList = Gtin.staticGetDao().getAll();
		List<ItemMaster> masterList = ItemMaster.staticGetDao().getAll();
		List<Item> itemList = Item.staticGetDao().getAll();
		Assert.assertEquals(3, gtinList.size());
		Assert.assertEquals(2, masterList.size());
		Assert.assertEquals(0, itemList.size());

		LOGGER.info("1b: look for order 1");
		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		Assert.assertNotNull(h1);
		OrderDetail d1_1 = h1.getOrderDetail("101");
		Gtin d1_1_gtin = d1_1.getItemMaster().getGtinForUom(d1_1.getUomMaster());
		Assert.assertEquals("gtin-1-each", d1_1_gtin.getDomainId());
		OrderDetail d1_2 = h1.getOrderDetail("102");
		Gtin d1_2_gtin = d1_2.getItemMaster().getGtinForUom(d1_2.getUomMaster());
		Assert.assertEquals("gtin-1-case", d1_2_gtin.getDomainId());
		commitTransaction();

		LOGGER.info("--2: re-import orders, modifying the name of gtin-1-each to gtin-1-mod");
		LOGGER.info("v17 result is a new gtin, leaving the other. No warning.");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,Group1,gtin-1-each-mod"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,each,Group1,gtin0000002";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("2b: count gtins. Did we modify one, or make new one?");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList2 = Gtin.staticGetDao().getAll();
		if (gtinList2.size() != 3)
			LOGGER.warn("gtins: {}",gtinList2);
		Assert.assertEquals(3, gtinList2.size());
		List<ItemMaster> masterList2 = ItemMaster.staticGetDao().getAll();
		List<Item> itemList2 = Item.staticGetDao().getAll();
		Assert.assertEquals(2, masterList2.size());
		Assert.assertEquals(0, itemList2.size());

		LOGGER.info("2c: look at the item1 gtins");
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		d1_1 = h1.getOrderDetail("101");

		ItemMaster sku_Item1 = d1_1.getItemMaster();
		Collection<Gtin> sku1Gtins = sku_Item1.getGtins().values();
		LOGGER.info("sku1 gtins =  {}", sku1Gtins);

		commitTransaction();

		LOGGER.info("--3a: re-import orders, modifying gtin2 to a different uom");
		LOGGER.info("v17 result leaving the original gtin as it was. There is a WARN");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,Group1,gtin-1-each-mod"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,case,Group1,gtin0000002";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("3b: count gtins. Did we modify one, or make new one?");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList3 = Gtin.staticGetDao().getAll();
		Assert.assertEquals(3, gtinList3.size());
		List<ItemMaster> masterList3 = ItemMaster.staticGetDao().getAll();
		List<Item> itemList3 = Item.staticGetDao().getAll();
		Assert.assertEquals(2, masterList3.size());
		Assert.assertEquals(0, itemList3.size());

		LOGGER.info("3c: look at the item2 gtins");
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail h1_3 = h1.getOrderDetail("103");

		ItemMaster sku_Item2 = h1_3.getItemMaster();
		Collection<Gtin> sku2Gtins = sku_Item2.getGtins().values();
		LOGGER.info("sku2 gtins =  {}", sku2Gtins);

		commitTransaction();

		LOGGER.info("--4a: re-import orders, modifying gtin2 to a different SKU");
		LOGGER.info("v17 result leaving the original gtin as it was. There is no WARN");
		LOGGER.info("and in v17, the one gtin is in both item2 and item3 master list of gtins");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item3,,80,case,Group1,gtin0000002";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("4b: count gtins. Did we modify one, or make new one?");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList4 = Gtin.staticGetDao().getAll();
		Assert.assertEquals(3, gtinList4.size());
		List<ItemMaster> masterList4 = ItemMaster.staticGetDao().getAll();
		List<Item> itemList4 = Item.staticGetDao().getAll();
		Assert.assertEquals(3, masterList4.size()); //See: Item3  now instead of Item2, which still exists as a master.
		Assert.assertEquals(0, itemList4.size());

		LOGGER.info("4c: look at the item3 gtins");
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		h1_3 = h1.getOrderDetail("103");

		ItemMaster sku_Item3 = h1_3.getItemMaster();
		Collection<Gtin> sku3Gtins = sku_Item3.getGtins().values();
		LOGGER.info("sku3 gtins =  {}", sku3Gtins);

		LOGGER.info("4d: what about item2?");
		sku_Item2 = ItemMaster.staticGetDao().reload(sku_Item2);
		sku2Gtins = sku_Item2.getGtins().values();
		LOGGER.info("sku2 gtins =  {}", sku2Gtins);

		commitTransaction();

	}

	/**
	 * Exactly the same as above, except that the itemId field has internal comma, and we have an extension point to modify it within a bean transform.
	 * Reimport with variations 4 times. See assorted warnings.
	 * In v20,  also yielding 13 WARN and one ERROR
	 */
	@Test
	public final void testGtinReimportBeanTransform() throws IOException {
		// initial order import
		Facility facility = setUpSimpleNoSlotFacility();		
		
		LOGGER.info("--1a: set up order bean transform to deal with commas in the itemId");
		beginTransaction();
		String beanText = "def OrderImportBeanTransformation(bean) {" +
				"\r\n       bean.itemId = bean.itemId.replace(',', '')" +
				"\r\n   	return bean;" +
				"\r\n  }";
		ExtensionPoint beanExtp = new ExtensionPoint(facility, ExtensionPointType.OrderImportBeanTransformation);
		beanExtp.setActive(true);
		beanExtp.setScript(beanText);
		ExtensionPoint.staticGetDao().store(beanExtp);
		
		commitTransaction();		

		LOGGER.info("--1b: import orders. The itemId has internal commas");
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,\"Ite,m1\",,70,each,Group1,gtin-1-each"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,\"Ite,m1\",,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,\"Ite,m2\",,80,each,Group1,gtin0000002";
		beginTransaction();
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		// check gtin
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList = Gtin.staticGetDao().getAll();
		List<ItemMaster> masterList = ItemMaster.staticGetDao().getAll();
		List<Item> itemList = Item.staticGetDao().getAll();
		Assert.assertEquals(3, gtinList.size());
		Assert.assertEquals(2, masterList.size());
		Assert.assertEquals(0, itemList.size());

		LOGGER.info("1b: look for order 1");
		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		Assert.assertNotNull(h1);
		OrderDetail d1_1 = h1.getOrderDetail("101");
		Gtin d1_1_gtin = d1_1.getItemMaster().getGtinForUom(d1_1.getUomMaster());
		Assert.assertEquals("gtin-1-each", d1_1_gtin.getDomainId());
		OrderDetail d1_2 = h1.getOrderDetail("102");
		Gtin d1_2_gtin = d1_2.getItemMaster().getGtinForUom(d1_2.getUomMaster());
		Assert.assertEquals("gtin-1-case", d1_2_gtin.getDomainId());
		commitTransaction();

		LOGGER.info("--2: re-import orders, modifying the name of gtin-1-each to gtin-1-mod");
		LOGGER.info("v17 result is a new gtin, leaving the other. No warning.");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,\"Ite,m1\",,70,each,Group1,gtin-1-each-mod"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,\"Ite,m1\",,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,\"Ite,m2\",,80,each,Group1,gtin0000002";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("2b: count gtins. Did we modify one, or make new one?");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList2 = Gtin.staticGetDao().getAll();
		if (gtinList2.size() != 3)
			LOGGER.warn("gtins: {}",gtinList2);
		Assert.assertEquals(3, gtinList2.size());
		List<ItemMaster> masterList2 = ItemMaster.staticGetDao().getAll();
		List<Item> itemList2 = Item.staticGetDao().getAll();
		Assert.assertEquals(2, masterList2.size());
		Assert.assertEquals(0, itemList2.size());

		LOGGER.info("2c: look at the item1 gtins");
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		d1_1 = h1.getOrderDetail("101");

		ItemMaster sku_Item1 = d1_1.getItemMaster();
		Collection<Gtin> sku1Gtins = sku_Item1.getGtins().values();
		LOGGER.info("sku1 gtins =  {}", sku1Gtins);

		commitTransaction();

		LOGGER.info("--3a: re-import orders, modifying gtin2 to a different uom");
		LOGGER.info("v17 result leaving the original gtin as it was. There is a WARN");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,\"Ite,m1\",,70,each,Group1,gtin-1-each-mod"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,\"Ite,m1\",,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,\"Ite,m2\",,80,case,Group1,gtin0000002";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("3b: count gtins. Did we modify one, or make new one?");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList3 = Gtin.staticGetDao().getAll();
		Assert.assertEquals(3, gtinList3.size());
		List<ItemMaster> masterList3 = ItemMaster.staticGetDao().getAll();
		List<Item> itemList3 = Item.staticGetDao().getAll();
		Assert.assertEquals(2, masterList3.size());
		Assert.assertEquals(0, itemList3.size());

		LOGGER.info("3c: look at the item2 gtins");
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		OrderDetail h1_3 = h1.getOrderDetail("103");

		ItemMaster sku_Item2 = h1_3.getItemMaster();
		Collection<Gtin> sku2Gtins = sku_Item2.getGtins().values();
		LOGGER.info("sku2 gtins =  {}", sku2Gtins);

		commitTransaction();

		LOGGER.info("--4a: re-import orders, modifying gtin2 to a different SKU");
		LOGGER.info("v17 result leaving the original gtin as it was. There is no WARN");
		LOGGER.info("and in v17, the one gtin is in both item2 and item3 master list of gtins");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,\"Ite,m1\",,70,case,Group1,gtin-1-case"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,\"Ite,m3\",,80,case,Group1,gtin0000002";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("4b: count gtins. Did we modify one, or make new one?");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtinList4 = Gtin.staticGetDao().getAll();
		Assert.assertEquals(3, gtinList4.size());
		List<ItemMaster> masterList4 = ItemMaster.staticGetDao().getAll();
		List<Item> itemList4 = Item.staticGetDao().getAll();
		Assert.assertEquals(3, masterList4.size()); //See: Item3  now instead of Item2, which still exists as a master.
		Assert.assertEquals(0, itemList4.size());

		LOGGER.info("4c: look at the item3 gtins");
		h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		h1_3 = h1.getOrderDetail("103");

		ItemMaster sku_Item3 = h1_3.getItemMaster();
		Collection<Gtin> sku3Gtins = sku_Item3.getGtins().values();
		LOGGER.info("sku3 gtins =  {}", sku3Gtins);

		LOGGER.info("4d: what about item2?");
		sku_Item2 = ItemMaster.staticGetDao().reload(sku_Item2);
		sku2Gtins = sku_Item2.getGtins().values();
		LOGGER.info("sku2 gtins =  {}", sku2Gtins);

		commitTransaction();

	}

	/**
	 * Mimicking what may happen at new site:
	 * testInventoryOnboarding() in CheProcessInventory should do this better
	 * After initial inventory, later the orders file has GTINS and may correct the itemMaster.
	 */
	@Test
	public final void testInventoryThenOrderImport() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		// No orders file yet, so no GTINs or OrderMasters in the system
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("1a: inventory three gtins to good locations. Later orders file will have the gtin and replace the item/uom");
		picker.inventoryViaTape("gtin1c", "L%D301"); // 
		picker.inventoryViaTape("gtin1e", "L%D302"); // 
		picker.inventoryViaTape("gtin2c", "L%D303"); // 

		// probably need to wait here to allow the transactions to complete.
		ThreadUtils.sleep(4000);

		beginTransaction();
		facility = Facility.staticGetDao().reload(facility);

		LOGGER.info("1b: Verify that with the 3 inventory actions with 3 gtins, 3 master and 3 items made");
		List<Gtin> gtins = Gtin.staticGetDao().getAll();
		List<ItemMaster> masters = ItemMaster.staticGetDao().getAll();
		List<Item> items = Item.staticGetDao().getAll();
		List<UomMaster> uoms = UomMaster.staticGetDao().getAll();
		Assert.assertEquals(3, gtins.size());
		Assert.assertEquals(3, masters.size());
		Assert.assertEquals(3, items.size());
		Assert.assertEquals(1, uoms.size());

		LOGGER.info("1c: See the gtins in the log or console");
		LOGGER.info("gtins {}", gtins);
		commitTransaction();

		// Following fails! "gtin1123" was made, guessing at master "gtin1123". When the orders file comes, the master SKU is "1123". The import does not
		// deal with this. After the import, our desired goal is no master with name gtin1123; there is a master SKU 1123; and gtin1123 belongs to the master.
		// Perhaps just changing the domainId of the master will work.
		// This is DEV-840

		LOGGER.info("2: Load the orders file with the 3 gtins and defined SKUs. This needs to update the gtin to the correct master");
		beginTransaction();
		facility = facility.reload();
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,gtin"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,Group1,gtin1c"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,Group1,gtin1e"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,case,Group1,gtin2c";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("3: See what we have now");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtins2 = Gtin.staticGetDao().getAll();
		List<ItemMaster> masters2 = ItemMaster.staticGetDao().getAll();
		List<Item> items2 = Item.staticGetDao().getAll();
		List<UomMaster> uoms2 = UomMaster.staticGetDao().getAll();
		Assert.assertEquals(3, gtins2.size());
		Assert.assertEquals(3, masters2.size());
		Assert.assertEquals(3, items2.size());
		Assert.assertEquals(2, uoms2.size());// the inventory created the default EA. In the orders file, each matched to it, and case was created.
		LOGGER.info("2b: See the gtins in the log or console");
		LOGGER.info("gtins {}", gtins2);

		commitTransaction();
	}

	/**
	 * Reproduce Accu's problem. Core-E only handles up to UPC-12. But some Lunera products use 17 chars. So truncated.
	 * Can these be inventoried? Can they be picked with a scan?
	 */
	@Test
	public final void testTooLongGtin() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		// No orders file yet, so no GTINs or OrderMasters in the system
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("1a: inventory two gtins to good locations. Later orders file will have the gtin and replace the item/uom");
		LOGGER.info("these are 17 character gtins, too long for Core-E");
		picker.inventoryViaTape("12345678901234567", "L%D301"); // 
		picker.inventoryViaTape("12345678901334567", "L%D302"); // 

		// probably need to wait here to allow the transactions to complete.
		ThreadUtils.sleep(4000);
		
		LOGGER.info("1b: We should have two item masters, and two gtin now");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(2, gtins.size());
		List<ItemMaster> masters = ItemMaster.staticGetDao().getAll();
		LOGGER.info("masters {}", masters);
		Assert.assertEquals(2, masters.size());
		commitTransaction();

		LOGGER.info("2: Load the orders file with the 4 gtins suffering from Core-E trunctionation to 12 characters");
		LOGGER.info("   The gtin and master caches should have two");
	beginTransaction();
		facility = facility.reload();
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,123456789012"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,123456789013"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,case,123456789014"
				+ "\r\n1,1,104,12/03/14 12:00,12/31/14 12:00,Item3,,80,case,123456789015";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("2b: See what gtin we have. Do we have 4 or 6? Answer 4. We successful transformed the itemLocations for item1/ea and item1/case");
		beginTransaction();
		facility = facility.reload();
		gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(4, gtins.size());
		commitTransaction();

		LOGGER.info("3: inventory the other two gtins to good locations. We scan the full gtin");
		picker.inventoryViaTape("12345678901434567", "L%D303"); // 
		picker.inventoryViaTape("12345678901534567", "L%D401"); // 
		picker.scanCommand("CANCEL");
		picker.waitForCheState(CheStateEnum.SETUP_SUMMARY, 1000);

		LOGGER.info("3b: See what we have now. 4 or 6? Answer 6.");
		beginTransaction();
		facility = facility.reload();
		gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(6, gtins.size());
		commitTransaction();
		// The only way to continue is to get to one gtin per order line
		// Note that during order import we have the full gtinCache available. But not during the server-side inventory transaction via gtin.
		// We do not want to do a full query there in order to call findUniqueManufacturedSubstringMatch()

		// After this, normal pickscan applies. Full gtin is in the database. Full scan will match. No need for separate testing here.

		LOGGER.info("4a: Demonstrate how user would fix. We have two extra gtin now. In the pairs, one has correct gtin location with an item, and one correct details for truncated gtin.");

	}

	/**
	 * Core-E only handles up to UPC-12. But some Lunera products use 17 chars. So truncated.
	 * Just explore the order import issue.
	 */
	@Test
	public final void truncatedGtinInFile() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		// No orders file yet, so no GTINs or OrderMasters in the system

		LOGGER.info("1: Load the orders file with the 4 long gtins. This is just to get correct data into the system");
		// At accu, this would be done by editing the gtins in webapp
		beginTransaction();
		facility = facility.reload();
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,12345678901234567"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,12345678901334567"
				+ "\r\n1,1,103,12/03/14 12:00,12/31/14 12:00,Item2,,80,case,12345678901434567"
				+ "\r\n1,1,104,12/03/14 12:00,12/31/14 12:00,Item3,,80,case,12345678901534567";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("1b: See that we have 4 gtin now.");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(4, gtins.size());
		commitTransaction();

		LOGGER.info("2a: Import new order line for item1/case, but with truncated Gtin. This should do a GTIN case 5 match and not make a new gtin");
		// Actually checked below in gtins.size() assert.
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n2,2,2_01,12/03/14 12:00,12/31/14 12:00,Item1,,80,case,123456789012";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("2b: Import new order line for item1/case, but stripping off the front of the Gtin. This should do a GTIN case 5 match and not make a new gtin");
		// Actually checked below in gtins.size() assert.
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n3,3,3_01,12/03/14 12:00,12/31/14 12:00,Item1,,80,case,678901234567";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("3: Import new order line for item1/each, but with an internal substring of Gtin. This does not match");
		// This looks like the host corrected a GTIN so it does change, and does not make a new one. See GGTIN_case_4b in log
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n4,4,402,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,345678901334";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(4, gtins.size());
		commitTransaction();

		LOGGER.info("4: Import new order line for item2/each, wth a truncation that would match the item2 case gtin");
		// This will make a new gtin
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n5,5,501,12/03/14 12:00,12/31/14 12:00,Item2,,80,each,123456789014";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(5, gtins.size());
		commitTransaction();
	}

	/**
	 * Sometimes, someone will pass an orders file through Excel, which results in removal of leading zeros.
	 * If we import this, do not make a mess.
	 */
	@Test
	public final void strippedLeadingZerosGtin() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		// No orders file yet, so no GTINs or OrderMasters in the system
		startSiteController();
		PickSimulator picker = createPickSim(cheGuid1);

		picker.loginAndCheckState("Picker #1", CheStateEnum.SETUP_SUMMARY);
		picker.scanCommand("INVENTORY");
		picker.waitForCheState(CheStateEnum.SCAN_GTIN, 1000);

		LOGGER.info("1a: inventory two gtins with leading zeros to good locations.");
		picker.inventoryViaTape("000123456788", "L%D301"); // 
		picker.inventoryViaTape("000123456789", "L%D302"); // 

		// probably need to wait here to allow the transactions to complete.
		ThreadUtils.sleep(4000);

		LOGGER.info("2: Load the orders file that had those two truncated. Do they match up?");
		beginTransaction();
		facility = facility.reload();
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,123456788"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,123456789";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("2b: See what gtin we have. Do we have 2 or 4? Answer 2. They resolved due to trailing truncation match, which is good.");
		// No special case needed for zeros.
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(2, gtins.size());
		List<ItemMaster> masters = ItemMaster.staticGetDao().getAll();
		LOGGER.info("masters {}", masters);
		Assert.assertEquals(2, masters.size());

		// The gtins should have leading zeros.
		Gtin gtin = gtins.get(0);
		char firstChar = gtin.getGtin().charAt(0);
		Assert.assertEquals('0', firstChar);
		List<UomMaster> uoms = UomMaster.staticGetDao().getAll();
		Assert.assertEquals(2, uoms.size());// the original scan without UOM to EA, which normalizes with each. Add case from this import.
		commitTransaction();

		LOGGER.info("3: Load the orders file again, but without losing leading zeros");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,000123456788"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,000123456789";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("3b: See what we have now. 2 or 4? Answer 2.");
		beginTransaction();
		facility = facility.reload();
		gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(2, gtins.size());

		masters = ItemMaster.staticGetDao().getAll();
		LOGGER.info("masters {}", masters);
		Assert.assertEquals(2, masters.size());

		// The gtins should still have leading zeros.
		gtin = gtins.get(0);
		firstChar = gtin.getGtin().charAt(0);
		Assert.assertEquals('0', firstChar);
		uoms = UomMaster.staticGetDao().getAll();
		Assert.assertEquals(2, uoms.size());
		commitTransaction();

		LOGGER.info("4: Load the orders file without leading zeros");
		// looks like same case as 2. But it is not. In case 2, the leading zero gtin was "manufactured".
		// Now in test 4, the existing leading zero gtin was converted, and is no longer "manufactured".
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,70,case,123456788"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item1,,70,each,123456789";
		importOrdersDataHandlingTruncatedGtins(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("4b: Still 2 gtin with leading zeros.");
		beginTransaction();
		facility = facility.reload();
		gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		uoms = UomMaster.staticGetDao().getAll();
		Assert.assertEquals(2, uoms.size());// the default bad one, and each and case from this import.
		LOGGER.info("uoms {}", uoms);

		masters = ItemMaster.staticGetDao().getAll();
		LOGGER.info("masters {}", masters);
		Assert.assertEquals(2, masters.size());

		Assert.assertEquals(2, gtins.size());
		// The gtins should have leading zeros.
		gtin = gtins.get(0);
		firstChar = gtin.getGtin().charAt(0);
		Assert.assertEquals('0', firstChar);
		commitTransaction();

	}
	/**
	 * Nipul did something in test that seemed to show dropping an orders file did not update a gtin on a master.
	 * This test tries to reproduce
	 */
	@Test
	public final void changingUom() throws IOException {
		Facility facility = setUpSimpleNoSlotFacility();
		// No orders file yet, so no GTINs or OrderMasters in the system

		LOGGER.info("1: Load the orders file with the no gtins and each");
		beginTransaction();
		facility = facility.reload();
		String firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,7,each"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item2,,9,each"
				+ "\r\n2,1,201,12/03/14 12:00,12/31/14 12:00,Item1,,70,each";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		LOGGER.info("1b: See that we have 1 gtin now.");
		beginTransaction();
		facility = facility.reload();
		List<Gtin> gtins = Gtin.staticGetDao().getAll();
		Assert.assertEquals(0, gtins.size());
		commitTransaction();

		LOGGER.info("2: Import new orders file gtin but using EA");
		beginTransaction();
		facility = facility.reload();
		firstCsvString = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,gtin"
				+ "\r\n1,1,101,12/03/14 12:00,12/31/14 12:00,Item1,,7,EA,1234567890123"
				+ "\r\n1,1,102,12/03/14 12:00,12/31/14 12:00,Item2,,9,EA,1234567890999"
				+ "\r\n2,1,201,12/03/14 12:00,12/31/14 12:00,Item1,,70,EA,1234567890123";
		importOrdersData(facility, firstCsvString);
		commitTransaction();

		beginTransaction();
		gtins = Gtin.staticGetDao().getAll();
		LOGGER.info("gtins {}", gtins);
		Assert.assertEquals(2, gtins.size());
		
		LOGGER.info("2b: look for order 1");
		OrderHeader h1 = OrderHeader.staticGetDao().findByDomainId(facility, "1");
		Assert.assertNotNull(h1);
		OrderDetail d1_1 = h1.getOrderDetail("101");
		String gtinId = d1_1.getGtinId();
		Assert.assertEquals("1234567890123", gtinId);

		commitTransaction();
	}


}
