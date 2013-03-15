/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ContainerUse.java,v 1.11 2013/03/15 14:57:13 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
 * ContainerUse
 * 
 * A single use of the container in the facility
 * 
 * @author jeffw
 */

@Entity
@Table(name = "CONTAINERUSE", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class ContainerUse extends DomainObjectTreeABC<Container> {

	@Inject
	public static ITypedDao<ContainerUse>	DAO;

	@Singleton
	public static class ContainerUseDao extends GenericDaoABC<ContainerUse> implements ITypedDao<ContainerUse> {
		public final Class<ContainerUse> getDaoClass() {
			return ContainerUse.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(ContainerUse.class);

	// The container used.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Container			parent;

	// Use date.
	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			useTimeStamp;

	// The order where we used this container.
	@Column(nullable = false)
	@OneToOne(optional = true)
	@Getter
	@Setter
	private OrderHeader			orderHeader;

	// The che where we're using this container.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private Che					currentChe;

	public ContainerUse() {
	}

	public final ITypedDao<ContainerUse> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "USE";
	}

	public final Container getParentContainer() {
		return parent;
	}

	public final void setParentContainer(final Container inContaienr) {
		parent = inContaienr;
	}

	public final Container getParent() {
		return getParentContainer();
	}

	public final void setParent(Container inParent) {
		parent = inParent;
	}

	public final List<IDomainObject> getChildren() {
		return new ArrayList<IDomainObject>();
	}
}
