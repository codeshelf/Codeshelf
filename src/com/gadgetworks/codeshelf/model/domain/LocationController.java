/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: LocationController.java,v 1.4 2013/03/15 14:57:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
 * LocationController
 * 
 * Controls the lights on an aisle.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "AISLECONTROLLER", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class LocationController extends DomainObjectTreeABC<LocationABC> {

	@Inject
	public static ITypedDao<LocationController>	DAO;

	@Singleton
	public static class LocationControllerDao extends GenericDaoABC<LocationController> implements ITypedDao<LocationController> {
		public final Class<LocationController> getDaoClass() {
			return LocationController.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(LocationController.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private LocationABC			parent;

	public LocationController() {

	}

	public final ITypedDao<LocationController> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "AC";
	}

	public final String getContainerId() {
		return getDomainId();
	}

	public final void setContainerId(String inContainerId) {
		setDomainId(inContainerId);
	}

	public final LocationABC getParent() {
		return parent;
	}

	public final void setParent(LocationABC inParent) {
		parent = inParent;
	}

	public final List<LocationABC> getChildren() {
		return new ArrayList<LocationABC>();
	}
}
