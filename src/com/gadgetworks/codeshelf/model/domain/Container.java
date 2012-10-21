/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Container.java,v 1.7 2012/10/21 02:02:18 jeffw Exp $
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
import lombok.Setter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonProperty;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
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
@Table(name = "CONTAINER")
@CacheStrategy
@JsonAutoDetect(getterVisibility=Visibility.NONE)
public class Container extends DomainObjectABC {

	@Inject
	public static ITypedDao<Container>	DAO;

	@Singleton
	public static class ContainerDao extends GenericDaoABC<Container> implements ITypedDao<Container> {
		public final Class<Container> getDaoClass() {
			return Container.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Container.class);

	// The container kind.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	@Getter
	@Setter
	@JsonProperty
	private ContainerKind		kind;

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

	public final Facility getParentFacility() {
		return parent;
	}

	public final void setParentFacility(final Facility inFacility) {
		parent = inFacility;
	}

	public final String getContainerId() {
		return getShortDomainId();
	}

	public final void setContainerId(String inContainerId) {
		setShortDomainId(inContainerId);
	}

	public final IDomainObject getParent() {
		return parent;
	}

	public final void setParent(IDomainObject inParent) {
		if (inParent instanceof Facility) {
			setParentFacility((Facility) inParent);
		}
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
}
