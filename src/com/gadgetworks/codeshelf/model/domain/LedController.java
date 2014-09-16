/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LedController.java,v 1.4 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.NetGuid;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * LedController
 * 
 * Controls the LEDs for a location.
 * 
 * @author jeffw
 */

@Entity
@CacheStrategy(useBeanCache = true)@Table(name = "led_controller")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
//@ToString(doNotUseGetters = true)
public class LedController extends WirelessDeviceABC {

	@Inject
	public static ITypedDao<LedController>	DAO;

	@Singleton
	public static class LedControllerDao extends GenericDaoABC<LedController> implements ITypedDao<LedController> {
		@Inject
		public LedControllerDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}
		
		public final Class<LedController> getDaoClass() {
			return LedController.class;
		}
	}

	private static final Logger		LOGGER		= LoggerFactory.getLogger(LedController.class);

	// All of the locations that use this controller.
	@SuppressWarnings("rawtypes")
	@OneToMany(mappedBy = "ledController")
	@Getter
	private List<SubLocationABC>	locations	= new ArrayList<SubLocationABC>();

	public LedController() {

	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<LedController> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "LED";
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addLocation(ISubLocation<?> inSubLocation) {
		// Ebean can't deal with interfaces.
		SubLocationABC<?> subLocation = (SubLocationABC<?>) inSubLocation;
		locations.add(subLocation);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeLocation(ISubLocation<?> inSubLocation) {
		locations.remove(inSubLocation);
	}
	
	//  Called from the UI, so really should return any persistence error.
	// Perhaps this should be at ancestor level. CHE changes this field only. LED controller changes domain ID and controller ID.
	// Therefore, see  and consider declone from Che::changeControllerId()
	public final void changeLedControllerId(String inNewControllerId) {
		NetGuid currentGuid = this.getDeviceNetGuid();
		NetGuid newGuid = null;
		try {
			newGuid = new NetGuid(inNewControllerId);
			if (currentGuid.equals(newGuid))
				return;
		}

		catch (Exception e) {

		}
		if (newGuid != null) {
			try {
				this.setDeviceNetGuid(newGuid);
				String newName = newGuid.getHexStringNoPrefix();
				this.setDomainId(newName);
				LedController.DAO.store(this);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

}
