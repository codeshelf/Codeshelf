package com.codeshelf.model.domain;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.behavior.BehaviorFactory;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.testframework.ServerTest;
import com.codeshelf.util.ConverterProvider;
import com.codeshelf.validation.ErrorCode;
import com.codeshelf.validation.MethodArgumentException;
import com.codeshelf.ws.protocol.request.ComputeDetailWorkRequest;
import com.codeshelf.ws.protocol.response.GetOrderDetailWorkResponse;
import com.codeshelf.ws.protocol.response.ResponseABC;
import com.codeshelf.ws.protocol.response.ResponseStatus;
import com.codeshelf.ws.server.ServerMessageProcessor;
import com.codeshelf.ws.server.WebSocketConnection;

public class LineScanTest extends ServerTest {
	@SuppressWarnings("unused")
	private final static Logger LOGGER=LoggerFactory.getLogger(LineScanTest.class);
	private ServerMessageProcessor	processor;

	@Before
	public void initTest() throws IOException {
		beginTransaction();
		Facility facility = createFacility(); 
		BehaviorFactory serviceFactory = new BehaviorFactory(workService, null, null, null, null, null, null, null, null, null);
		commitTransaction();

		processor = new ServerMessageProcessor(serviceFactory, new ConverterProvider().get(), this.webSocketManagerService);
		
		String csvString = "orderId,preassignedContainerId,orderDetailId,itemId,description,quantity,uom,gtin,type,locationId,cmFromLeft"
				+ "\r\n10,10,10.1,SKU0001,16 OZ. PAPER BOWLS,3,CS,,pick,D34,30"
				+ "\r\n11,11,11.1,SKU0003,Spoon 6in.,1,CS,,pick,D21,"
				+ "\r\n11,11,11.2,SKU0004,9 Three Compartment Unbleached Clamshel,2,EA,,pick,D35,10"
				+ "\r\n11,11,10.1,SKU0005,Mars Bars,20,EA,,pick,D36,10";
		beginTransaction();
		importOrdersData(facility, csvString);
		commitTransaction();
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

		ResponseABC response = processor.handleRequest(this.getMockWsConnection(), request);
		Assert.assertTrue(response instanceof GetOrderDetailWorkResponse);
		Assert.assertEquals(ResponseStatus.Success, response.getStatus());
		
		List<WorkInstruction> instructions = ((GetOrderDetailWorkResponse)response).getWorkInstructions();
		WorkInstruction instruction = instructions.get(0);
		Assert.assertEquals(instruction.getDescription(), "Spoon 6in.");
		Assert.assertEquals(instruction.getItemId(), "SKU0003");
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.NEW);
		
		this.getTenantPersistenceService().commitTransaction();
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
		beginTransaction();
		Che che = Che.staticGetDao().getAll().get(0);

		ComputeDetailWorkRequest request = new ComputeDetailWorkRequest(che.getPersistentId().toString(), "11.1");
		
		GetOrderDetailWorkResponse response = (GetOrderDetailWorkResponse)processor.handleRequest(this.getMockWsConnection(), request);
		
		List<WorkInstruction> instructions = response.getWorkInstructions();
		WorkInstruction instruction = instructions.get(0);
		Assert.assertEquals(instruction.getStatus(), WorkInstructionStatusEnum.NEW);
		instruction.setType(WorkInstructionTypeEnum.ACTUAL);
		instruction.setCompleteState("test", instruction.getPlanQuantity());
		
		workService.completeWorkInstruction(che.getPersistentId(), instruction);

		response = workService.getWorkInstructionsForOrderDetail(che, "11.1"); 
		instructions = response.getWorkInstructions();
		Assert.assertEquals(WorkInstructionStatusEnum.COMPLETE, instruction.getStatus());
		commitTransaction();
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
}
