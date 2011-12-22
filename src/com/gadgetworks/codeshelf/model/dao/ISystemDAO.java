/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ISystemDAO.java,v 1.7 2011/12/22 11:46:31 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.util.Collection;

import com.gadgetworks.codeshelf.controller.IDeviceMaintainer;
import com.gadgetworks.codeshelf.controller.NetAddress;
import com.gadgetworks.codeshelf.controller.NetMacAddress;
import com.gadgetworks.codeshelf.controller.NetworkId;
import com.gadgetworks.codeshelf.model.persist.Aisle;
import com.gadgetworks.codeshelf.model.persist.CodeShelfNetwork;
import com.gadgetworks.codeshelf.model.persist.ControlGroup;
import com.gadgetworks.codeshelf.model.persist.DBProperty;
import com.gadgetworks.codeshelf.model.persist.Facility;
import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.gadgetworks.codeshelf.model.persist.PersistentProperty;
import com.gadgetworks.codeshelf.model.persist.PickTag;
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

//	DBProperty loadDBProperty(Integer inID);
//
//	DBProperty findDBProperty(String inPropertyID);
//
//	void storeDBProperty(DBProperty inProperty);
//
//	void deleteDBProperty(DBProperty inProperty);
//
//	Collection<DBProperty> getDBProperties();

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

	WirelessDevice findWirelessDeviceByMacAddr(NetMacAddress inMacAddr);

	void storeWirelessDevice(WirelessDevice inWirelessDevice);

	void deleteWirelessDevice(WirelessDevice inWirelessDevice);

	Collection<WirelessDevice> getWirelessDevices();
	
	// --------------------------------------------------------------------------
	// CodeShelfNetwork

	CodeShelfNetwork loadCodeShelfNetwork(Integer inID);

	CodeShelfNetwork findCodeShelfNetwork(NetworkId inNetworkId);

	void storeCodeShelfNetwork(CodeShelfNetwork inCodeShelfNetwork) throws DAOException;

	void deleteCodeShelfNetwork(CodeShelfNetwork inCodeShelfNetwork) throws DAOException;

	Collection<CodeShelfNetwork> getCodeShelfNetworks();
	
	// --------------------------------------------------------------------------
	// Facility

	Facility loadFacility(Integer inID);

	Facility findFacility(String inFacilityId);

	void storeFacility(Facility inFacility) throws DAOException;

	void deleteFacility(Facility inFacility) throws DAOException;

	Collection<Facility> getFacilities();
	
	// --------------------------------------------------------------------------
	// Aisle

	Aisle loadAisle(Integer inID);

	void storeAisle(Aisle inAisle) throws DAOException;

	void deleteAisle(Aisle inAisle) throws DAOException;

	Collection<Aisle> getAisles();
	
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

	PickTag findPickTagByMacAddr(NetMacAddress inMacAddr);

	PickTag findPickTagByNetAddr(NetAddress inMacAddr);

	void storePickTag(PickTag inPickTag) throws DAOException;

	void deletePickTag(PickTag inPickTag) throws DAOException;

	Collection<PickTag> getPickTags();
	
}
