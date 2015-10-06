package com.codeshelf.api.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.BaseResponse.UUIDParam;
import com.codeshelf.api.ErrorResponse;
import com.codeshelf.edi.ICsvInventoryImporter;
import com.codeshelf.edi.ICsvOrderImporter;
import com.codeshelf.edi.InventoryCsvImporter;
import com.codeshelf.edi.OutboundOrderPrefetchCsvImporter;
import com.codeshelf.event.EventProducer;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.ExtensionPoint;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.LocationAlias;
import com.codeshelf.model.domain.WorkInstruction;
import com.codeshelf.model.domain.WorkPackage.WorkList;
import com.codeshelf.persistence.TenantPersistenceService;
import com.codeshelf.service.ExtensionPointType;
import com.codeshelf.service.WorkBehavior;
import com.google.inject.Inject;

@Path("/test")
@RequiresAuthentication
public class TestingResource {
	private TenantPersistenceService persistence = TenantPersistenceService.getInstance();
	private WorkBehavior workService;

	@Inject
	public TestingResource(WorkBehavior workService) {
		this.workService = workService;
	}

	@POST
	@Path("/createorders")
	@RequiresPermissions("order:import")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createTestOrders(@QueryParam("facilityId") UUIDParam facilityUUID) {
		ErrorResponse errors = new ErrorResponse();
		try {
			Facility facility = Facility.staticGetDao().findByPersistentId(facilityUUID.getValue());
			if (facility == null) {
				errors.addErrorUUIDDoesntExist(facilityUUID.getRawValue(), "facility");;
				return errors.buildResponse();
			}
			importInventory(facility);
			long order1 = new Date().getTime() / 1000;
			long order2 = order1 + 1;
			createOrders(facility, order1, order2);
			persistence.commitTransaction();

			Thread.sleep(6000);

			persistence.beginTransaction();
			List<Che> ches = Che.staticGetDao().getAll();
			if (ches == null || ches.isEmpty()){
				errors.addError("Ensure that facility " + facilityUUID.getRawValue() + " has, at least, one che");
				return errors.buildResponse();
			}
			Che che = ches.get(0);
			Map<String,String> containers = new HashMap<String,String>();
			containers.put("1",order1 + "");
			containers.put("2",order2 + "");
			facility = Facility.staticGetDao().findByPersistentId(facilityUUID.getValue());
			WorkList workList = workService.computeWorkInstructions(che, containers);
			List<WorkInstruction> instructions = workList.getInstructions();
			persistence.commitTransaction();

			int i = 0;
			for(WorkInstruction instruction : instructions) {
				persistence.beginTransaction();
				instruction.setType(WorkInstructionTypeEnum.ACTUAL);
				if (i++ % 4 == 0) {
					instruction.setShortState("SIMULATED", instruction.getPlanQuantity() - 1);
				} else {
					instruction.setCompleteState("SIMULATED", instruction.getPlanQuantity());
				}
				workService.completeWorkInstruction(che.getPersistentId(), instruction);
				Thread.sleep(2000);
				persistence.commitTransaction();
			}
			return BaseResponse.buildResponse("Test orders created and ran.");
		} catch (Exception e) {
			return errors.processException(e);
		} finally {
			persistence.commitTransaction();
		}
	}

	private void importInventory(Facility facility) throws Exception{
		List<LocationAlias> aliases = LocationAlias.staticGetDao().findByParent(facility);
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
		ICsvInventoryImporter importer = new InventoryCsvImporter(new EventProducer());
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
		ICsvOrderImporter importer2 = new OutboundOrderPrefetchCsvImporter(new EventProducer());
		importer2.importOrdersFromCsvStream(reader2, facility, ediProcessTime2);
	}
	
	@POST
	@Path("/createextensionpoints")
	@RequiresPermissions("companion:create_extension")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createTestExtensionPoints(@QueryParam("facilityId") UUIDParam facilityUUID) {
		ErrorResponse errors = new ErrorResponse();
		try {
			Facility facility = Facility.staticGetDao().findByPersistentId(facilityUUID.getValue());
			if (facility == null) {
				errors.addErrorUUIDDoesntExist(facilityUUID.getRawValue(), "facility");;
				return errors.buildResponse();
			}
			String time = System.currentTimeMillis() + "";
			ExtensionPoint extension = new ExtensionPoint();
			extension.setParent(facility);
			extension.setDomainId("EP" + time);
			extension.setScript(time + " " + time + " " + time + " " + time + " " + time + " " + time + " " + time + " " + time + " " + time + " " + time + " " + time + " ");
			extension.setActive(new Random().nextInt() % 2 == 0);
			switch (new Random().nextInt() % 3) {
				case 0:
					extension.setType(ExtensionPointType.OrderImportBeanTransformation);
					break;
				case 1:
					extension.setType(ExtensionPointType.OrderImportHeaderTransformation);
					break;
				case 2:
					extension.setType(ExtensionPointType.OrderImportLineTransformation);
					break;
			}
			ExtensionPoint.staticGetDao().store(extension);			
			return BaseResponse.buildResponse("Extension Point Created");
		} catch (Exception e) {
			return errors.processException(e);
		}
	}
}
