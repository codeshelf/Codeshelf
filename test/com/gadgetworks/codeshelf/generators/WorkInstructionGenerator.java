package com.gadgetworks.codeshelf.generators;

import java.sql.Timestamp;
import java.util.UUID;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.OrderStatusEnum;
import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionStatusEnum;
import com.gadgetworks.codeshelf.model.WorkInstructionTypeEnum;
import com.gadgetworks.codeshelf.model.domain.Aisle;
import com.gadgetworks.codeshelf.model.domain.Che;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Container;
import com.gadgetworks.codeshelf.model.domain.ContainerKind;
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
	private static long sequence = 0;
	
	public WorkInstruction generateWithNewStatus(Facility facility) {
		UomMaster uomMaster = facility.createUomMaster("UOMID");
		ItemMaster itemMaster = facility.createItemMaster("ITEMID", uomMaster);

		OrderDetail orderDetail = generateValidOrderDetail(facility, itemMaster, uomMaster);
		WorkInstruction workInstruction = new WorkInstruction();
		workInstruction.setPersistentId(UUID.randomUUID());
		workInstruction.setOrderDetail(orderDetail);
		workInstruction.setDomainId("WIDOMAINID" + sequence++);
		workInstruction.setDescription("A DESCRIPTION");
		
		Container container = 	
				new Container("CONTID",
							  facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND),
							  true);
		facility.addContainer(container);

		workInstruction.setContainer(container);
		
		
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
		
		CodeshelfNetwork network = new CodeshelfNetwork();
		network.setDomainId("TEST");
		facility.addNetwork(network);
		
		Che che1 = new Che("CHE1");
		network.addChe(che1);
		
		che1.addWorkInstruction(workInstruction);
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
	private OrderDetail generateValidOrderDetail(Facility facility, ItemMaster itemMaster, UomMaster uom) {
		OrderGroup orderGroup = new OrderGroup("OG1");
		facility.addOrderGroup(orderGroup);
		
		OrderHeader orderHeader = new OrderHeader("OH1", OrderTypeEnum.OUTBOUND);
		facility.addOrderHeader(orderHeader);
		
		orderGroup.addOrderHeader(orderHeader);

		OrderDetail detail = new OrderDetail("OD1", true);
		detail.setStatus(OrderStatusEnum.CREATED);
		detail.setUpdated(new Timestamp(System.currentTimeMillis()));
		detail.setQuantities(5);
		detail.setItemMaster(itemMaster);
		detail.setUomMaster(uom);
		orderHeader.addOrderDetail(detail);
		return detail;
		
	}
	
}

