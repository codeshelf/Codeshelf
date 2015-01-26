/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Che.java,v 1.12 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gadgetworks.codeshelf.model.dao.GenericDaoABC;
import com.gadgetworks.codeshelf.model.dao.ITypedDao;
import com.gadgetworks.codeshelf.platform.persistence.PersistenceService;
import com.gadgetworks.flyweight.command.ColorEnum;
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
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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

	public final String getDefaultDomainIdPrefix() {
		return "CHE";
	}

	public void addContainerUse(ContainerUse inContainerUse) {
		Che previousChe = inContainerUse.getCurrentChe();
		if (previousChe == null) {
			uses.add(inContainerUse);
			inContainerUse.setCurrentChe(this);
		} else if (previousChe.equals(this)) {
			LOGGER.warn("call to add ContainerUse " + inContainerUse.getDomainId() + " to " + this.getDomainId()
					+ " when it already is. This is a noOp ");
		} else {
			LOGGER.error("cannot add ContainerUse " + inContainerUse.getDomainId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousChe.getDomainId(), new Exception());
		}
	}

	public void removeContainerUse(ContainerUse inContainerUse) {
		if (uses.contains(inContainerUse)) {
			inContainerUse.setCurrentChe(null);
			uses.remove(inContainerUse);
		} else {
			LOGGER.error("cannot remove ContainerUse " + inContainerUse.getDomainId() + " from " + this.getDomainId()
					+ " because it isn't found in children", new Exception());
		}
	}

	public void addWorkInstruction(WorkInstruction inWorkInstruction) {
		Che previousChe = inWorkInstruction.getAssignedChe();
		if (previousChe == null) {
			cheWorkInstructions.add(inWorkInstruction);
			inWorkInstruction.setAssignedChe(this);
		} else if (!previousChe.equals(this)) {
			LOGGER.error("cannot add WorkInstruction " + inWorkInstruction.getPersistentId() + " to " + this.getDomainId()
					+ " because it has not been removed from " + previousChe.getDomainId(), new Exception());
		}
	}

	public void removeWorkInstruction(WorkInstruction inWorkInstruction) {
		if (this.cheWorkInstructions.contains(inWorkInstruction)) {
			inWorkInstruction.setAssignedChe(null);
			cheWorkInstructions.remove(inWorkInstruction);
		} else {
			LOGGER.error("cannot remove WorkInstruction " + inWorkInstruction.getPersistentId() + " from " + this.getDomainId()
					+ " because it isn't found in children", new Exception());
		}
	}

	// Utility functions for CHE work instructions, past runs and current
	// the lomboc getter gives us List<WorkInstruction> aList =	getCheWorkInstructions();
	// Work instruction has assignedChe. When work instructions are computed for a run, they all get the same assignedTime field set.

	public Timestamp getTimeStampOfCurrentRun() {
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

	// --------------------------------------------------------------------------
	/**
	 * For a UI meta field
	 * @return
	 */
	public String getActiveContainers() {
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
