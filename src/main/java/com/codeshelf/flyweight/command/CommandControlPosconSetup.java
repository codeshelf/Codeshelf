package com.codeshelf.flyweight.command;

public class CommandControlPosconSetup extends CommandControlABC{
	private static final int RESEND_DELAY = 0;
	
	/**
	 *  This is the constructor to use to create a data command to send to the network.
	 *  @param inEndpoint	The end point to send the command.
	 */
	public CommandControlPosconSetup(final NetEndpoint inEndpoint) {
		super(inEndpoint, new NetCommandId(CommandControlABC.POSCON_SETUP));
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is the constructor to use to create a data command that's read off of the network input stream.
	 */
	public CommandControlPosconSetup() {
		super(new NetCommandId(CommandControlABC.POSCON_SETUP));
	}

	@Override
	protected String doToString() {
		return null;
	}
	
	// --------------------------------------------------------------------------
	/**
	*  @return
	*/
	public int getResendDelay() {
		return RESEND_DELAY;
	}
}
