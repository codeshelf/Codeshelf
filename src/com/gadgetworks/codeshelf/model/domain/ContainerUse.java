/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerUse.java,v 1.2 2012/10/01 07:16:28 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
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
 * ContainerUse
 * 
 * A single use of the container in the facility
 * 
 * @author jeffw
 */

@Entity
@Table(name = "CONTAINERUSE")
@CacheStrategy
public class ContainerUse extends DomainObjectABC {

	@Inject
	public static ContainerUseDao	DAO;

	@Singleton
	public static class ContainerUseDao extends GenericDaoABC<ContainerUse> implements ITypedDao<ContainerUse> {
		public final Class<ContainerUse> getDaoClass() {
			return ContainerUse.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(ContainerUse.class);

	// Use date.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Timestamp			useTimeStamp;

	// The container used.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@JsonIgnore
	private Container			parent;

	public ContainerUse() {
	}

	@JsonIgnore
	public final ITypedDao<ContainerUse> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "USE";
	}

	public final Container getParentOrderHeader() {
		return parent;
	}

	public final void setParentContainer(final Container inContaienr) {
		parent = inContaienr;
	}

	public final IDomainObject getParent() {
		return getParentOrderHeader();
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Container) {
			setParentContainer((Container) inParent);
		}
	}

	@JsonIgnore
	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
