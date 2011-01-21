/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2010, Jeffrey B. Williams, All rights reserved
 *  $Id: ISystemDAO.java,v 1.1 2011/01/21 01:08:22 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;

import com.gadgetworks.codeshelf.controller.IDeviceMaintainer;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.PickTag;
import com.gadgetworks.codeshelf.model.persist.SnapNetwork;
import com.gadgetworks.codeshelf.model.persist.WirelessDevice;

public interface ISystemDAO extends IDeviceMaintainer {

	String	BEAN_ID	= "ISystemDAO";
	
	boolean	USE_CACHE = true;

	void registerDAOListener(IDAOListener inListener);

	void unregisterDAOListener(IDAOListener inListener);

	void removeDAOListeners();

	void pushNonPersistentUpdates(PersistABC inPersistentObject);
	
	boolean isObjectPersisted(PersistABC inObject);

	// --------------------------------------------------------------------------
	// DBProperty

	DBProperty loadDBProperty(Integer inID);

	DBProperty findDBProperty(String inPropertyID);

	void storeDBProperty(DBProperty inProperty);

	void deleteDBProperty(DBProperty inProperty);

	Collection<DBProperty> getDBProperties();

	// --------------------------------------------------------------------------
	// PersistentProperty

	PersistentProperty loadPersistentProperty(Integer inID);

	PersistentProperty findPersistentProperty(String inPropertyID);

	void storePersistentProperty(PersistentProperty inProperty);

	void deletePersistentProperty(PersistentProperty inProperty);

	Collection<PersistentProperty> getPersistentProperties();

	// --------------------------------------------------------------------------
	// WirelessDevice

	WirelessDevice loadWirelessDevice(Integer inID);

	WirelessDevice findWirelessDeviceByGUID(String inGUID);

	void storeWirelessDevice(WirelessDevice inWirelessDevice);

	void deleteWirelessDevice(WirelessDevice inWirelessDevice);

	Collection<WirelessDevice> getWirelessDevices();
	
	// --------------------------------------------------------------------------
	// SnapNetwork

	SnapNetwork loadSnapNetwork(Integer inID);

	SnapNetwork findSnapNetwork(String inNetworkId);

	void storeSnapNetwork(SnapNetwork inSnapNetwork) throws DAOException;

	void deleteSnapNetwork(SnapNetwork inSnapNetwork) throws DAOException;

	Collection<SnapNetwork> getSnapNetworks();
	
	// --------------------------------------------------------------------------
	// ControlGroup

	ControlGroup loadControlGroup(Integer inID);

	ControlGroup findControlGroup(String inGroupId);

	void storeControlGroup(ControlGroup inControlGroup) throws DAOException;

	void deleteControlGroup(ControlGroup inControlGroup) throws DAOException;

	Collection<ControlGroup> getControlGroups();
	
	// --------------------------------------------------------------------------
	// PickTag

	PickTag loadPickTag(Integer inID);

	PickTag findPickTagByGUID(String inGUID);

	void storePickTag(PickTag inPickTag) throws DAOException;

	void deletePickTag(PickTag inPickTag) throws DAOException;

	Collection<PickTag> getPickTags();
	
}
