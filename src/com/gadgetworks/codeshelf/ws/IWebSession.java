/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSession.java,v 1.1 2013/03/17 19:19:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws;

import com.gadgetworks.codeshelf.model.dao.IDaoListener;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * @author jeffw
 *
 */
public interface IWebSession extends IDaoListener {
	enum ConnectionActivityStatus { ACTIVE, IDLE, DEAD };

	IWsRespCmd processMessage(String inMessage);

	void sendCommand(IWsRespCmd inCommand);

	void endSession();
	
	void resetPongTimer();
	long getPongTimerElapsedMillis();
	boolean setConnectionActivityStatus(ConnectionActivityStatus inNewStatus);
	
	// String getContextFacility()
	// String getContextUserBadgeId()
	// String getContextFacility()
}
