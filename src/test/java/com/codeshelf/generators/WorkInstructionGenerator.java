package com.codeshelf.generators;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.codeshelf.device.LedCmdGroup;
import com.codeshelf.device.LedCmdGroupSerializer;
import com.codeshelf.device.LedSample;
import com.codeshelf.edi.Generator;
import com.codeshelf.edi.InventoryGenerator;
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
import com.codeshelf.model.domain.Item;
import com.codeshelf.model.domain.OrderDetail;
import com.codeshelf.model.domain.OrderGroup;
import com.codeshelf.model.domain.OrderHeader;
import com.codeshelf.model.domain.Point;
import com.codeshelf.model.domain.WorkInstruction;
import com.google.common.collect.ImmutableList;

public class WorkInstructionGenerator {

	private InventoryGenerator inventoryGenerator = new InventoryGenerator(null);
	private Generator generator = new Generator();
	
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
	
	public WorkInstruction generateWithNewStatus(OrderDetail orderDetail, Che che) {
		DateTime assignedTime = new DateTime().minus(10000);
				
		WorkInstruction wi = generateWithStatusAndType(orderDetail, WorkInstructionStatusEnum.NEW, WorkInstructionTypeEnum.ACTUAL, new Timestamp(assignedTime.getMillis()));
		wi.setAssignedChe(che);
		return wi;
	}

	public List<WorkInstruction> generateAllHousekeeping(Facility facility, WorkInstruction prevWi, WorkInstruction nextWi) {
		ArrayList<WorkInstruction> wiCombos = new ArrayList<>();
		for (WorkInstructionTypeEnum typeEnum : WorkInstructionTypeEnum.getHousekeepingTypeEnums()) {		
			WorkInstruction workInstruction = WiFactory.createHouseKeepingWi(typeEnum, facility, prevWi, nextWi);
			wiCombos.add(workInstruction);
		}
		return wiCombos;
	}
	
	private WorkInstruction generateWithStatusAndType(OrderDetail orderDetail, WorkInstructionStatusEnum statusEnum, WorkInstructionTypeEnum typeEnum, Timestamp assignedTime) {
		Facility facility = orderDetail.getParent().getFacility();
		Aisle aisle=facility.createAisle("A99", Point.getZeroPoint(), Point.getZeroPoint());
		Aisle.staticGetDao().store(aisle);
		Container container = 	
				new Container("CONTID",
							  facility.getContainerKind(ContainerKind.DEFAULT_CONTAINER_KIND),
							  true);
		container.setParent(facility);
		Container.staticGetDao().store(container);


		//Che che1 = Che.staticGetDao().findByDomainId(facility.getNetworks().get(0), "CHE1");
		Che che1 = Che.staticGetDao().findByDomainId(null, "CHE1");
		
		WorkInstruction workInstruction = WiFactory.createWorkInstruction(statusEnum, typeEnum, WiPurpose.WiPurposeOutboundPick, orderDetail, che1, assignedTime, container, aisle);
		workInstruction.setStarted(  new Timestamp(System.currentTimeMillis()-5000));
		workInstruction.setCompleted(new Timestamp(System.currentTimeMillis()-0000));

		String cmd = LedCmdGroupSerializer.serializeLedCmdString(ImmutableList.of(new LedCmdGroup("0x9999ABCD", (short)1 , (short)1, ImmutableList.of(new LedSample((short)1, ColorEnum.MAGENTA)) )));
		workInstruction.setLedCmdStream(cmd);
		
		return workInstruction;
		
	}
	
	private WorkInstruction generateWithStatusAndType(Facility facility, WorkInstructionStatusEnum statusEnum, WorkInstructionTypeEnum typeEnum, Timestamp assignedTime) {
		OrderGroup orderGroup = OrderGroup.staticGetDao().findByDomainId(facility, "OG1");
		if (orderGroup == null){
			orderGroup = new OrderGroup("OG1");
			facility.addOrderGroup(orderGroup);
			OrderGroup.staticGetDao().store(orderGroup);
		}
		
		OrderHeader orderHeader = generateValidOrderHeader(facility);
		orderGroup.addOrderHeader(orderHeader);
		OrderHeader.staticGetDao().store(orderHeader);
		
		Item item = inventoryGenerator.generateItem(facility);
		OrderDetail orderDetail = generateValidOrderDetail(orderHeader, item);
		return generateWithStatusAndType(orderDetail, statusEnum, typeEnum, assignedTime);
	}
	
	public WorkInstruction generateValid(Facility facility) {
		return generateWithNewStatus(facility);
	}
	
	public OrderHeader generateValidOrderHeader(Facility facility) {
		OrderHeader orderHeader = new OrderHeader(facility, generator.generateString(), OrderTypeEnum.OUTBOUND);
		OrderHeader.staticGetDao().store(orderHeader);
		return orderHeader;
	}
	
	//Move to order detail generator
	public OrderDetail generateValidOrderDetail(OrderHeader orderHeader, Item item) {

		OrderDetail detail = new OrderDetail(generator.generateString(), item.getParent(), generator.generateInt(1, 5));
		detail.setStatus(OrderStatusEnum.RELEASED);
		detail.setUomMaster(item.getUomMaster());
		orderHeader.addOrderDetail(detail);
		OrderDetail.staticGetDao().store(detail);
		return detail;	
	}	
}

