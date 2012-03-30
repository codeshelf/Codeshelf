/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionReqCmd.java,v 1.4 2012/03/30 23:21:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
import com.gadgetworks.codeshelf.web.websession.command.IWebSessionCmd;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;


/**
 * @author jeffw
 *
 */
public interface IWebSessionReqCmd extends IWebSessionCmd {

	WebSessionReqCmdEnum getCommandEnum();
	
	String getCommandId();
	
	void setCommandId(String inCommandId);
	
	// --------------------------------------------------------------------------
	/**
	 * Calling this method causes the session to execute the command and create the (optional) result.
	 * @return
	 */
	IWebSessionRespCmd exec();
	
	// --------------------------------------------------------------------------
	/**
	 * Call this method when we have a new object that may change the state of remote data for a persistent command.
	 * @param inDomainObject	The domain object that changed.
	 * @return	The response command.
	 */
	IWebSessionRespCmd processObjectAdd(PersistABC inDomainObject);

	// --------------------------------------------------------------------------
	/**
	 * Call this method when we have an updated object that may change the state of remote data for a persistent command.
	 * @param inDomainObject	The domain object that changed.
	 * @return	The response command.
	 */
	IWebSessionRespCmd processObjectUpdate(PersistABC inDomainObject);

	// --------------------------------------------------------------------------
	/**
	 * Call this method when we have a deleted object that may change the state of remote data for a persistent command.
	 * @param inDomainObject	The domain object that changed.
	 * @return	The response command.
	 */
	IWebSessionRespCmd processObjectDelete(PersistABC inDomainObject);

	// --------------------------------------------------------------------------
	/**
	 * The method lets the system know if the command should stick around after we receive/exec() it the first time.
	 * The reason for this is that we have listener/filter commands that need to send back a repsonse later if an object updates.
	 * @return	True of the command should stay in the session.
	 */
	boolean doesPersist();
	
	// --------------------------------------------------------------------------
	/**
	 * This method gives the command the chance to register the WebSession with all of the DAOs that generate create/update/delete events
	 * that this command handles for the session (if it is a persistent command).
	 * @param inWebSession	The websession we register with the DAO.
	 */
	void registerSessionWithDaos(IWebSession inWebSession);

	// --------------------------------------------------------------------------
	/**
	 * This method give the command a change to unregister the WebSession with all of the DAOs that WERE involved in tracking 
	 * domain object changes.
	 * @param inWebSession	The WebSession we unregister from the DAO.
	 */
	void unregisterSessionWithDaos(IWebSession inWebSession);

}
