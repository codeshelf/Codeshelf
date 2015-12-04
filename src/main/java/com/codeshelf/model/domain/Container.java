/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Container.java,v 1.18 2013/09/18 00:40:09 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

// --------------------------------------------------------------------------
/**
 * Container
 * 
 * An instance of a container class (ever) used in the facility.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "container", uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"})})
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Container extends DomainObjectTreeABC<Facility> {

	public static class ContainerDao extends GenericDaoABC<Container> implements ITypedDao<Container> {
		public final Class<Container> getDaoClass() {
			return Container.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Container.class);

	// The container kind. appears to be not used.
	@ManyToOne(optional = false, fetch=FetchType.LAZY)
	@Getter
	@Setter
	@JsonProperty
	private ContainerKind kind;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Boolean active;

	@Column(nullable = false)
	@Getter
	@Setter
	@JsonProperty
	private Timestamp updated;

	@OneToMany(mappedBy = "parent")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@Getter
	private List<ContainerUse>	uses = new ArrayList<ContainerUse>();

	public Container() {
	}
	
	public Container(String domainId, ContainerKind kind, boolean active) {
		this.setDomainId(domainId);
		this.active = active;
		this.kind = kind;
		this.updated = new Timestamp(System.currentTimeMillis());
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Container> getDao() {
		return staticGetDao();
	}

	public static ITypedDao<Container> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Container.class);
	}

	public final String getDefaultDomainIdPrefix() {
		return "P";
	}

	public String getContainerId() {
		return getDomainId();
	}

	public void setContainerId(String inContainerId) {
		setDomainId(inContainerId);
	}

	public Facility getFacility() {
		return getParent();
	}

	public List<? extends IDomainObject> getChildren() {
		return getUses();
	}

	public void addContainerUse(ContainerUse inContainerUse) {
		if (inContainerUse == null) {
			LOGGER.error("null input to Container.addContainerUse");
			return;
		}
		Container previousContainer = inContainerUse.getParent();
		if (previousContainer == null) {
			uses.add(inContainerUse);
			inContainerUse.setParent(this);
		} else if ((!previousContainer.equals(this)) || (!uses.contains(inContainerUse))) {
			LOGGER.error("cannot add ContainerUse " + inContainerUse.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousContainer.getDomainId());
		}
	}

	public void removeContainerUse(ContainerUse inContainerUse) {
		if (inContainerUse == null) {
			LOGGER.error("null input to Container.rermoveContainerUse");
			return;
		}
		if (uses.contains(inContainerUse)) {
			inContainerUse.setParent(null);
			uses.remove(inContainerUse);
		} else {
			LOGGER.error("cannot remove ContainerUse " + inContainerUse.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Get the container use associated with this order.
	 * @param inOrderHeader
	 * @return
	 */
	public ContainerUse getContainerUse(final OrderHeader inOrderHeader) {
		ContainerUse result = null;

		for (ContainerUse use : getUses()) {
			OrderHeader useHeader = use.getOrderHeader();
			if (useHeader != null) {
				if (useHeader.getOrderId().equals(inOrderHeader.getOrderId())) {
					result = use;
					break;
				}
			}
		}

		return result;
	}

	// --------------------------------------------------------------------------
	/**
	 * Return the currently working container use for this container.
	 * @return
	 */
	public OrderHeader getCurrentOrderHeader() {
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
	public ContainerUse getCurrentContainerUse() {
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
	public String toString() {
		// What we would want to see if logged as toString?
		String returnString = getDomainId();
		return returnString;
	}

}
