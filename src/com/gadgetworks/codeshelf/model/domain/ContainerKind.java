/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerKind.java,v 1.15 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.proxy.HibernateProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * ContainerKind
 * 
 * Physical characteristics of a container class.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "container_kind")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ContainerKind extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<ContainerKind>	DAO;

	@Singleton
	public static class ContainerKindDao extends GenericDaoABC<ContainerKind> implements ITypedDao<ContainerKind> {
		@Inject
		public ContainerKindDao(PersistenceService persistenceService) {
			super(persistenceService);
		}
		
		public final Class<ContainerKind> getDaoClass() {
			return ContainerKind.class;
		}
	}

	@SuppressWarnings("unused")
	private static final Logger	LOGGER					= LoggerFactory.getLogger(ContainerKind.class);

	public static final String	DEFAULT_CONTAINER_KIND	= "DEFAULT";

	// The parent facility.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	private Facility parent;

	// The container class ID.
	@Column(nullable = false,name="class_id")
	@Getter
	@Setter
	@JsonProperty
	private String				classId;

	// Length.
	@Column(nullable = false,name="length_meters")
	@Getter
	@Setter
	@JsonProperty
	private Double				lengthMeters;

	// Width.
	@Column(nullable = false,name="width_meters")
	@Getter
	@Setter
	@JsonProperty
	private Double				widthMeters;

	// Height.
	@Column(nullable = false,name="height_meters")
	@Getter
	@Setter
	@JsonProperty
	private Double				heightMeters;

	public ContainerKind() {
		classId = "";
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<ContainerKind> getDao() {
		return DAO;
	}

	public final static void setDao(ITypedDao<ContainerKind> dao) {
		ContainerKind.DAO = dao;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public final Facility getParent() {
		if (this.parent instanceof HibernateProxy) {
			this.parent = (Facility) DomainObjectABC.deproxify(this.parent);
		}
		return parent;
	}

	public final Facility getFacility() {
		return getParent();
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

}
