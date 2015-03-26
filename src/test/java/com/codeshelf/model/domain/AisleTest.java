package com.codeshelf.model.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.AislesFileCsvImporter;
import com.codeshelf.edi.EdiFileReadException;
import com.codeshelf.model.dao.IDaoListener;
import com.codeshelf.testframework.HibernateTest;

public class AisleTest extends HibernateTest { // TODO: maybe split associatepathsegment into separate class- rest of tests can be mockdao
	private static final Logger	LOGGER			= LoggerFactory.getLogger(AisleTest.class);

	@Test
	public final void testGetLocationIdWithInvalidSublevel() {
		this.getTenantPersistenceService().beginTransaction();
		
		Aisle aisle = getDefaultAisle(getFacility(), "A1");
		String locationId = aisle.getLocationIdToParentLevel(Tier.class);
		Assert.assertEquals("", locationId);
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void updateControllerOnAisle() {
		// Case 1: simple add
		this.getTenantPersistenceService().beginTransaction();

		Facility facility = getFacility();
		CodeshelfNetwork network = facility.getNetworks().get(0);
		LedController controller1 = getDefaultController(network, "0xABCDEF");
		Aisle aisle = getDefaultAisle(getFacility(), "A1");

		Short testChannel = 8;
		aisle.setControllerChannel(controller1.getPersistentId().toString(), testChannel.toString());
		
		Aisle storedAisle = Aisle.staticGetDao().findByPersistentId(aisle.getPersistentId());
		assertEquals(controller1.getDomainId(), storedAisle.getLedControllerId());
		assertEquals(testChannel, storedAisle.getLedChannel());

		this.getTenantPersistenceService().commitTransaction();
		
		// Case 2: Cover the odd-ball case of aisle has a controller, but try to assign to a bad one.
		this.getTenantPersistenceService().beginTransaction();
		try {
			aisle.setControllerChannel(UUID.randomUUID().toString(),"2");
			fail("Should have thrown an exception");
		}
		catch(Exception e) {			
		}
		// verify that no change happened.
		assertEquals(controller1.getDomainId(), storedAisle.getLedControllerId());
		this.getTenantPersistenceService().commitTransaction();
		
		// Case 3: Make sure prior controller is removed
		this.getTenantPersistenceService().beginTransaction();
		LedController controller2 = getDefaultController(network, "0x000FFF");
		aisle.setControllerChannel(controller2.getPersistentId().toString(),"3");
		// verify that the change happened.
		assertEquals(controller2.getDomainId(), storedAisle.getLedControllerId());
		LOGGER.info("controller1: "+ controller1.getPersistentId().toString());
		LOGGER.info("controller2: "+ controller2.getPersistentId().toString());
		this.getTenantPersistenceService().commitTransaction();

	}

	@Test
	public final void updateNonexistantController() {
		this.getTenantPersistenceService().beginTransaction();

		Short testChannel = 8;
		Aisle aisle = getDefaultAisle(getFacility(), "A1");
		try {
			aisle.setControllerChannel(UUID.randomUUID().toString(), testChannel.toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public final void associatePathSegment() {
		this.getTenantPersistenceService().beginTransaction();
		IDaoListener listener = Mockito.mock(IDaoListener.class);
		this.getTenantPersistenceService().getEventListenerIntegrator().getChangeBroadcaster().registerDAOListener(this.getDefaultTenantId(),listener, Aisle.class);
		
		String aisleDomainId = "A1";
		
		Facility facility = getFacility();
		
		PathSegment pathSegment  = getDefaultPathSegment(getDefaultPath(facility, "P1"), 1);

		Aisle aisle = getDefaultAisle(facility, aisleDomainId);
		String segPersistId = pathSegment.getPersistentId().toString();
		this.getTenantPersistenceService().commitTransaction();
		
		
		this.getTenantPersistenceService().beginTransaction();
		aisle.associatePathSegment(segPersistId);
		// Paul: please see facility.recomputeLocationPathDistances()
		
		
		Aisle storedAisle = (Aisle) facility.findLocationById(aisleDomainId);
		assertEquals(pathSegment.getPersistentId(), storedAisle.getAssociatedPathSegment().getPersistentId());

		verify(listener, times(1)).objectAdded(eq(Aisle.class), eq(storedAisle.getPersistentId()));
		this.getTenantPersistenceService().commitTransaction();
		
		// Cover the odd-ball case of aisle has a path segment, but try to assign to a bad one.
		this.getTenantPersistenceService().beginTransaction();
		try {
			aisle.associatePathSegment(UUID.randomUUID().toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		// verify that no change happened.
		assertEquals(pathSegment.getPersistentId(), storedAisle.getAssociatedPathSegment().getPersistentId());
		this.getTenantPersistenceService().commitTransaction();		
	}
	
	@Test
	public final void updateNonexistantPathSegment() {
		this.getTenantPersistenceService().beginTransaction();

		Aisle aisle = getDefaultAisle(getFacility(), "A1");
		try {
			aisle.associatePathSegment(UUID.randomUUID().toString());
			fail("Should have thrown an exception");
		}
		catch(Exception e) {
			
		}
		
		this.getTenantPersistenceService().commitTransaction();
		
	}

	@SuppressWarnings("unused")
	@Test
	public final void testThrowInTransaction() {
		// DAO-correct
		// And nested transactions
		this.getTenantPersistenceService().beginTransaction();

		// Start with a file read to new facility
		String csvString = "binType,nominalDomainId,lengthCm,slotsInTier,ledCountInTier,tierFloorCm,controllerLED,anchorX,anchorY,orientXorY,depthCm\r\n" //
				+ "Aisle,A30,,,,,tierNotB1S1Side,12.85,43.45,Y,120,\r\n" //
				+ "Bay,B1,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n" //
				+ "Bay,B2,115,,,,,\r\n" //
				+ "Tier,T1,,5,40,0,,\r\n"; //

		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Facility facility = Facility.createFacility("F-AISLE30", "TEST", Point.getZeroPoint());

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		AislesFileCsvImporter importer = createAisleFileImporter();
		importer.importAislesFileFromCsvStream(reader, facility, ediProcessTime);

		this.getTenantPersistenceService().commitTransaction();

		this.getTenantPersistenceService().beginTransaction();

		List<Facility> listA = Facility.staticGetDao().getAll();
		Facility facilityA = listA.get(0);
		Slot slotB1T1S5 = (Slot) facilityA.findSubLocationById("A30.B1.T1.S5");
		Assert.assertTrue(slotB1T1S5.getActive());

		this.getTenantPersistenceService().commitTransaction();

		LOGGER.info("Case 1: try to store without a transaction should throw");
		boolean caughtExpected = false;
		try {
			slotB1T1S5.setActive(true);
			LOGGER.info("Modify a detached object was ok."); // Modify and forget to store will be an easy mistake to make.
			Slot.staticGetDao().store(slotB1T1S5);
			LOGGER.error("Should not see this message. Cannot store a detached object");
		} catch (HibernateException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		final boolean throwYes = true;
		final boolean throwNo = false;
		final boolean transactionYes = true;
		final boolean transactionNo = false;

		LOGGER.info("Case 2: simple nested transaction that might work. See errors from PersistenceService");
		this.getTenantPersistenceService().beginTransaction();
		setActiveValue(slotB1T1S5, false, transactionYes, throwNo);
		Assert.assertFalse(slotB1T1S5.getActive());
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertFalse(slotB1T1S5.getActive());

		LOGGER.info("Case 3: simple nested transaction will throw");
		this.getTenantPersistenceService().beginTransaction();
		caughtExpected = false;
		try {
			setActiveValue(slotB1T1S5, true, transactionYes, throwYes);
		} catch (EdiFileReadException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());

		LOGGER.info("Case 4: does nested transaction spoil the outer transaction? See errors from PersistenceService");
		this.getTenantPersistenceService().beginTransaction();
		setActiveValue(slotB1T1S5, true, transactionYes, throwNo);
		slotB1T1S5.setLedChannel((short) 4);
		try {
			Slot.staticGetDao().store(slotB1T1S5);
		} catch (HibernateException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());
		Assert.assertTrue(slotB1T1S5.getLedChannel() == 4);

		LOGGER.info("Case 5: Not a nested transaction. Will throw, leaving transaction open");
		caughtExpected = false;
		try {
			setActiveValue(slotB1T1S5, false, transactionYes, throwYes);
		} catch (EdiFileReadException e) {
			caughtExpected = true;
		}
		if (!caughtExpected)
			Assert.fail("did not see the expected throw");
		Assert.assertFalse(slotB1T1S5.getActive());

		LOGGER.info("Case 6: After the throw that left a transaction open, continue with normal transaction.");
		this.getTenantPersistenceService().beginTransaction();
		slotB1T1S5.setActive(true);
		Slot.staticGetDao().store(slotB1T1S5);
		this.getTenantPersistenceService().commitTransaction();
		Assert.assertTrue(slotB1T1S5.getActive());
	}

	protected void setActiveValue(Location inLocation, boolean inValue, boolean inWithTransaction, boolean inThrow) {
		if (inWithTransaction)
			this.getTenantPersistenceService().beginTransaction();

		inLocation.setActive(inValue);
		inLocation.<Location>getDao().store(inLocation);

		if (inThrow) {
			throw new EdiFileReadException("Just a throw because test commanded it to. No relevance to EDI.");
		}

		if (inWithTransaction)
			this.getTenantPersistenceService().commitTransaction();
	}


	
}
