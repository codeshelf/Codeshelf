/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Che.java,v 1.12 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.exception.DataException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.DaoException;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.flyweight.command.ColorEnum;
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
@Table(name = "che")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Che extends WirelessDeviceABC {

	@Inject
	public static ITypedDao<Che>	DAO;

	@Singleton
	public static class CheDao extends GenericDaoABC<Che> implements ITypedDao<Che> {
		@Inject
		public CheDao(PersistenceService persistenceService) {
			super(persistenceService);
		}

		public final Class<Che> getDaoClass() {
			return Che.class;
		}
	}

	private static final Logger		LOGGER				= LoggerFactory.getLogger(Che.class);

	// The current work area. appears not to be used anywhere.
	/*
	@ManyToOne(optional = true, fetch=FetchType.LAZY)
	@Setter
	@JoinColumn(name = "current_work_area_persistentid")
	private WorkArea				currentWorkArea;
	*/

	// The current user. appears not to be used anywhere.
	/*
	@ManyToOne(optional = true, fetch=FetchType.LAZY)
	@Setter
	@JoinColumn(name = "current_user_persistentid")
	private User					currentUser;
	*/

	@NotNull
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private ColorEnum				color;

	@OneToMany(mappedBy = "currentChe")
	@Getter
	private List<ContainerUse>		uses				= new ArrayList<ContainerUse>();

	@OneToMany(mappedBy = "assignedChe")
	@Getter
	private List<WorkInstruction>	cheWorkInstructions	= new ArrayList<WorkInstruction>();

	public Che(String domainId) {
		this();
		setDomainId(domainId);
	}

	public Che() {
		super();
		color = ColorEnum.BLUE;
	}

	@SuppressWarnings("unchecked")
	public final ITypedDao<Che> getDao() {
		return DAO;
	}

	public final static void setDao(ITypedDao<Che> dao) {
		Che.DAO = dao;
	}

	/*
	// appears not to be used anywhere
	public User getCurrentUser() {
		if (currentUser instanceof HibernateProxy) {
			currentUser = (User) DomainObjectABC.deproxify(currentUser);
		}
		return currentUser;
	}

	// appears not to be used anywhere
	public WorkArea getCurrentWorkArea() {
		if (currentWorkArea instanceof HibernateProxy) {
			currentWorkArea = (WorkArea) DomainObjectABC.deproxify(currentWorkArea);
		}
		return currentWorkArea;
	}
	*/
	
	public final String getDefaultDomainIdPrefix() {
		return "CHE";
	}

	public final void addContainerUse(ContainerUse inContainerUse) {
		Che previousChe = inContainerUse.getCurrentChe();
		if (previousChe == null) {
			uses.add(inContainerUse);
			inContainerUse.setCurrentChe(this);
		} else if (previousChe.equals(this)) {
			LOGGER.warn("call to add ContainerUse " + inContainerUse.getDomainId() + " to " + this.getDomainId()
					+ " when it already is. This is a noOp ");
		} else {
			LOGGER.error("cannot add ContainerUse " + inContainerUse.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousChe.getDomainId());
		}
	}

	public final void removeContainerUse(ContainerUse inContainerUse) {
		if (uses.contains(inContainerUse)) {
			inContainerUse.setCurrentChe(null);
			uses.remove(inContainerUse);
		} else {
			LOGGER.error("cannot remove ContainerUse " + inContainerUse.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
	}

	public final void addWorkInstruction(WorkInstruction inWorkInstruction) {
		Che previousChe = inWorkInstruction.getAssignedChe();
		if (previousChe == null) {
			cheWorkInstructions.add(inWorkInstruction);
			inWorkInstruction.setAssignedChe(this);
		} else if (!previousChe.equals(this)) {
			LOGGER.error("cannot add WorkInstruction " + inWorkInstruction.getPersistentId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousChe.getDomainId());
		}
	}

	public final void removeWorkInstruction(WorkInstruction inWorkInstruction) {
		if (this.cheWorkInstructions.contains(inWorkInstruction)) {
			inWorkInstruction.setAssignedChe(null);
			cheWorkInstructions.remove(inWorkInstruction);
		} else {
			LOGGER.error("cannot remove WorkInstruction " + inWorkInstruction.getPersistentId() + " from " + this.getDomainId()
					+ " because it isn't found in children");
		}
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
			// Need to fix this. What kind of exception? Presumeably, bad controller ID that leads to invalid GUID
			LOGGER.error("Failed to set controller ID", e);
		}
		if (newGuid != null) {
			try {
				this.setDeviceNetGuid(newGuid);
				this.setDomainId(this.toString());
				// curious that setDeviceNetGuid does not do the persist
				Che.DAO.store(this);
			} catch (DaoException e) {
				LOGGER.error("", e);
			}
		}
	}

	// Utility functions for CHE work instructions, past runs and current
	// the lomboc getter gives us List<WorkInstruction> aList =	getCheWorkInstructions();
	// Work instruction has assignedChe. When work instructions are computed for a run, they all get the same assignedTime field set.

	public final Timestamp getTimeStampOfCurrentRun() {
		// return null if not on a current run.
		WorkInstruction latestAssignedWi = null; // there is no active field on wi

		for (WorkInstruction wi : cheWorkInstructions) {
			Timestamp wiTime = wi.getAssigned();
			if (wiTime != null) {
				if (latestAssignedWi == null)
					latestAssignedWi = wi;
				else {
					if (wiTime.after(latestAssignedWi.getAssigned()))
						latestAssignedWi = wi;
				}
			}
		}

		if (latestAssignedWi != null)
			return latestAssignedWi.getAssigned();

		return null;
	}

	private void doIntentionalPersistenceError() {
		// If you want to test this, change value of doThrowInstead in method below. Then from the UI, select a CHE and
		// do the testing only, set up containers.  Should need "simulate" login for this, although as of V11, works with "configure".
		// DEV-532 shows what used to happen before the error was caught and the transaction rolled back.
		String desc = "";
		for (int count = 0; count < 500; count++) {
			desc += "X";
		}
		// No try/catch here. The intent is to fail badly and see how the system handles it.
		this.setDescription(desc);
		Che.DAO.store(this);
		LOGGER.warn("Intentional database persistence error. Setting too long description on " + this.getDomainId());
	}

	// just a call through to facility, but convenient for the UI
	public final void fakeSetupUpContainersOnChe(String inContainers) {
		final boolean doThrowInstead = false;
		CodeshelfNetwork network = this.getParent();
		if (network == null)
			return;
		Facility facility = network.getParent();
		if (facility == null)
			return;
		if (doThrowInstead)
			doIntentionalPersistenceError();
		else
			facility.setUpCheContainerFromString(this, inContainers);
	}

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public final String getActiveContainers() {
		String returnStr = "";

		for (ContainerUse use : getUses()) {
			if (use.getActive()) {
				if (returnStr.isEmpty())
					returnStr = use.getContainerName();
				else {
					returnStr = returnStr + "," + use.getContainerName();
				}
			}
		}
		return returnStr;
	}

}
