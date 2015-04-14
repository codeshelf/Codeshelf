package com.codeshelf.integration;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.device.CheDeviceLogic;
import com.codeshelf.device.CheStateEnum;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.WorkInstruction;

public class CheProcessPutWallSlowPick extends CheProcessPutWallSuper{
	private static final Logger	LOGGER			= LoggerFactory.getLogger(CheProcessPutWallSlowPick.class);
	private static final int	WAIT_TIME		= 4000;
	
	@Test
	public final void slowMoverWorkInstructions() throws IOException {
		// This is for DEV-711

		this.getTenantPersistenceService().beginTransaction();
		setUpFacilityWithPutWall();
		setUpOrders1(getFacility());
		this.getTenantPersistenceService().commitTransaction();

		this.startSiteController();
		PickSimulator picker1 = new PickSimulator(this, cheGuid1);

		LOGGER.info("1: Just set up some orders to the put wall. Intentionally choose order with inventory location in the slow mover area.");
		picker1.login("Picker #1");
		picker1.scanCommand("ORDER_WALL");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11114");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P14");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11115");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P15");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanSomething("11116");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_LOCATION, WAIT_TIME);
		picker1.scanSomething("L%P16");
		picker1.waitForCheState(CheStateEnum.PUT_WALL_SCAN_ORDER, WAIT_TIME);
		picker1.scanCommand("CLEAR");
		picker1.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);

		LOGGER.info("2: P14 is in WALL1. P15 and P16 are in WALL2. Set up slow mover CHE for that SKU pick");
		// Verify that orders 11114, 11115, and 11116 are having order locations in put wall
		this.getTenantPersistenceService().beginTransaction();
		Facility facility = Facility.staticGetDao().reload(getFacility());
		assertOrderLocation("11114", "P14");
		assertOrderLocation("11115", "P15");
		assertOrderLocation("11116", "P16");
		assertItemMaster(facility, "1514");
		assertItemMaster(facility, "1515");
		this.getTenantPersistenceService().commitTransaction();

		PickSimulator picker2 = new PickSimulator(this, cheGuid2);
		picker2.login("Picker #2");
		picker2.waitForCheState(CheStateEnum.CONTAINER_SELECT, WAIT_TIME);
		picker2.setupOrderIdAsContainer("WALL1", "1");
		picker2.setupOrderIdAsContainer("WALL2", "2");

		// picker2.startAndSkipReview("S11", WAIT_TIME, WAIT_TIME);
		picker2.scanCommand("START");
		picker2.waitForCheState(CheStateEnum.LOCATION_SELECT, WAIT_TIME);
		Byte posConValue1 = picker2.getLastSentPositionControllerDisplayValue((byte) 1);
		Byte posConValue2 = picker2.getLastSentPositionControllerDisplayValue((byte) 2);
		Assert.assertEquals(new Byte("1"), posConValue1);
		Assert.assertEquals(new Byte("1"), posConValue2);

		LOGGER.info("3: The result should be only two work instructions, as orders 11115 and 11116 are for the same SKU on the same wall.");
		picker2.scanCommand(CheDeviceLogic.STARTWORK_COMMAND);
		picker2.waitForCheState(CheStateEnum.DO_PICK, WAIT_TIME);

		// DEV-711 ComputeWorkInstructions will achieve this.
		List<WorkInstruction> wiList = picker2.getAllPicksList();
		Assert.assertEquals(2, wiList.size());
		WorkInstruction wi1 = wiList.get(0);
		WorkInstruction wi2 = wiList.get(1);
		Assert.assertEquals("Item mismatch", "1514", wi1.getItemId());
		Assert.assertEquals("Quantity mismatch", new Integer(3), wi1.getPlanQuantity());
		Assert.assertEquals("Item mismatch", "1515", wi2.getItemId());
		Assert.assertEquals("Quantity mismatch", new Integer(9), wi2.getPlanQuantity());
	}

}
