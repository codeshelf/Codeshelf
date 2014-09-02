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
 * Container
 * 
 * An instance of a container class (ever) used in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "container")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Container extends DomainObjectTreeABC<Facility> {

	@Inject
	public static ITypedDao<Container>	DAO;

	@Singleton
	public static class ContainerDao extends GenericDaoABC<Container> implements ITypedDao<Container> {
		@Inject
		public ContainerDao(PersistencyService persistencyService) {
			super(persistencyService);
		}

		public final Class<Container> getDaoClass() {
			return Container.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Container.class);

	// The container kind.
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

	// --------------------------------------------------------------------------
	/**
	 * Get the container use associated with this order.
	 * @param inOrderHeader
	 * @return
	 */
	public final ContainerUse getContainerUse(final OrderHeader inOrderHeader) {
		ContainerUse result = null;

		for (ContainerUse use : getUses()) {
			if (inOrderHeader.getOrderId().equals(use.getOrderHeader().getOrderId())) {
				result = use;
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the currently working container use for this container.
	 * @return
	 */
	public final OrderHeader getCurrentOrderHeader() {
		OrderHeader result = null;

		ContainerUse containerUse = getCurrentContainerUse();
		if (containerUse != null)
			result = containerUse.getOrderHeader();
		
		return result;
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Return the currently working container use for this container.
	 * @return
	 */
	public final ContainerUse getCurrentContainerUse() {
		ContainerUse result = null;

		// Find the container use with the latest timestamp - that's the active one.
		Timestamp timestamp = null;
		for (ContainerUse containerUse : getUses()) {
			if ((timestamp == null) || (containerUse.getUsedOn().after(timestamp))) {
				if (containerUse.getActive()) {
					timestamp = containerUse.getUsedOn();
					result = containerUse;
				}
			}
		}
		return result;
	}
	
	// Instead of lomboc annotation, which can lead to infinite loop if fields are not restricted
	public final String toString() {
		// What we would want to see if logged as toString?
		String returnString = getDomainId();
		return returnString;
	}


}
