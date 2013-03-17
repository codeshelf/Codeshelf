/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: IWsPersistentReqCmd.java,v 1.1 2013/03/17 19:19:12 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.ws.command.req;

import java.util.Set;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.gadgetworks.codeshelf.ws.IWebSession;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;

/**
 * @author jeffw
 *
 */
public interface IWsPersistentReqCmd extends IWsReqCmd {

	Class<IDomainObject> getPersistenceClass();

	// --------------------------------------------------------------------------
	/**
	 * Call this method when we have a new object that may change the state of remote data for a persistent command.
	 * @param inDomainObject	The domain object that changed.
	 * @return	The response command.
	 */
	IWsRespCmd processObjectAdd(IDomainObject inDomainObject);

	// --------------------------------------------------------------------------
	/**
	 * Call this method when we have an updated object that may change the state of remote data for a persistent command.
	 * @param inDomainObject	The domain object that changed.
	 * @return	The response command.
	 */
	IWsRespCmd processObjectUpdate(IDomainObject inDomainObject, Set<String> inChangedProperties);

	// --------------------------------------------------------------------------
	/**
	 * Call this method when we have a deleted object that may change the state of remote data for a persistent command.
	 * @param inDomainObject	The domain object that changed.
	 * @return	The response command.
	 */
	IWsRespCmd processObjectDelete(IDomainObject inDomainObject);

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
