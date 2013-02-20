/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ITransport.java,v 1.1 2013/02/20 08:28:26 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.controller;

import java.util.List;

import com.gadgetworks.codeshelf.command.CommandIdEnum;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public interface ITransport {

	String	BEAN_ID	= "Transport";

	NetworkId getNetworkId();
	
	void setNetworkId(NetworkId inNetworkid);
	
	NetAddress getSrcAddr();
	
	void setSrcAddr(NetAddress inSrcAddr);
	
	NetAddress getDstAddr();
	
	void setDstAddr(NetAddress inDstAddr);

	CommandIdEnum getCommandId();
	
	void setCommandId(CommandIdEnum inCommandId);
	
	void setNextParam(Object inParam);

	Object getParam(int inParamNum);
	
	List<Object> getParams();
	
}
