package com.gadgetworks.codeshelf.model.domain;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.edi.EdiTestABC;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.service.WorkService;
import com.gadgetworks.codeshelf.validation.BatchResult;
import com.gadgetworks.codeshelf.validation.ErrorCode;
import com.gadgetworks.codeshelf.validation.MethodArgumentException;

public class LineScanTest extends EdiTestABC {
	private final static Logger LOGGER=LoggerFactory.getLogger(ProductivityReportingTest.class);
	private WorkService mService = new WorkService().start();
	private ICsvOrderImporter importer;

	private UUID				facilityId;

	@Before
	public void initTest() {
		this.getPersistenceService().beginTenantTransaction();
		importer = createOrderImporter();
		facilityId = createFacility().getPersistentId();
		this.getPersistenceService().commitTenantTransaction();
	}

	@Test
	public void testGetWorkInstruction() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		Che che = Che.DAO.getAll().get(0);
		
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,30"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10";
		importCsvString(facility, csvString);

		OrderHeader header = facility.getOrderHeaders().get(0);
		OrderDetail detail = header.getOrderDetails().get(0);

		List<WorkInstruction> instructions = mService.getWorkInstructionsForOrderDetail(che, detail.getDomainId());
		WorkInstruction instruction = instructions.get(0);
		Assert.assertEquals(instruction.getDescription(), detail.getDescription());
		Assert.assertEquals(instruction.getItemId(), detail.getItemMasterId());
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.NEW);
		
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testGetWorkInstructionDuplicate() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		Che che = Che.DAO.getAll().get(0);
		
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,30"
				+ "\r\n11,11,10.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10";
		importCsvString(facility, csvString);
		try {
			mService.getWorkInstructionsForOrderDetail(che, "10.1");
		} catch (MethodArgumentException e) {
			Assert.assertEquals("Expected a NotUnique exception", e.getErrorCode(), ErrorCode.FIELD_REFERENCE_NOT_UNIQUE);
		}
	}

	@Test
	public void testGetWorkInstructionCompletedInstruction() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		Che che = Che.DAO.getAll().get(0);
		
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,30"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10";
		importCsvString(facility, csvString);

		List<WorkInstruction> instructions = mService.getWorkInstructionsForOrderDetail(che, "11.1");
		WorkInstruction instruction = instructions.get(0);
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.NEW);
		
		instruction.setActualQuantity(instruction.getPlanQuantity());
		instruction.setCompleted(new Timestamp(System.currentTimeMillis()));
		instruction.setType(WorkInstructionTypeEnum.ACTUAL);
		instruction.setStatus(WorkInstructionStatusEnum.COMPLETE);
		mService.completeWorkInstruction(che.getPersistentId(), instruction);

		instructions = mService.getWorkInstructionsForOrderDetail(che, "11.1");
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.COMPLETE);
		this.getPersistenceService().commitTenantTransaction();
	}
	
	@Test
	public void testGetWorkInstructionBadDetailId() throws Exception {
		this.getPersistenceService().beginTenantTransaction();
		Facility facility = Facility.DAO.findByPersistentId(facilityId);
		Che che = Che.DAO.getAll().get(0);
		
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,30"
				+ "\r\n11,11,10.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10";
		importCsvString(facility, csvString);
		try {
			mService.getWorkInstructionsForOrderDetail(che, "xxx");
		} catch (MethodArgumentException e) {
			Assert.assertEquals("Expected a NotUnique exception", e.getErrorCode(), ErrorCode.FIELD_REFERENCE_NOT_FOUND);
		}
	}

	
	private BatchResult<Object> importCsvString(Facility facility, String csvString) throws IOException {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		BatchResult<Object> results = importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		return results;
	}
}
