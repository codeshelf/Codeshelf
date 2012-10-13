/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerKind.java,v 1.3 2012/10/13 22:14:24 jeffw Exp $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnore;

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
@Table(name = "CONTAINERKIND")
@CacheStrategy
public class ContainerKind extends DomainObjectABC {

	@Inject
	public static ITypedDao<ContainerKind>	DAO;

	@Singleton
	public static class ContainerKindDao extends GenericDaoABC<ContainerKind> implements ITypedDao<ContainerKind> {
		public final Class<ContainerKind> getDaoClass() {
			return ContainerKind.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(ContainerKind.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Facility			parent;

	// The container class ID.
	@Getter
	@Setter
	@Column(nullable = false)
	private String				classId;

	// Length.
	@Getter
	@Setter
	@Column(nullable = false)
	private Float				lengthMeters;

	// Width.
	@Getter
	@Setter
	@Column(nullable = false)
	private Float				widthMeters;

	// Height.
	@Getter
	@Setter
	@Column(nullable = false)
	private Float				heightMeters;

	public ContainerKind() {
		classId = "";
	}

	@JsonIgnore
	public final ITypedDao<ContainerKind> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	@JsonIgnore
	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	@JsonIgnore
	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
