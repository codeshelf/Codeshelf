package com.codeshelf.testframework;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.Callable;

import lombok.Setter;

import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.After;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.CrossBatchCsvImporter;
import com.codeshelf.edi.ICsvCrossBatchImporter;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionSequencerType;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.CodeshelfNetwork;
import com.codeshelf.model.domain.DomainObjectProperty;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.LedController;
import com.codeshelf.model.domain.Location;
import com.codeshelf.model.domain.Path;
import com.codeshelf.model.domain.PathSegment;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.sim.worker.PickSimulator;
import com.codeshelf.util.ThreadUtils;

public abstract class ServerTest extends HibernateTest {
	private final static Logger LOGGER = LoggerFactory.getLogger(ServerTest.class);
	@Setter
	private boolean skipFacilityDelete = false;
	
	@After
	public void deleteFacility() throws Exception{
		if (!skipFacilityDelete){
			boolean stale = true;
			int attempt = 0;
			do {
				try {
					stale = false;
					beginTransaction();
					Facility facility = Facility.staticGetDao().reload(getFacility());
					facility.delete();
					commitTransaction();
				} catch (StaleObjectStateException e) {
					//A number of tests don't wait for all DB changes to propagate, resulting in Stale errors on Facility delete 
					tenantPersistenceService.rollbackTransaction();
					stale = true;
					attempt++;
					LOGGER.warn("Stale error encountered while deleting facility. Attempt" + attempt);
					if (attempt > 3){
						throw e;
					}
					ThreadUtils.sleep(1000);
				} catch (ConstraintViolationException e) {
					SQLException se = e.getSQLException();
					throw (se == null ? e : se);
				}
			} while(stale);
		}
	}
	
	@Override
	Type getFrameworkType() {
		return Type.COMPLETE_SERVER;
	}

	@Override
	public boolean ephemeralServicesShouldStartAutomatically() {
		return true;
	}

	// various server utilities
	protected Facility setUpSimpleNoSlotFacility() {
		// This returns a facility with aisle A1, with two bays with one tier each. No slots. With a path, associated to the aisle.
		//   With location alias for first baytier only, not second.
		// The organization will get "O-" prepended to the name. Facility F-
		// Caller must use a different organization name each time this is used
		// Valid tier names: A1.B1.T1 = D101, and A1.B2.T1
		// Also, A1.B1 has alias D100
		// Just for variance, bay3 has 4 slots
		// Aisle 2 associated to same path segment. But with aisle controller on the other side
		// Aisle 3 will be on a separate path.
		// All tiers have controllers associated.
		// There are two CHE called CHE1 and CHE2

		/*
		Organization organization = new Organization();
		String oName = "O-" + inOrganizationName;
		organization.setDomainId(oName);
		mOrganizationDao.store(organization);
		*/

		/*
		String fName = "F-" + inOrganizationName;
		organization.createFacility(fName, "TEST", Point.getZeroPoint());
		Facility facility = organization.getFacility(fName);
		*/

		beginTransaction();
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A1,,,,,tierB1S1Side,12.85,43.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n" //
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,4,80,160,,\r\n" //
				+ "Aisle,A2,,,,,tierNotB1S1Side,12.85,55.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n" //
				+ "Aisle,A3,,,,,tierNotB1S1Side,12.85,65.45,X,120,Y\r\n" //
				+ "Bay,B1,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,0,,\r\n"//
				+ "Bay,B2,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,80,,\r\n" //
				+ "Bay,B3,230,,,,,\r\n" //
				+ "Tier,T1,,0,80,160,,\r\n"; //
		importAislesData(getFacility(), csvString);
		commitTransaction();

		// Get the aisle
		beginTransaction();
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 22.0, 48.45, 12.85, 48.45);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);

		Aisle aisle2 = Aisle.staticGetDao().findByDomainId(getFacility(), "A2");
		Assert.assertNotNull(aisle2);
		aisle2.associatePathSegment(persistStr);

		Path path2 = createPathForTest(getFacility());
		PathSegment segment02 = addPathSegmentForTest(path2, 0, 22.0, 58.45, 12.85, 58.45);

		Aisle aisle3 = Aisle.staticGetDao().findByDomainId(getFacility(), "A3");
		Assert.assertNotNull(aisle3);
		String persistStr2 = segment02.getPersistentId().toString();
		aisle3.associatePathSegment(persistStr2);
		commitTransaction();

		beginTransaction();
		String csvString2 = "mappedLocationId,locationAlias\r\n" //
				+ "A1.B1, D300\r\n" //
				+ "A1.B2, D400\r\n" //
				+ "A1.B3, D500\r\n" //
				+ "A1.B1.T1, D301\r\n" //
				+ "A1.B2.T1, D302\r\n" //
				+ "A1.B3.T1, D303\r\n" //
				+ "A2.B1.T1, D401\r\n" //
				+ "A2.B2.T1, D402\r\n" //
				+ "A2.B3.T1, D403\r\n"//
				+ "A3.B1.T1, D501\r\n" //
				+ "A3.B2.T1, D502\r\n" //
				+ "A3.B3.T1, D503\r\n";//
		importLocationAliasesData(getFacility(), csvString2);
		commitTransaction();

		beginTransaction();
		CodeshelfNetwork network = getNetwork();

		LedController controller1 = network.findOrCreateLedController("1", new NetGuid("0x00000011"));
		LedController controller2 = network.findOrCreateLedController("2", new NetGuid("0x00000012"));
		LedController controller3 = network.findOrCreateLedController("3", new NetGuid("0x00000013"));

		Short channel1 = 1;
		Facility facility = getFacility().reload();
		Location tier = facility.findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		// Make sure we also got the alias
		String tierName = tier.getPrimaryAliasId();
		if (!tierName.equals("D301"))
			LOGGER.error("D301 vs. A1.B1.T1 alias not set up in setUpSimpleNoSlotFacility");

		tier = facility.findSubLocationById("A1.B2.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A1.B3.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A2.B1.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A2.B2.T1");
		controller2.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A3.B1.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		tier = facility.findSubLocationById("A3.B2.T1");
		controller3.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		commitTransaction();

		return getFacility();
	}

	protected Facility setUpOneAisleFourBaysFlatFacilityWithOrders() throws IOException{
		beginTransaction();
		String aislesCsvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" +
				"Aisle,A1,,,,,zigzagB1S1Side,2.85,5,X,20\r\n" +
				"Bay,B1,50,,,,,,,,\r\n" +
				"Tier,T1,50,0,16,0,,,,,\r\n" +
				"Bay,B2,50,,,,,,,,\r\n" +
				"Tier,T1,50,0,16,0,,,,,\r\n" +
				"Bay,B3,50,,,,,,,,\r\n" +
				"Tier,T1,50,0,16,0,,,,,\r\n" +
				"Bay,B4,50,,,,,,,,\r\n" +
				"Tier,T1,50,0,16,0,,,,,\r\n"; //
		importAislesData(getFacility(), aislesCsvString);
		commitTransaction();

		// Get the aisle
		beginTransaction();
		Aisle aisle1 = Aisle.staticGetDao().findByDomainId(getFacility(), "A1");
		Assert.assertNotNull(aisle1);

		Path aPath = createPathForTest(getFacility());
		PathSegment segment0 = addPathSegmentForTest(aPath, 0, 3d, 6d, 5d, 6d);

		String persistStr = segment0.getPersistentId().toString();
		aisle1.associatePathSegment(persistStr);
		
		CodeshelfNetwork network = getNetwork();
		LedController controller1 = network.findOrCreateLedController("LED1", new NetGuid("0x00000011"));

		propertyService.changePropertyValue(getFacility(), DomainObjectProperty.WORKSEQR, WorkInstructionSequencerType.BayDistance.toString());
		commitTransaction();

		beginTransaction();
		Facility fac = Facility.staticGetDao().reload(getFacility());
		Short channel1 = 1;
		Location tier = fac.findSubLocationById("A1.B1.T1");
		controller1.addLocation(tier);
		tier.setLedChannel(channel1);
		tier.getDao().store(tier);
		commitTransaction();

		beginTransaction();
		String csvLocationAliases = "mappedLocationId,locationAlias\r\n" +
				"A1.B1.T1,LocX24\r\n" +
				"A1.B2.T1,LocX25\r\n" +
				"A1.B3.T1,LocX26\r\n" +
				"A1.B4.T1,LocX27\r\n";//
		importLocationAliasesData(getFacility(), csvLocationAliases);
		commitTransaction();

		beginTransaction();
		String inventory = "itemId,locationId,description,quantity,uom,inventoryDate,lotId,cmFromLeft\r\n" +
				"Item1,LocX24,Item Desc 1,1000,a,12/03/14 12:00,,0\r\n" +
				"Item2,LocX24,Item Desc 2,1000,a,12/03/14 12:00,,12\r\n" +
				"Item3,LocX24,Item Desc 3,1000,a,12/03/14 12:00,,24\r\n" +
				"Item4,LocX24,Item Desc 4,1000,a,12/03/14 12:00,,36\r\n" +
				"Item5,LocX25,Item Desc 5,1000,a,12/03/14 12:00,,0\r\n" +
				"Item6,LocX25,Item Desc 6,1000,a,12/03/14 12:00,,12\r\n" +
				"Item7,LocX25,Item Desc 7,1000,a,12/03/14 12:00,,24\r\n" +
				"Item8,LocX25,Item Desc 8,1000,a,12/03/14 12:00,,36\r\n" +
				"Item9,LocX26,Item Desc 9,1000,a,12/03/14 12:00,,0\r\n" +
				"Item10,LocX26,Item Desc 10,1000,a,12/03/14 12:00,,12\r\n" +
				"Item11,LocX26,Item Desc 11,1000,a,12/03/14 12:00,,24\r\n" +
				"Item12,LocX26,Item Desc 12,1000,a,12/03/14 12:00,,36\r\n" +
				"Item13,LocX27,Item Desc 13,1000,a,12/03/14 12:00,,0\r\n" +
				"Item14,LocX27,Item Desc 14,1000,a,12/03/14 12:00,,12\r\n" +
				"Item15,LocX27,Item Desc 15,1000,a,12/03/14 12:00,,24\r\n" +
				"Item16,LocX27,Item Desc 16,1000,a,12/03/14 12:00,,36\r\n";
		importInventoryData(getFacility(), inventory);
		commitTransaction();

		beginTransaction();
		String orders = "orderId,preAssignedContainerId,orderDetailId,orderDate,dueDate,itemId,description,quantity,uom,orderGroupId,workSequence,locationId\r\n" +
				"1,1,345,12/03/14 12:00,12/31/14 12:00,Item15,,90,a,Group1,,\r\n" +
				"1,1,346,12/03/14 12:00,12/31/14 12:00,Item7,,100,a,Group1,,\r\n" +
				"1,1,347,12/03/14 12:00,12/31/14 12:00,Item11,,120,a,Group1,,\r\n" +
				"1,1,348,12/03/14 12:00,12/31/14 12:00,Item9,,11,a,Group1,,\r\n" +
				"1,1,349,12/03/14 12:00,12/31/14 12:00,Item2,,22,a,Group1,,\r\n" +
				"1,1,350,12/03/14 12:00,12/31/14 12:00,Item5,,33,a,Group1,,\r\n" +
				"1,1,351,12/03/14 12:00,12/31/14 12:00,Item3,,22,a,Group1,5,LocX24\r\n" +
				"2,2,353,12/03/14 12:00,12/31/14 12:00,Item3,,44,a,Group1,,\r\n" +
				"2,2,354,12/03/14 12:00,12/31/14 12:00,Item15,,55,a,Group1,,\r\n" +
				"2,2,355,12/03/14 12:00,12/31/14 12:00,Item2,,66,a,Group1,,\r\n" +
				"2,2,356,12/03/14 12:00,12/31/14 12:00,Item8,,77,a,Group1,,\r\n" +
				"2,2,357,12/03/14 12:00,12/31/14 12:00,Item14,,77,a,Group1,,\r\n" +
				"7,7,346,12/03/14 12:00,12/31/14 12:00,Item2,,40,a,Group1,,\r\n" +
				"7,7,347,12/03/14 12:00,12/31/14 12:00,Item6,,30,a,Group1,,\r\n" +
				"8,8,349,12/03/14 12:00,12/31/14 12:00,Item2,,40,a,Group1,,\r\n" +
				"8,8,350,12/03/14 12:00,12/31/14 12:00,Item6,,30,a,Group1,,\r\n";
		importOrdersData(getFacility(), orders);
		commitTransaction();
		return getFacility();
	}

	// this method should be removed. state should be managed in unit test method.
	protected CodeshelfNetwork getNetwork() {
		return CodeshelfNetwork.staticGetDao().findByPersistentId(this.networkPersistentId);
	}
	
	public void logWiList(List<WorkInstruction> inList) {
		for (WorkInstruction wi : inList) {
			// If this is called from a list of WIs from the site controller, the WI may not have all its normal fields populated.
			String statusStr = padRight(wi.getStatusString(), 8);

			LOGGER.info(statusStr + " WiSort: " + wi.getGroupAndSortCode() + " cntr: " + wi.getContainerId() + " loc: "
					+ wi.getPickInstruction() + "(" + wi.getNominalLocationId() + ")" + " count: " + wi.getPlanQuantity()
					+ " SKU: " + wi.getItemId() + " order: " + wi.getOrderId() + " desc.: " + wi.getDescription());
		}
	}
	public void logOneWi(WorkInstruction inWi) {
		// If this is called from a list of WIs from the site controller, the WI may not have all its normal fields populated.
		String statusStr = padRight(inWi.getStatusString(), 8);

		LOGGER.info(statusStr + " " + inWi.getGroupAndSortCode() + " " + inWi.getContainerId() + " loc: "
				+ inWi.getPickInstruction() + "(" + inWi.getNominalLocationId() + ")" + " count: " + inWi.getPlanQuantity() + " actual: " + inWi.getActualQuantity()
				+ " SKU: " + inWi.getItemMasterId() + " order: " + inWi.getOrderId() + " desc.: " + inWi.getDescription());
	}
	public void logItemList(List<Item> inList) {
		for (Item item : inList)
			LOGGER.info("SKU: " + item.getItemMasterId() + " cm: " + item.getCmFromLeft() + " posAlongPath: "
					+ item.getPosAlongPathui() + " desc.: " + item.getItemDescription());
	}
	protected ICsvCrossBatchImporter createCrossBatchImporter() {
		ICsvCrossBatchImporter importer = new CrossBatchCsvImporter(eventProducer,
			workService);
		return importer;
	}

	protected int importBatchData(Facility facility, String csvString, Timestamp ediProcessTime) {
		ICsvCrossBatchImporter importer = createCrossBatchImporter();
		return importer.importCrossBatchesFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
	}

	protected int importBatchData(Facility facility, String csvString) {
		return importBatchData(facility,csvString,new Timestamp(System.currentTimeMillis()));
	}

	protected List<WorkInstruction> startWorkFromBeginning(Facility facility, String cheName, String containers) {
		// Now ready to run the cart
		CodeshelfNetwork theNetwork = facility.getNetworks().get(0);
		Che theChe = theNetwork.getChe(cheName);

		workService.setUpCheContainerFromString(theChe, containers);

		List<WorkInstruction> wiList = workService.getWorkInstructions(theChe, ""); // This returns them in working order.
		logWiList(wiList);
		return wiList;

	}

	protected PickSimulator waitAndGetPickerForProcessTypeX(IntegrationTest test, NetGuid cheGuid, String inProcessType) {
		ThreadUtils.sleep(250);
		long start = System.currentTimeMillis();
		final long maxTimeToWaitMillis = 5000;
		String existingType = "";
		int count = 0;
		while (System.currentTimeMillis() - start < maxTimeToWaitMillis) {
			count++;
			PickSimulator picker = new PickSimulator(test.getDeviceManager(), cheGuid);
			existingType = picker.getProcessType();
			if (existingType.equals(inProcessType)) {
				LOGGER.info(count + " pickers made in waitAndGetPickerForProcessType before getting it right");
				return picker;
			}
			ThreadUtils.sleep(100); // retry every 100ms
		}
		Assert.fail(String.format("Process type %s not encounter in %dms after %d checks. Process type is %s", inProcessType, maxTimeToWaitMillis, count, existingType));
		return null;
	}

	protected PickSimulator waitAndGetPickerForProcessType(final IntegrationTest test, final NetGuid deviceGuid, final String inProcessType) {
		Callable<PickSimulator> createPickSimulator = new Callable<PickSimulator> () {
			@Override
			public PickSimulator call() throws Exception {
				PickSimulator picker = new PickSimulator(test.getDeviceManager(), deviceGuid);
				String type = picker.getProcessType();
				return (type.equals(inProcessType))? picker : null;
			}
		};

		PickSimulator picker = new WaitForResult<PickSimulator>(createPickSimulator).waitForResult();
		return picker;
	}

	protected class WaitForResult <T>{
		private Callable<T> logicToCall;

		public WaitForResult(Callable<T> callable) {
			this.logicToCall = callable;
		}

		public T waitForResult() {
			ThreadUtils.sleep(250);
			long start = System.currentTimeMillis();
			final long maxTimeToWaitMillis = 5000;
			int count = 0;
			while (System.currentTimeMillis() - start < maxTimeToWaitMillis) {
				count++;
				T result = null;
				try {
					result = this.logicToCall.call();
				} catch (Exception e) {e.printStackTrace();}
				if (result != null) {
					LOGGER.info("Desired object retrieved in " + count + " attempts");
					return result;
				}
				ThreadUtils.sleep(100); // retry every 100ms
			}
			Assert.fail(String.format("Did not encounter requested object in %dms after %d checks.", maxTimeToWaitMillis, count));
			return null;
		}
	}

	public void verifyCheDisplay(PickSimulator picker, String exp1, String exp2, String exp3, String exp4) {
		String disp1 = picker.getLastCheDisplayString(1).trim();
		String disp2 = picker.getLastCheDisplayString(2).trim();
		String disp3 = picker.getLastCheDisplayString(3).trim();
		String disp4 = picker.getLastCheDisplayString(4).trim();
		String template = "%s\n%s\n%s\n%s";
		String expected = String.format(template, exp1, exp2, exp3, exp4);
		String displayed = String.format(template, disp1, disp2, disp3, disp4);
		Assert.assertEquals(expected, displayed);
	}

	/**
	 * A wrapper to avoid our endless casts of integers used in tests to Bytes to compare with deviceLogic values
	 * 	Without do this: Assert.assertEquals((Byte) (byte) 3, displayValue); and no range checking.
	 *  Instead do this: Assert.assertEquals(toByte(3), displayValue);
	 *  Does not throw. Logs error on out of range. Always returns a good Byte
	 */
	protected static Byte toByte(int theInt){
		if (theInt < 0 || theInt > 255) {
			LOGGER.error("toByte out of range");
			return new Byte((byte) 0);
		}
		else {
			return ((Byte) (byte) theInt);
		}
	}
	
	protected PickSimulator createPickSim(NetGuid cheGuid) {
		return new PickSimulator(this.getDeviceManager(), cheGuid);
	}

}
