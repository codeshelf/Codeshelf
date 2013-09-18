/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Container.java,v 1.18 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Container
 * 
 * An instance of a container class (ever) used in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "container")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Container extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<Container>	DAO;

	@Singleton
	public static class ContainerDao extends GenericDaoABC<Container> implements ITypedDao<Container> {
		@Inject
		public ContainerDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}
		
		public final Class<Container> getDaoClass() {
			return Container.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Container.class);

	// The container kind.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	@JsonProperty
	private ContainerKind		kind;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean				active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp			updated;

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private Facility			parent;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "parent")
	@Getter
	private List<ContainerUse>	uses	= new ArrayList<ContainerUse>();

	public Container() {

	}

	public final ITypedDao<Container> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public final String getContainerId() {
		return getDomainId();
	}

	public final void setContainerId(String inContainerId) {
		setDomainId(inContainerId);
	}

	public final Facility getParent() {
		return parent;
	}

	public final void setParent(Facility inParent) {
		parent = inParent;
	}

	public final List<? extends IDomainObject> getChildren() {
		return getUses();
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void addContainerUse(ContainerUse inContainerUse) {
		uses.add(inContainerUse);
	}

	// Even though we don't really use this field, it's tied to an eBean op that keeps the DB in synch.
	public final void removeContainerUse(ContainerUse inContainerUse) {
		uses.remove(inContainerUse);
	}

	public final ContainerUse getContainerUse(final OrderHeader inOrderHeader) {
		ContainerUse result = null;

		for (ContainerUse use : getUses()) {
			if (inOrderHeader.getOrderId().equals(use.getOrderHeader().getOrderId())) {
				result = use;
			}
		}

		return result;
	}
}
