/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: AisleController.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

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
@Table(name = "AISLECONTROLLER", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class AisleController extends DomainObjectTreeABC<Aisle> {

	@Inject
	public static ITypedDao<AisleController>	DAO;

	@Singleton
	public static class AisleControllerDao extends GenericDaoABC<AisleController> implements ITypedDao<AisleController> {
		public final Class<AisleController> getDaoClass() {
			return AisleController.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(AisleController.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Aisle				parent;

	public AisleController() {

	}

	public final ITypedDao<AisleController> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "CC";
	}

	public final String getContainerId() {
		return getDomainId();
	}

	public final void setContainerId(String inContainerId) {
		setDomainId(inContainerId);
	}

	public final Aisle getParent() {
		return parent;
	}

	public final void setParent(Aisle inParent) {
		parent = inParent;
	}
}
