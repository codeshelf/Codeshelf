/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: AtopCmdMapper.java,v 1.2 2011/02/21 21:33:46 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.server.tags;

/**
 * @author jeffw
 *
 */
public class AtopCmdMapper {
	
	protected AtopCmdMapper() {
		
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inDataBytes
	 * @param inSubCommand
	 * @param inSerialBusNumber
	 */
	protected static void buildHeader(byte[] inDataBytes, byte inSubCommand, byte inSerialBusNumber) {
		inDataBytes[0] = (byte) inDataBytes.length;
		inDataBytes[1] = 0x00;
		inDataBytes[2] = 0x60;
		inDataBytes[3] = 0x00;
		inDataBytes[4] = 0x00;
		inDataBytes[5] = 0x00;
		inDataBytes[6] = inSubCommand;
		inDataBytes[7] = inSerialBusNumber;
	}

}
