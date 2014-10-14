package com.gadgetworks.codeshelf.generators;

import java.sql.Timestamp;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ItemMaster;
import com.gadgetworks.codeshelf.model.domain.OrderDetail;
import com.gadgetworks.codeshelf.model.domain.OrderGroup;
import com.gadgetworks.codeshelf.model.domain.OrderHeader;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.UomMaster;
import com.gadgetworks.codeshelf.model.domain.WorkInstruction;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.collect.ImmutableList;

public class WorkInstructionGenerator {

	
	public WorkInstruction generateWithNewStatus(Facility facility) {
		WorkInstruction workInstruction = new WorkInstruction();
		workInstruction.setOrderDetail(generateValidOrderDetail(facility));
		workInstruction.setDomainId("WIDOMAINID");
		workInstruction.setDescription("A DESCRIPTION");
		
		Container container = facility.createContainer("C1");
		workInstruction.setContainer(container);
		
		UomMaster uomMaster = facility.createUomMaster("UOMID");
		
		ItemMaster itemMaster = facility.createItemMaster("ITEMID", uomMaster);
		workInstruction.setItemMaster(itemMaster);
		workInstruction.setLocationId("LOCID");
		
		Aisle aisle=facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		
		workInstruction.setLocation(aisle);
		workInstruction.setPickerId("Picker");
		workInstruction.setType(WorkInstructionTypeEnum.ACTUAL);
		workInstruction.setStatus(WorkInstructionStatusEnum.NEW);
		workInstruction.setPickInstruction("Pick it");
		workInstruction.setPlanQuantity(2);
		workInstruction.setPlanMinQuantity(2);
		workInstruction.setPlanMaxQuantity(2);
		workInstruction.setActualQuantity(2);
		workInstruction.setAssignedChe(new Che("CHE1"));
		workInstruction.setAssigned( new Timestamp(System.currentTimeMillis()-10000));
		workInstruction.setStarted(  new Timestamp(System.currentTimeMillis()-5000));
		workInstruction.setCompleted(new Timestamp(System.currentTimeMillis()-0000));
		String cmd = LedCmdGroupSerializer.serializeLedCmdString(ImmutableList.of(new LedCmdGroup("0x9999ABCD", (short)1 , (short)1, ImmutableList.of(new LedSample((short)1, ColorEnum.MAGENTA)) )));
		workInstruction.setLedCmdStream(cmd);
		
		facility.addWorkInstruction(workInstruction);

		return workInstruction;
	}
	
	public WorkInstruction generateValid(Facility facility) {
		return generateWithNewStatus(facility);
	}
	
	//Move to order detail generator
	private OrderDetail generateValidOrderDetail(Facility facility) {
		OrderHeader orderHeader = new OrderHeader(facility, "OH1");
		orderHeader.setOrderGroup(new OrderGroup(facility, "OG1"));
		OrderDetail detail = new OrderDetail(orderHeader, "OD1");
		detail.setQuantities(5);
		detail.setUomMaster(new UomMaster(facility, "newUOM"));
		return detail;
		
	}
	
}

