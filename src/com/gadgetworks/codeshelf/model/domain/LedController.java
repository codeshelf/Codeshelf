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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
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
@Table(name = "led_controller")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class LedController extends WirelessDeviceABC {

	@Inject
	public static ITypedDao<LedController>	DAO;

	@Singleton
	public static class LedControllerDao extends GenericDaoABC<LedController> implements ITypedDao<LedController> {
		@Inject
		public LedControllerDao(final PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<LedController> getDaoClass() {
			return LedController.class;
		}
	}

	private static final Logger		LOGGER		= LoggerFactory.getLogger(LedController.class);
	
	// parent of che is network
	//@Getter @Setter
	//@ManyToOne(optional = false)
	//private CodeshelfNetwork parent;

	// All of the locations that use this controller.
	@OneToMany(targetEntity=SubLocationABC.class)
	@Getter
	private List<SubLocationABC<?>> locations	= new ArrayList<SubLocationABC<?>>();

	public LedController() {

	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<LedController> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "LED";
	}

	public final void addLocation(ISubLocation<?> inSubLocation) {
		LedController previousLedController = inSubLocation.getLedController();
		if(previousLedController == null) {
			SubLocationABC<?> subLocation = (SubLocationABC<?>) inSubLocation;
			locations.add(subLocation);
			inSubLocation.setLedController(this);
		} else {
			LOGGER.error("cannot add Location "+inSubLocation.getDomainId()+" to "+this.getDomainId()+" because it has not been removed from "+previousLedController.getDomainId());
		}	
	}

	public final void removeLocation(ISubLocation<?> inSubLocation) {
		if(locations.contains(inSubLocation)) {
			inSubLocation.setLedController(null);
			locations.remove(inSubLocation);
		} else {
			LOGGER.error("cannot remove Location "+inSubLocation.getDomainId()+" from "+this.getDomainId()+" because it isn't found in children");
		}
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

	public static void setDao(LedControllerDao inLedControllerDao) {
		LedController.DAO = inLedControllerDao;
	}

}
