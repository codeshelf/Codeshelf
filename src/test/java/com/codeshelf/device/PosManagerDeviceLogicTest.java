package com.codeshelf.device;

import static org.mockito.Mockito.mock;

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.codeshelf.device.PosConInstrGroupSerializer.PosConCmdGroup.Brightness;
import com.codeshelf.device.PosConInstrGroupSerializer.PosConCmdGroup.Frequency;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.flyweight.controller.IRadioController;
import com.codeshelf.model.domain.DomainTestABC;

public class PosManagerDeviceLogicTest extends DomainTestABC {
	private PosManagerDeviceLogic mPosConControllerLogic;
	
	@Before
	public void initTest(){
		IRadioController radioController = mock(IRadioController.class);
		mPosConControllerLogic = new PosManagerDeviceLogic(UUID.randomUUID(), new NetGuid("0xABC"), mock(CsDeviceManager.class), radioController);
	}
	
	@Test
	public void postCommands() {
		NetGuid device1Guid = new NetGuid("0x00000001");
		NetGuid device2Guid = new NetGuid("0x00000002");
		//Use commands sent to controller's own device to take a deeper look at PosCon displays
		NetGuid ownGuid = mPosConControllerLogic.getGuid();
		
		//SEND INSTRUCTIONS
		mPosConControllerLogic.addPosConInstrFor(device1Guid, null, (byte)2, (byte)50, (byte)40, (byte)60, Frequency.SOLID.toByte(), Brightness.BRIGHT.toByte());
		mPosConControllerLogic.addPosConInstrFor(device1Guid, null, (byte)4, (byte)6, (byte)3, (byte)9, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		mPosConControllerLogic.addPosConInstrFor(device2Guid, null, (byte)4, (byte)60, (byte)30, (byte)90, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		mPosConControllerLogic.addPosConInstrFor(ownGuid, null, (byte)3, (byte)65, (byte)35, (byte)95, Frequency.SOLID.toByte(), Brightness.MEDIUM.toByte());
		mPosConControllerLogic.updatePosCons();
		
		PosControllerInstr instr1 = mPosConControllerLogic.getPosConInstrFor(device1Guid, (byte)2);
		PosControllerInstr instr2 = mPosConControllerLogic.getPosConInstrFor(device1Guid, (byte)4);
		PosControllerInstr instr3 = mPosConControllerLogic.getPosConInstrFor(device2Guid, (byte)4);
		PosControllerInstr instr4 = mPosConControllerLogic.getPosConInstrFor(ownGuid, (byte)3);
		assertInstruction(instr1, null, (byte)2, (byte)50, (byte)40, (byte)60, Frequency.SOLID.toByte(), Brightness.BRIGHT.toByte());
		assertInstruction(instr2, null, (byte)4, (byte)6, (byte)3, (byte)9, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		assertInstruction(instr3, null, (byte)4, (byte)60, (byte)30, (byte)90, Frequency.BLINK.toByte(), Brightness.DIM.toByte());
		assertInstruction(instr4, null, (byte)3, (byte)65, (byte)35, (byte)95, Frequency.SOLID.toByte(), Brightness.MEDIUM.toByte());
		
		//Get PosCon displays for own device
		Map<Byte, PosControllerInstr> ownInstructions = mPosConControllerLogic.getPosToLastSetIntrMap();
		PosControllerInstr instr4Intr = ownInstructions.get((byte)3);
		assertInstruction(instr4Intr, null, (byte)3, (byte)65, (byte)35, (byte)95, Frequency.SOLID.toByte(), Brightness.MEDIUM.toByte());
		
		//CLEAR INSTRUCTIONS
		mPosConControllerLogic.clearAllPosConInstrsAndSend();
		instr4 = mPosConControllerLogic.getPosConInstrFor(ownGuid, (byte)3);
		Assert.assertNull(instr4);
		ownInstructions = mPosConControllerLogic.getPosToLastSetIntrMap();
		instr4Intr = ownInstructions.get((byte)3);
		Assert.assertNull(instr4Intr);
	}
	
	private void assertInstruction(PosControllerInstr instruction, LedSample inLedSample, byte position, byte quantity, byte min, byte max, byte frequency, byte brightness) {
		Assert.assertNotNull(instruction);
		Assert.assertEquals(instruction.getPosition(), position);
		Assert.assertEquals(instruction.getReqQty(), quantity);
		Assert.assertEquals(instruction.getMinQty(), min);
		Assert.assertEquals(instruction.getMaxQty(), max);
		Assert.assertEquals(instruction.getFreq(), frequency);
		Assert.assertEquals(instruction.getDutyCycle(), brightness);
	}
}
