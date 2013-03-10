/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerKind.java,v 1.10 2013/03/10 08:58:43 jeffw Exp $
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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
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
@CacheStrategy
@Table(name = "CONTAINERKIND", schema = "CODESHELF")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ContainerKind extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<ContainerKind>	DAO;

	@Singleton
	public static class ContainerKindDao extends GenericDaoABC<ContainerKind> implements ITypedDao<ContainerKind> {
		public final Class<ContainerKind> getDaoClass() {
			return ContainerKind.class;
		}
	}

	private static final Logger	LOGGER					= LoggerFactory.getLogger(ContainerKind.class);

	public static final String	DEFAULT_CONTAINER_KIND	= "DEFAULT";

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	protected Facility			parent;

	// The container class ID.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected String			classId;

	// Length.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected Double			lengthMeters;

	// Width.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected Double			widthMeters;

	// Height.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	protected Double			heightMeters;

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
