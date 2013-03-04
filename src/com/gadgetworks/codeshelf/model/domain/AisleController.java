/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleController.java,v 1.3 2013/03/04 04:47:27 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.ToString;

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
 * AisleController
 * 
 * Controls the lights on an aisle.
 * 
 * @author jeffw
 */

@Entity
@CacheStrategy
@Table(name = "AISLECONTROLLER", schema = "CODESHELF")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString
public class AisleController extends WirelessDeviceABC {

	@Inject
	public static ITypedDao<AisleController>	DAO;

	@Singleton
	public static class AisleControllerDao extends GenericDaoABC<AisleController> implements ITypedDao<AisleController> {
		public final Class<AisleController> getDaoClass() {
			return AisleController.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(AisleController.class);

	// The parent aisle.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Aisle				parentAisle;

	public AisleController() {

	}

	public final ITypedDao<AisleController> getDao() {
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

	public final Aisle getParentAisle() {
		return parentAisle;
	}

	public final void setParentAisle(Aisle inParentAisle) {
		parentAisle = inParentAisle;
	}
}
