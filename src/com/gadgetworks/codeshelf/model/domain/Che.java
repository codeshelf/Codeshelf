/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Che.java,v 1.3 2013/02/27 01:17:02 jeffw Exp $
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

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;

// --------------------------------------------------------------------------
/**
 * Che
 * 
 * Controls the lights on an aisle.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "CHE", schema = "CODESHELF")
@CacheStrategy
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Che extends WirelessDeviceABC<CodeshelfNetwork> {

	@Inject
	public static ITypedDao<Che>	DAO;

	@Singleton
	public static class CheDao extends GenericDaoABC<Che> implements ITypedDao<Che> {
		public final Class<Che> getDaoClass() {
			return Che.class;
		}
	}

	private static final Log	LOGGER	= LogFactory.getLog(Che.class);

	// The parent facility.
	@Column(nullable = false)
	@ManyToOne(optional = false)
	private CodeshelfNetwork	parent;

	// The current work area.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private WorkArea			currentWorkArea;

	// The current user.
	@Column(nullable = true)
	@ManyToOne(optional = true)
	@Getter
	@Setter
	private User				currentUser;

	// For a network this is a list of all of the users that belong in the set.
	@OneToMany(mappedBy = "currentChe")
	@Getter
	private List<ContainerUse>	uses	= new ArrayList<ContainerUse>();

	public Che() {

	}

	public final ITypedDao<Che> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "CHE";
	}

	public final String getContainerId() {
		return getDomainId();
	}

	public final void setContainerId(String inContainerId) {
		setDomainId(inContainerId);
	}

	public final CodeshelfNetwork getParent() {
		return parent;
	}

	public final void setParent(CodeshelfNetwork inParent) {
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
}
