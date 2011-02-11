/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ITransport.java,v 1.3 2011/02/11 23:23:57 jeffw Exp $
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
