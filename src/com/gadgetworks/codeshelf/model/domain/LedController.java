/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LedController.java,v 1.2 2013/04/09 07:58:20 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Getter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@CacheStrategy
@Table(name = "led_controller", schema = "codeshelf")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
//@ToString(doNotUseGetters = true)
public class LedController extends WirelessDeviceABC {

	@Inject
	public static ITypedDao<LedController>	DAO;

	@Singleton
	public static class LedControllerDao extends GenericDaoABC<LedController> implements ITypedDao<LedController> {
		public final Class<LedController> getDaoClass() {
			return LedController.class;
		}
	}

	private static final Logger		LOGGER		= LoggerFactory.getLogger(LedController.class);

	// All of the locations that use this controller.
	@OneToMany(mappedBy = "ledController")
	@Getter
	private List<SubLocationABC>	locations	= new ArrayList<SubLocationABC>();

	public LedController() {

	}

	public final ITypedDao<LedController> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "LED";
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addLocation(ISubLocation inSubLocation) {
		// Ebean can't deal with interfaces.
		SubLocationABC subLocation = (SubLocationABC) inSubLocation;
		locations.add(subLocation);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeLocation(ISubLocation inSubLocation) {
		locations.remove(inSubLocation);
	}
}
