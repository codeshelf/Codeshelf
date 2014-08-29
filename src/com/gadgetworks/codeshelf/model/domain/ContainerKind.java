/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerKind.java,v 1.15 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.services.PersistencyService;
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
//@CacheStrategy(useBeanCache = true)
@Table(name = "container_kind")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class ContainerKind extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<ContainerKind>	DAO;

	@Singleton
	public static class ContainerKindDao extends GenericDaoABC<ContainerKind> implements ITypedDao<ContainerKind> {
		@Inject
		public ContainerKindDao(PersistencyService persistencyService) {
			super(persistencyService);
		}
		
		public final Class<ContainerKind> getDaoClass() {
			return ContainerKind.class;
		}
	}

	private static final Logger	LOGGER					= LoggerFactory.getLogger(ContainerKind.class);

	public static final String	DEFAULT_CONTAINER_KIND	= "DEFAULT";

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility			parent;

	// The container class ID.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private String				classId;

	// Length.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				lengthMeters;

	// Width.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				widthMeters;

	// Height.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Double				heightMeters;

	public ContainerKind() {
		classId = "";
	}

	public final ITypedDao<ContainerKind> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
