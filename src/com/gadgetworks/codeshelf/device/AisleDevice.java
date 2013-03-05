package com.gadgetworks.codeshelf.device;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.flyweight.command.ColorEnum;
import com.gadgetworks.flyweight.command.CommandControlLight;
import com.gadgetworks.flyweight.command.ICommand;
import com.gadgetworks.flyweight.command.NetEndpoint;
import com.gadgetworks.flyweight.command.NetGuid;
import com.gadgetworks.flyweight.controller.IRadioController;

public class AisleDevice extends DeviceABC {

	private static final Logger	LOGGER	= LoggerFactory.getLogger(AisleDevice.class);

	public AisleDevice(final UUID inPersistentId, final NetGuid inGuid, final ICsDeviceManager inDeviceManager, final IRadioController inRadioController) {
		super(inPersistentId, inGuid, inDeviceManager, inRadioController);

	}

	@Override
	public final void start() {
		short position = 1;
		sendLightCommand(CommandControlLight.CHANNEL1, position, ColorEnum.BLUE, CommandControlLight.EFFECT_SOLID);

	}

	@Override
	public final void commandReceived(String inCommandStr) {
		// The aisle device never returns commands.
	}

	// --------------------------------------------------------------------------
	/**
	 * Send a display message to the CHE's embedded control device.
	 * @param inLine1Message
	 */
	private void sendLightCommand(final Short inChannel, final Short inPosition, final ColorEnum inColor, final String inEffect) {
		ICommand command = new CommandControlLight(NetEndpoint.PRIMARY_ENDPOINT, inChannel, inPosition, inColor, inEffect);
		mRadioController.sendCommand(command, getAddress(), false);
	}

}
