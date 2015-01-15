package com.gadgetworks.codeshelf.api.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.gadgetworks.codeshelf.api.BaseResponse;
import com.gadgetworks.codeshelf.api.BaseResponse.UUIDParam;
import com.gadgetworks.codeshelf.api.ErrorResponse;
import com.gadgetworks.codeshelf.edi.ICsvInventoryImporter;
import com.gadgetworks.codeshelf.edi.ICsvOrderImporter;
import com.gadgetworks.codeshelf.edi.InventoryCsvImporter;
import com.gadgetworks.codeshelf.edi.OutboundOrderCsvImporter;
import com.gadgetworks.codeshelf.event.EventProducer;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerUse;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.LocationAlias;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;

@Path("/test")
public class TestingResource {
	private PersistenceService persistence = PersistenceService.getInstance();
	
	@POST
	@Path("/createorders")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createTestOrders(@QueryParam("facilityId") UUIDParam facilityUUID) {
		ErrorResponse errors = new ErrorResponse();
		if (!BaseResponse.isUUIDValid(facilityUUID, "facilityId", errors)){
			return errors.buildResponse();
		}

		try {
			persistence.beginTenantTransaction();
			Facility facility = Facility.DAO.findByPersistentId(facilityUUID.getUUID());
			if (facility == null) {
				errors.addErrorUUIDDoesntExist(facilityUUID.getRawValue(), "facility");;
				return errors.buildResponse();
			}
			importInventory(facility);
			long order1 = new Date().getTime() / 1000;
			long order2 = order1 + 1;
			createOrders(facility, order1, order2);
			persistence.commitTenantTransaction();
			
			System.out.println("Orders created");
			Thread.sleep(6000);
			
			persistence.beginTenantTransaction();
			List<Che> ches = Che.DAO.getAll();
			if (ches == null || ches.isEmpty()){
				errors.addError("Ensure that facility " + facilityUUID.getRawValue() + " has, at least, one che");
				return errors.buildResponse();
			}
			Che che = ches.get(0);
			List<String> containers = new ArrayList<String>();
			containers.add(order1 + "");
			containers.add(order2 + "");
			facility = Facility.DAO.findByPersistentId(facilityUUID.getUUID());
			List<WorkInstruction> instructions = facility.computeWorkInstructions(che, containers);
			System.out.println("*****************Got " + instructions.size() + " instructions");
			persistence.commitTenantTransaction();
			System.out.println("Assigned to CHE");
			
			int i = 0;
			for(WorkInstruction instruction : instructions) {
				persistence.beginTenantTransaction();
				instruction.setActualQuantity(instruction.getPlanQuantity());
				instruction.setCompleted(new Timestamp(System.currentTimeMillis()));
				instruction.setType(WorkInstructionTypeEnum.ACTUAL);
				OrderDetail detail = instruction.getOrderDetail();
				if (i++ % 4 == 0) {
					instruction.setStatus(WorkInstructionStatusEnum.SHORT);
					detail.setStatus(OrderStatusEnum.SHORT);
				} else {
					instruction.setStatus(WorkInstructionStatusEnum.COMPLETE);
					detail.setStatus(OrderStatusEnum.COMPLETE);
				}
				OrderDetail.DAO.store(detail);
				WorkInstruction.DAO.store(instruction);
				Thread.sleep(2000);
				System.out.println("Complete Instruction");
				persistence.commitTenantTransaction();
			}
			return BaseResponse.buildResponse("Test orders created and ran.");
		} catch (Exception e) {
			errors.processException(e);
			return errors.buildResponse();
		} finally {
			persistence.commitTenantTransaction();
		}
	}
	
	private void importInventory(Facility facility) throws Exception{	
		List<LocationAlias> aliases = facility.getLocationAliases();
		if (aliases == null || aliases.isEmpty()) {
			throw new Exception("Please add at least one location alias to the facility");
		}
		String alias = aliases.get(0).getDomainId();

		String csvString = String.format(
				"itemId,locationId,description,quantity,uom,inventoryDate,cmFromLeft\r\n"
				+ "1123,%s,12/16 oz Bowl Lids -PLA Compostable,6,EA,6/25/14 12:00,135\r\n"
				+ "1493,%s,PARK RANGER Doll,2,case,6/25/14 12:00,66\r\n"
				+ "1522,%s,SJJ BPP,1,case,6/25/14 12:00,3,each,6/25/14 12:00,0\r\n"
				+ "1522,%s,SJJ BPP,10,each,6/25/14 12:00,3,each,6/25/14 12:00,0\r\n"
				+ "1122,%s,8 oz Bowl Lids -PLA Compostable,100,each,6/25/14 12:00,0\r\n"
				+ "1523,%s,SJJ BPP,100,each,6/25/14 12:00,0\r\n"
				+ "1124,%s,8 oz Bowls -PLA Compostable,100,each,6/25/14 12:00,0\r\n"
				+ "1555,%s,paper towel,100,each,6/25/14 12:00,0\r\n"
				+ "1556,%s,kit-kat bars,100,each,6/25/14 12:00,0\r\n"
				+ "1557,%s,mars bars,100,each,6/25/14 12:00,0\r\n",
				alias, alias, alias, alias, alias, alias, alias, alias, alias, alias);
		byte[] csvArray = csvString.getBytes();

		ByteArrayInputStream stream = new ByteArrayInputStream(csvArray);
		InputStreamReader reader = new InputStreamReader(stream);

		Timestamp ediProcessTime = new Timestamp(System.currentTimeMillis()); 
		ICsvInventoryImporter importer = new InventoryCsvImporter(new EventProducer(), ItemMaster.DAO, Item.DAO, UomMaster.DAO);
		importer.importSlottedInventoryFromCsvStream(reader, facility, ediProcessTime);		
	}
	
	private void createOrders(Facility facility, long order1, long order2) throws Exception{		
		String csvString2 = String.format(
				"orderGroupId,shipmentId,customerId,preAssignedContainerId,orderId,itemId,description,quantity,uom,orderDate,dueDate,workSequence"
				//order1
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1123,12/16 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1493,PARK RANGER Doll,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1122,8 oz Bowl Lids -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1522,Butterfly Yoyo,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1556,kit-kat bars,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				//order2
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1523,SJJ BPP,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1124,8 oz Bowls -PLA Compostable,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1555,,2,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0"
				+ "\r\nTestGroup,USF314,COSTCO,%d,%d,1557,mars bars,1,each,2012-09-26 11:31:01,2012-09-26 11:31:03,0",
				order1, order1, order1, order1, order1, order1, order1, order1, order2, order2, order2, order2, order2, order2, order2, order2, order2, order2, order2, order2);

		byte[] csvArray2 = csvString2.getBytes();

		ByteArrayInputStream stream2 = new ByteArrayInputStream(csvArray2);
		InputStreamReader reader2 = new InputStreamReader(stream2);

		Timestamp ediProcessTime2 = new Timestamp(System.currentTimeMillis());
		ICsvOrderImporter importer2 = new OutboundOrderCsvImporter(new EventProducer(), OrderGroup.DAO, OrderHeader.DAO, OrderDetail.DAO, Container.DAO, ContainerUse.DAO, ItemMaster.DAO, UomMaster.DAO);
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
	}
}