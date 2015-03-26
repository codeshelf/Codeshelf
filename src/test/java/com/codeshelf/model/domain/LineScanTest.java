package com.codeshelf.model.domain;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.service.ServiceFactory;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.validation.BatchResult;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.MethodArgumentException;
import com.codeshelf.ws.jetty.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.jetty.protocol.response.GetOrderDetailWorkResponse;
import com.codeshelf.ws.jetty.protocol.response.ResponseABC;
import com.codeshelf.ws.jetty.protocol.response.ResponseStatus;
import com.codeshelf.ws.jetty.server.ServerMessageProcessor;
import com.codeshelf.ws.jetty.server.WebSocketConnection;

public class LineScanTest extends ServerTest {
	@SuppressWarnings("unused")
	private final static Logger LOGGER=LoggerFactory.getLogger(ProductivityReportingTest.class);
	private ICsvOrderImporter importer;
	private ServerMessageProcessor	processor;

	@Before
	public void initTest() throws IOException {
		this.getTenantPersistenceService().beginTransaction();
		importer = createOrderImporter();
		Facility facility = createFacility(); 
		ServiceFactory serviceFactory = new ServiceFactory(workService, null, null, null, null, null);
		//processor = new ServerMessageProcessor(Mockito.mock(ServiceFactory.class), new ConverterProvider().get());
		processor = new ServerMessageProcessor(serviceFactory, new ConverterProvider().get(), this.webSocketManagerService);
		
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,upc,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,30"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10"
				+ "\r\n11,11,10.1,SKU0005,Mars Bars,20,EA,,pick,D36,10";
		importCsvString(facility, csvString);

		this.getTenantPersistenceService().commitTransaction();
	}

	@Test
	public void testGetWorkInstructionDirect() throws Exception {
		this.getTenantPersistenceService().beginTransaction();
		Che che = Che.staticGetDao().getAll().get(0);

		GetOrderDetailWorkResponse response = workService.getWorkInstructionsForOrderDetail(che, "11.1");
		List<WorkInstruction> instructions = response.getWorkInstructions();
		WorkInstruction instruction = instructions.get(0);
		Assert.assertEquals(instruction.getDescription(), "Spoon 6in.");
		Assert.assertEquals(instruction.getItemId(), "SKU0003");
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.NEW);
		
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testGetWorkInstruction() {
		this.getTenantPersistenceService().beginTransaction();
		Che che = Che.staticGetDao().getAll().get(0);
		
		ComputeDetailWorkRequest request = new ComputeDetailWorkRequest(che.getPersistentId().toString(), "11.1");
		this.getTenantPersistenceService().commitTransaction();

		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), request);
		Assert.assertTrue(response instanceof GetOrderDetailWorkResponse);
		Assert.assertEquals(ResponseStatus.Success, response.getStatus());
		
		List<WorkInstruction> instructions = ((GetOrderDetailWorkResponse)response).getWorkInstructions();
		WorkInstruction instruction = instructions.get(0);
		Assert.assertEquals(instruction.getDescription(), "Spoon 6in.");
		Assert.assertEquals(instruction.getItemId(), "SKU0003");
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.NEW);
		
	}

	
	@Test
	public void testGetWorkInstructionDuplicate() throws Exception {
		this.getTenantPersistenceService().beginTransaction();
		Che che = Che.staticGetDao().getAll().get(0);
		try {
			ComputeDetailWorkRequest request = new ComputeDetailWorkRequest(che.getPersistentId().toString(), "10.1");
			processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);
			// Assert.fail("Failed to trigger the expected NonUnique exception");
		} catch (MethodArgumentException e) {
			Assert.assertEquals("Expected a NotUnique exception", e.getErrorCode(), ErrorCode.FIELD_REFERENCE_NOT_UNIQUE);
		} finally {
			this.getTenantPersistenceService().commitTransaction();
		}
	}

	@Test
	public void testGetWorkInstructionCompletedInstruction() throws Exception {
		this.getTenantPersistenceService().beginTransaction();
		Che che = Che.staticGetDao().getAll().get(0);

		ComputeDetailWorkRequest request = new ComputeDetailWorkRequest(che.getPersistentId().toString(), "11.1");
		this.getTenantPersistenceService().commitTransaction();
		
		
		GetOrderDetailWorkResponse response = (GetOrderDetailWorkResponse)processor.handleRequest(this.getMockWsConnection(), request);
		
		this.getTenantPersistenceService().beginTransaction();		
		che = Che.staticGetDao().reload(che);
		List<WorkInstruction> instructions = response.getWorkInstructions();
		WorkInstruction instruction = instructions.get(0);
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.NEW);
		
		instruction.setActualQuantity(instruction.getPlanQuantity());
		instruction.setCompleted(new Timestamp(System.currentTimeMillis()));
		instruction.setType(WorkInstructionTypeEnum.ACTUAL);
		instruction.setStatus(WorkInstructionStatusEnum.COMPLETE);
		
		workService.completeWorkInstruction(che.getPersistentId(), instruction);

		response = workService.getWorkInstructionsForOrderDetail(che, "11.1"); 
		instructions = response.getWorkInstructions();
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, instruction.getStatus());
		this.getTenantPersistenceService().commitTransaction();
	}
	
	@Test
	public void testGetWorkInstructionBadDetailId() throws Exception {
		this.getTenantPersistenceService().beginTransaction();
		Che che = Che.staticGetDao().getAll().get(0);
		
		try {
			ComputeDetailWorkRequest request = new ComputeDetailWorkRequest(che.getPersistentId().toString(), "xxx");
			processor.handleRequest(Mockito.mock(WebSocketConnection.class), request);
			// Assert.fail("Failed to trigger the expected NotFound exception");
		} catch (MethodArgumentException e) {
			Assert.assertEquals("Expected a NotUnique exception", e.getErrorCode(), ErrorCode.FIELD_REFERENCE_NOT_FOUND);
		}
		this.getTenantPersistenceService().commitTransaction();
	}
	
	private BatchResult<Object> importCsvString(Facility facility, String csvString) throws IOException {
		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis());
		BatchResult<Object> results = importer.importOrdersFromCsvStream(new StringReader(csvString), facility, ediProcessTime);
		return results;
	}
}
