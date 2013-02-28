/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005-2008, Jeffrey B. Williams, All rights reserved
 *  $Id: CommandFactory.java,v 1.4 2013/02/28 06:24:52 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.flyweight.command;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gadgetworks.codeshelf.device.RadioController;
import com.gadgetworks.flyweight.bitfields.BitFieldInputStream;
import com.gadgetworks.flyweight.bitfields.NBitInteger;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class CommandFactory {

	private static final Log	LOGGER	= LogFactory.getLog(RadioController.class);

	private CommandFactory() {

	}

	// --------------------------------------------------------------------------
	/**
	 *  Read the next command off of the input stream.
	 *  @param inInputStream	The stream to read.
	 *  @param inCommandByteCount	The number of bytes to read.
	 *  @return	The command read from the input stream.
	 *  @throws IOException
	 */
	public static ICommand createCommand(BitFieldInputStream inInputStream, int inCommandByteCount) throws IOException {

		ICommand result = null;
		NetCommandGroup cmdGroupID;

		// Read the command group ID and endpoint from the input stream, and create the command for it.
		cmdGroupID = new NetCommandGroup(CommandGroupEnum.INVALID);
		inInputStream.readNBitInteger(cmdGroupID);

		NetEndpoint endpoint = new NetEndpoint(NetEndpoint.MGMT_ENDPOINT_NUM);
		inInputStream.readNBitInteger(endpoint);

		// Now that we know the command group ID, create the proper command.
		switch (cmdGroupID.getCommandEnum()) {
			case ASSOC:
				result = createAssocCommand(inInputStream);
				break;

			case NETMGMT:
				result = createNetMgmtCommand(inInputStream);
				break;

			case CONTROL:
				result = createControlCommand(inInputStream);
				break;

			default:
				LOGGER.error("Invalid command in packet: " + cmdGroupID.getCommandEnum());
				break;

		}

		if (result != null) {
			result.fromStream(endpoint, inInputStream, inCommandByteCount);
		} else {
			LOGGER.debug("Command factory result null!!!");
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  The network management command group always has a specific sub-command instead.
	 *  @param inInputStream	The stream to read
	 *  @return
	 *  @throws IOException
	 */
	private static ICommand createAssocCommand(BitFieldInputStream inInputStream) throws IOException {
		ICommand result = null;

		NetCommandId extCmdID = new NetCommandId(NBitInteger.INIT_VALUE);
		inInputStream.readNBitInteger(extCmdID);

		switch (extCmdID.getValue()) {
			case CommandAssocABC.ASSOC_REQ_COMMAND:
				//result = (ICommand) FlyWeightBeanFactory.getBean(CommandAssocReq.BEAN_ID);
				result = new CommandAssocReq();
				break;

			case CommandAssocABC.ASSOC_RESP_COMMAND:
				//result = (ICommand) FlyWeightBeanFactory.getBean(CommandAssocResp.BEAN_ID);
				result = new CommandAssocResp();
				break;

			case CommandAssocABC.ASSOC_CHECK_COMMAND:
				//result = (ICommand) FlyWeightBeanFactory.getBean(CommandAssocCheck.BEAN_ID);
				result = new CommandAssocCheck();
				break;

			case CommandAssocABC.ASSOC_ACK_COMMAND:
				//result = (ICommand) FlyWeightBeanFactory.getBean(CommandAssocAck.BEAN_ID);
				result = new CommandAssocAck();
				break;

			default:
				break;

		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  The network management command group always has a specific sub-command instead.
	 *  @param inInputStream	The stream to read
	 *  @return
	 *  @throws IOException
	 */
	private static ICommand createNetMgmtCommand(BitFieldInputStream inInputStream) throws IOException {
		ICommand result = null;

		NetCommandId extCmdID = new NetCommandId(NBitInteger.INIT_VALUE);
		inInputStream.readNBitInteger(extCmdID);

		switch (extCmdID.getValue()) {
			case CommandNetMgmtABC.NETSETUP_COMMAND:
				//result = (ICommand) FlyWeightBeanFactory.getBean(CommandNetMgmtSetup.BEAN_ID);
				result = new CommandNetMgmtSetup();
				break;

			case CommandNetMgmtABC.NETCHECK_COMMAND:
				//result = (ICommand) FlyWeightBeanFactory.getBean(CommandNetMgmtCheck.BEAN_ID);
				result = new CommandNetMgmtCheck();
				break;

			case CommandNetMgmtABC.NETINTFTEST_COMMAND:
				//result = (ICommand) FlyWeightBeanFactory.getBean(CommandNetMgmtIntfTest.BEAN_ID);
				result = new CommandNetMgmtIntfTest();
				break;

			default:
				break;

		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 *  The info command group always has a specific sub-command instead.
	 *  @param inInputStream	The stream to read
	 *  @return
	 *  @throws IOException
	 */
	private static ICommand createControlCommand(BitFieldInputStream inInputStream) throws IOException {
		ICommand result = null;

		NetCommandId extCmdID = new NetCommandId(NBitInteger.INIT_VALUE);
		inInputStream.readNBitInteger(extCmdID);

		switch (extCmdID.getValue()) {
			case CommandControlABC.SCAN:
				result = new CommandControlScan();
				break;

			case CommandControlABC.MESSAGE:
				result = new CommandControlMessage();
				break;

			default:
				break;

		}

		return result;
	}
}
