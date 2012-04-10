/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: IWebSessionPersistentReqCmd.java,v 1.1 2012/04/10 08:01:19 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.web.websession.command.req;

import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.web.websession.IWebSession;
import com.gadgetworks.codeshelf.web.websession.command.resp.IWebSessionRespCmd;

/**
 * @author jeffw
 *
 */
public interface IWebSessionPersistentReqCmd extends IWebSessionReqCmd {

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
