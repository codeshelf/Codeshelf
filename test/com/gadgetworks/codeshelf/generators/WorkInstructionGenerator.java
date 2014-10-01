package com.gadgetworks.codeshelf.generators;

import java.sql.Timestamp;

import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;

public class WorkInstructionGenerator {

	
	public WorkInstruction generateValid(Facility facility) {
		WorkInstruction workInstruction = new WorkInstruction();
		workInstruction.setParent(generateValidOrderDetail(facility));
		workInstruction.setDomainId("WIDOMAINID");
		workInstruction.setContainer(facility.createContainer("C1"));
		workInstruction.setItemMaster(facility.createItemMaster( "ITEMID", facility.createUomMaster("UOMID")));
		workInstruction.setLocationId("LOCID");
		workInstruction.setLocation(facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint()));
		workInstruction.setPickerId("Picker");
		workInstruction.setType(WorkInstructionTypeEnum.ACTUAL);
		workInstruction.setStatus(WorkInstructionStatusEnum.COMPLETE);
		workInstruction.setPlanQuantity(2);
		workInstruction.setActualQuantity(2);
		workInstruction.setAssigned( new Timestamp(System.currentTimeMillis()-10000));
		workInstruction.setStarted(  new Timestamp(System.currentTimeMillis()-5000));
		workInstruction.setCompleted(new Timestamp(System.currentTimeMillis()-0000));
		return workInstruction;
	}
	
	//Move to order detail generator
	private OrderDetail generateValidOrderDetail(Facility facility) {
		OrderHeader orderHeader = new OrderHeader(facility, "OH1");
		orderHeader.setOrderGroup(new OrderGroup(facility, "OG1"));
		OrderDetail detail = new OrderDetail(orderHeader, "OD1");
		detail.setQuantities(5);
		return detail;
		
	}
	
}
