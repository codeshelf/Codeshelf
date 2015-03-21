package com.codeshelf.generators;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.LedSample;
import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.manager.Tenant;
import com.codeshelf.model.OrderStatusEnum;
import com.codeshelf.model.OrderTypeEnum;
import com.codeshelf.model.WiFactory;
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
	
	public List<WorkInstruction> generateCombinations(Tenant tenant, Facility facility, Timestamp assignedTime) {
		ArrayList<WorkInstruction> wiCombos = new ArrayList<>();
		for (WorkInstructionStatusEnum statusEnum : WorkInstructionStatusEnum.values()) {
			for (WorkInstructionTypeEnum typeEnum : WorkInstructionTypeEnum.values()) {
				wiCombos.add(generateWithStatusAndType(tenant, facility, statusEnum, typeEnum, assignedTime));
			}
			
		}
		//housekeeping
		wiCombos.addAll(generateAllHousekeeping(tenant,facility, wiCombos.get(0), wiCombos.get(1)));
		
		return wiCombos;
	}

	public WorkInstruction generateWithNewStatus(Tenant tenant,Facility facility) {
		Timestamp assignedTime = new Timestamp(System.currentTimeMillis()-10000);
		return generateWithStatusAndType(tenant,facility, WorkInstructionStatusEnum.NEW, WorkInstructionTypeEnum.ACTUAL, assignedTime);
	}
	
	public List<WorkInstruction> generateAllHousekeeping(Tenant tenant,Facility facility, WorkInstruction prevWi, WorkInstruction nextWi) {
		ArrayList<WorkInstruction> wiCombos = new ArrayList<>();
		for (WorkInstructionTypeEnum typeEnum : WorkInstructionTypeEnum.getHousekeepingTypeEnums()) {		
			WorkInstruction workInstruction = WiFactory.createHouseKeepingWi(tenant,typeEnum, facility, prevWi, nextWi);
			wiCombos.add(workInstruction);
		}
		return wiCombos;
	}
	
	private WorkInstruction generateWithStatusAndType(Tenant tenant, Facility facility, WorkInstructionStatusEnum statusEnum, WorkInstructionTypeEnum typeEnum, Timestamp assignedTime) {
		Aisle aisle=facility.createAisle("A1", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(tenant,aisle);
		
		UomMaster uomMaster = facility.createUomMaster("UOMID");
		UomMaster.staticGetDao().store(tenant,uomMaster);
		ItemMaster itemMaster = facility.createItemMaster("ITEMID", "ITEMDESCRIPTION", uomMaster);
		ItemMaster.staticGetDao().store(tenant,itemMaster);
		OrderDetail orderDetail = generateValidOrderDetail(tenant,facility, itemMaster, uomMaster);
		OrderDetail.staticGetDao().store(tenant,orderDetail);

		Container container = 	
				new Container("CONTID",
							  facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND),
							  true);
		facility.addContainer(container);
		Container.staticGetDao().store(tenant,container);

		//Che che1 = Che.staticGetDao().findByDomainId(tenant,facility.getNetworks().get(0), "CHE1");
		Che che1 = Che.staticGetDao().findByDomainId(tenant,null, "CHE1");
		
		WorkInstruction workInstruction = WiFactory.createWorkInstruction(tenant,statusEnum, typeEnum, orderDetail, container, che1, aisle, assignedTime);
		
		
		workInstruction.setStarted(  new Timestamp(System.currentTimeMillis()-5000));
		workInstruction.setCompleted(new Timestamp(System.currentTimeMillis()-0000));

		String cmd = LedCmdGroupSerializer.serializeLedCmdString(ImmutableList.of(new LedCmdGroup("0x9999ABCD", (short)1 , (short)1, ImmutableList.of(new LedSample((short)1, ColorEnum.MAGENTA)) )));
		workInstruction.setLedCmdStream(cmd);
		
		return workInstruction;
	}
	
	public WorkInstruction generateValid(Tenant tenant,Facility facility) {
		return generateWithNewStatus(tenant,facility);
	}
	
	//Move to order detail generator
	private OrderDetail generateValidOrderDetail(Tenant tenant,Facility facility, ItemMaster itemMaster, UomMaster uom) {
		OrderGroup orderGroup = new OrderGroup("OG1");
		facility.addOrderGroup(orderGroup);
		OrderGroup.staticGetDao().store(tenant,orderGroup);
		
		OrderHeader orderHeader = new OrderHeader("OH1", OrderTypeEnum.OUTBOUND);
		facility.addOrderHeader(orderHeader);
		
		orderGroup.addOrderHeader(orderHeader);
		OrderHeader.staticGetDao().store(tenant,orderHeader);

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

