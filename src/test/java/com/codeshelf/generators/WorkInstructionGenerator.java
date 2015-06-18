package com.codeshelf.generators;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.LedSample;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.WiFactory;
import com.codeshelf.model.WiFactory.WiPurpose;
import com.codeshelf.model.WorkInstructionStatusEnum;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.domain.Aisle;
import com.codeshelf.model.domain.Che;
import com.codeshelf.model.domain.Container;
import com.codeshelf.model.domain.ContainerKind;
import com.codeshelf.model.domain.Facility;
import com.codeshelf.model.domain.ItemMaster;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.UomMaster;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableList;

public class WorkInstructionGenerator {
	
	public List<WorkInstruction> generateCombinations(Facility facility, Timestamp assignedTime) {
		ArrayList<WorkInstruction> wiCombos = new ArrayList<>();
		for (WorkInstructionStatusEnum statusEnum : WorkInstructionStatusEnum.values()) {
			for (WorkInstructionTypeEnum typeEnum : WorkInstructionTypeEnum.values()) {
				wiCombos.add(generateWithStatusAndType(facility, statusEnum, typeEnum, assignedTime));
			}
			
		}
		//housekeeping
		wiCombos.addAll(generateAllHousekeeping(facility, wiCombos.get(0), wiCombos.get(1)));
		
		return wiCombos;
	}

	public WorkInstruction generateWithNewStatus(Facility facility) {
		Timestamp assignedTime = new Timestamp(System.currentTimeMillis()-10000);
		return generateWithStatusAndType(facility, WorkInstructionStatusEnum.NEW, WorkInstructionTypeEnum.ACTUAL, assignedTime);
	}
	
	public List<WorkInstruction> generateAllHousekeeping(Facility facility, WorkInstruction prevWi, WorkInstruction nextWi) {
		ArrayList<WorkInstruction> wiCombos = new ArrayList<>();
		for (WorkInstructionTypeEnum typeEnum : WorkInstructionTypeEnum.getHousekeepingTypeEnums()) {		
			WorkInstruction workInstruction = WiFactory.createHouseKeepingWi(typeEnum, facility, prevWi, nextWi);
			wiCombos.add(workInstruction);
		}
		return wiCombos;
	}
	
	private WorkInstruction generateWithStatusAndType(Facility facility, WorkInstructionStatusEnum statusEnum, WorkInstructionTypeEnum typeEnum, Timestamp assignedTime) {
		Aisle aisle=facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisle);
		
		UomMaster uomMaster = facility.createUomMaster("UOMID");
		UomMaster.staticGetDao().store(uomMaster);
		ItemMaster itemMaster = facility.createItemMaster("ITEMID", "ITEMDESCRIPTION", uomMaster);
		ItemMaster.staticGetDao().store(itemMaster);
		OrderDetail orderDetail = generateValidOrderDetail(facility, itemMaster, uomMaster);
		OrderDetail.staticGetDao().store(orderDetail);

		Container container = 	
				new Container("CONTID",
							  facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND),
							  true);
		container.setParent(facility);
		Container.staticGetDao().store(container);

		//Che che1 = Che.staticGetDao().findByDomainId(facility.getNetworks().get(0), "CHE1");
		Che che1 = Che.staticGetDao().findByDomainId(null, "CHE1");
		
		WorkInstruction workInstruction = WiFactory.createWorkInstruction(statusEnum, typeEnum, orderDetail, container, che1, aisle, assignedTime, WiPurpose.WiPurposeOutboundPick);
		
		
		workInstruction.setStarted(  new Timestamp(System.currentTimeMillis()-5000));
		workInstruction.setCompleted(new Timestamp(System.currentTimeMillis()-0000));

		String cmd = LedCmdGroupSerializer.serializeLedCmdString(ImmutableList.of(new LedCmdGroup("0x9999ABCD", (short)1 , (short)1, ImmutableList.of(new LedSample((short)1, ColorEnum.MAGENTA)) )));
		workInstruction.setLedCmdStream(cmd);
		
		return workInstruction;
	}
	
	public WorkInstruction generateValid(Facility facility) {
		return generateWithNewStatus(facility);
	}
	
	//Move to order detail generator
	private OrderDetail generateValidOrderDetail(Facility facility, ItemMaster itemMaster, UomMaster uom) {
		OrderGroup orderGroup = OrderGroup.staticGetDao().findByDomainId(facility, "OG1");
		if (orderGroup == null){
			orderGroup = new OrderGroup("OG1");
			facility.addOrderGroup(orderGroup);
			OrderGroup.staticGetDao().store(orderGroup);
		}
		
		OrderHeader orderHeader = new OrderHeader("OH1", OrderTypeEnum.OUTBOUND);
		orderHeader.setParent(facility);
		
		orderGroup.addOrderHeader(orderHeader);
		OrderHeader.staticGetDao().store(orderHeader);

		OrderDetail detail = new OrderDetail("OD1", true);
		detail.setStatus(OrderStatusEnum.RELEASED);
		detail.setUpdated(new Timestamp(System.currentTimeMillis()));
		detail.setQuantities(5);
		detail.setItemMaster(itemMaster);
		detail.setUomMaster(uom);
		orderHeader.addOrderDetail(detail);
		return detail;	
	}
	
}

