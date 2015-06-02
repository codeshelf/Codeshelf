package com.codeshelf.flyweight.command;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public class CommandControlPosconBroadcast extends CommandControlABC{
	public static final byte	POS_SHOW_ADDR 					= 1;
	public static final byte	CLEAR_POSCON 					= 2;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte 				mCommandId 						= 0;
	
	@Accessors(prefix = "m")
	@Getter @Setter
	private byte[] 				mExcludeMap 					= null;
	
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlPosconBroadcast(byte commandId, final NetEndpoint inEndpoint) {
		super(inEndpoint, new NetCommandId(CommandControlABC.POSCON_BROADCAST));
		setCommandId(commandId);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlPosconBroadcast() {
		super(new NetCommandId(CommandControlABC.POSCON_BROADCAST));
	}

	@Override
	protected String doToString() {
		return null;
	}

}
