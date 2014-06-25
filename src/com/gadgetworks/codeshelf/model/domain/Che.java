/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Che.java,v 1.12 2013/09/18 00:40:08 jeffw Exp $
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
import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.annotation.CacheStrategy;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ISchemaManager;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.flyweight.command.NetGuid;
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
@CacheStrategy(useBeanCache = true)
@Table(name = "che")
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString(doNotUseGetters = true, callSuper = true)
public class Che extends WirelessDeviceABC {

	@Inject
	public static ITypedDao<Che>	DAO;

	@Singleton
	public static class CheDao extends GenericDaoABC<Che> implements ITypedDao<Che> {
		@Inject
		public CheDao(final ISchemaManager inSchemaManager) {
			super(inSchemaManager);
		}

		public final Class<Che> getDaoClass() {
			return Che.class;
		}
	}

	private static final Logger	LOGGER	= LoggerFactory.getLogger(Che.class);

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

	// ebeans maintains a lazy-loaded list of containerUse for this CHE
	@OneToMany(mappedBy = "currentChe")
	@Getter
	private List<ContainerUse>	uses	= new ArrayList<ContainerUse>();

	// ebeans maintains a lazy-loaded list of work instructions for this CHE
	@OneToMany(mappedBy = "assignedChe")
	@Getter
	private List<WorkInstruction>	cheWorkInstructions	= new ArrayList<WorkInstruction>();

	public Che() {

	}

	public final ITypedDao<Che> getDao() {
		return DAO;
	}

	public final String getDefaultDomainIdPrefix() {
		return "CHE";
	}

	// jr comment. Why are the containerUses the children? ContainerUse parent is not the Che.
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
	
	// Ebean Ops for work instructions
	public final void addWorkInstruction(WorkInstruction inWorkInstruction) {
		cheWorkInstructions.add(inWorkInstruction);
	}
	public final void removeWorkInstruction(WorkInstruction inWorkInstruction) {
		cheWorkInstructions.remove(inWorkInstruction);
	}


	//  Called from the UI, so really should return any persistence error.
	// Perhaps this should be at ancestor level. CHE changes this field only. LED controller changes domain ID and controller ID.
	public final void changeControllerId(String inNewControllerId) {
		NetGuid currentGuid = this.getDeviceNetGuid();
		NetGuid newGuid = null;
		try {
			newGuid = new NetGuid(inNewControllerId);
			if (currentGuid.equals(newGuid))
				return;
		}

		catch (Exception e) {

		}
		if (newGuid != null) {
			try {
				this.setDeviceNetGuid(newGuid);
				// curious that setDeviceNetGuid does not do the persist
				Che.DAO.store(this);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}
}
