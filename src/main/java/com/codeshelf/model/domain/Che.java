/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Che.java,v 1.12 2013/09/18 00:40:08 jeffw Exp $
 *******************************************************************************/
package com.codeshelf.model.domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
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
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeshelf.flyweight.command.ColorEnum;
import com.codeshelf.flyweight.command.NetGuid;
import com.codeshelf.model.WorkInstructionTypeEnum;
import com.codeshelf.model.dao.DaoException;
import com.codeshelf.model.dao.GenericDaoABC;
import com.codeshelf.model.dao.ITypedDao;
import com.codeshelf.persistence.TenantPersistenceService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

// --------------------------------------------------------------------------
/**
 * Che
 * 
 * Controls the lights on an aisle.
 * 
 * @author jeffw
 */

@Entity
@Table(name = "che"/*,uniqueConstraints = {@UniqueConstraint(columnNames = {"parent_persistentid", "domainid"}),@UniqueConstraint(columnNames = {"device_guid"})}*/)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Che extends WirelessDeviceABC {

	public static class CheDao extends GenericDaoABC<Che> implements ITypedDao<Che> {
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

	@Column(nullable = true, name = "scanner_type")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private ScannerTypeEnum			scannerType;

	@Column(name = "processmode")
	@Enumerated(value = EnumType.STRING)
	@Getter
	@Setter
	@JsonProperty
	private ProcessMode				processMode;

	// lastScannedLocation.
	@Column(nullable = true, name = "last_scanned_location")
	@Getter
	@Setter
	@JsonProperty
	private String					lastScannedLocation;

	@Column(nullable = true, name = "associate_to_che_guid")
	@Getter
	@Setter
	@JsonProperty
	private byte[]					associateToCheGuid;

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
		return staticGetDao();
	}

	public static ITypedDao<Che> staticGetDao() {
		return TenantPersistenceService.getInstance().getDao(Che.class);
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

	public void clearChe() {
		// This will produce immediate shorts. See cleanup in deleteExistingShortWiToFacility()

		// This is ugly. We probably do want a housekeeping type here, but then might want subtypes not in this query
		Collection<WorkInstructionTypeEnum> wiTypes = new ArrayList<WorkInstructionTypeEnum>(3);
		wiTypes.add(WorkInstructionTypeEnum.PLAN);
		wiTypes.add(WorkInstructionTypeEnum.HK_BAYCOMPLETE);
		wiTypes.add(WorkInstructionTypeEnum.HK_REPEATPOS);

		// Delete any planned WIs for this CHE.
		List<Criterion> filterParams = new ArrayList<Criterion>();
		filterParams.add(Restrictions.eq("assignedChe.persistentId", getPersistentId()));
		filterParams.add(Restrictions.in("type", wiTypes));
		List<WorkInstruction> wis = WorkInstruction.staticGetDao().findByFilter(filterParams);
		for (WorkInstruction wi : wis) {
			try {

				Che assignedChe = wi.getAssignedChe();
				if (assignedChe != null) {
					assignedChe.removeWorkInstruction(wi); // necessary? new from v3
				}
				OrderDetail owningDetail = wi.getOrderDetail();
				// detail is optional from v5
				if (owningDetail != null) {
					owningDetail.removeWorkInstruction(wi); // necessary? new from v3
					owningDetail.reevaluateStatus();
				}
				WorkInstruction.staticGetDao().delete(wi);
			} catch (DaoException e) {
				LOGGER.error("failed to delete prior work instruction for CHE", e);
			}
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

	public enum ProcessMode {
		SETUP_ORDERS,
		LINE_SCAN,
		PALLETIZER;

		public static ProcessMode getMode(String str) {
			if ("setup_orders".equalsIgnoreCase(str)) {
				return SETUP_ORDERS;
			} else if ("line_scan".equalsIgnoreCase(str)) {
				return LINE_SCAN;
			} else if ("palletizer".equalsIgnoreCase(str)) {
				return PALLETIZER;
			}
			return null;
		}
	}

	// --------------------------------------------------------------------------
	/**
	 * Functions related to remembering a CHE's current path and work area. For DEV-721
	 * By our definition in https://codeshelf.atlassian.net/wiki/display/TD/CD_0064B+Work+Area+Concepts
	 * the CHE's active path is the path of the getLastScannedLocation()
	 */
	public Path getActivePath() {
		Location loc = getLocationOfLastScan();
		if (loc != null && loc.isActive()) {
			PathSegment segment = loc.getAssociatedPathSegment();
			if (segment != null)
				return segment.getParent();
		}
		return null;
	}

	public Location getLocationOfLastScan() {
		String lastLocationName = this.getLastScannedLocation();
		if (lastLocationName == null || lastLocationName.isEmpty())
			return null;
		Facility facility = this.getFacility();
		return facility.findSubLocationById(lastLocationName);
	}

	public String getActivePathUi() {
		String returnStr = "";
		Path path = getActivePath();
		if (path != null) {
			returnStr = path.getDomainId();
		}
		return returnStr;
	}

	/**
	 * This requires a search or query. Doing a linear search to start.
	 * Warning: cannot do .equals on the byte[] returned by getDeviceGuid() or getAssociateToCheGuid()
	 */
	public Che getLinkedToChe() {
		byte[] theBytes = getAssociateToCheGuid();
		if (theBytes == null)
			return null;
		else {
			NetGuid associateToGuid = new NetGuid(theBytes);
			Che foundChe = null;
			CodeshelfNetwork network = this.getParent();
			for (Che che : network.getChes().values()) {
				// if (theBytes.equals(che.getDeviceGuid())){
				byte[] cheBytes = che.getDeviceGuid();
				if (cheBytes != null) {
					NetGuid cheGuid = new NetGuid(cheBytes);
					if (associateToGuid.equals(cheGuid)) {
						foundChe = che;
						// complain if associated to itself
						if (foundChe.equals(this)) {
							LOGGER.error("CHE associated to itself in getLinkedToChe()?");
							foundChe = null;
						}
						break;
					}
				}
			}
			return foundChe;
		}
	}

	/**
	 * This requires a search or query. Doing a linear search to start.
	 * Warning: cannot do .equals on the byte[] returned by getDeviceGuid() or getAssociateToCheGuid()
	 */
	public Che getCheLinkedToThis() {
		byte[] theBytes = getDeviceGuid();
		if (theBytes == null) {
			LOGGER.error("should never be in getCheLinkedToThis()");
			return null;
		} else {
			NetGuid myGuid = new NetGuid(theBytes);
			Che foundChe = null;
			CodeshelfNetwork network = this.getParent();
			for (Che che : network.getChes().values()) {
				// if (theBytes.equals(che.getAssociateToCheGuid())) {
				byte[] associatedBytes = che.getAssociateToCheGuid();
				if (associatedBytes != null) {
					NetGuid associatedGuid = new NetGuid(associatedBytes);
					if (associatedGuid.equals(myGuid)) {
						foundChe = che;
						// complain if associated to itself
						if (foundChe.equals(this)) {
							LOGGER.error("CHE linked to itself in getCheLinkedToThis()?");
							foundChe = null;
						}
						break;
					}
				}
			}
			return foundChe;
		}
	}

	/**
	 * This is a straight field look up. No search.
	 */
	public NetGuid getLinkedToGuid() {
		byte[] theBytes = getAssociateToCheGuid();
		if (theBytes == null)
			return null;
		else {
			NetGuid deviceGuid = new NetGuid(theBytes);
			return deviceGuid;
		}
	}

	/**
	 * For the UI. Show CHE name. One field shows either association direction
	 */
	public String getAssociateToUi() {
		String returnStr = "";
		Che associatedTo = getLinkedToChe();
		Che associateTee = getCheLinkedToThis();
		if (associatedTo != null && associateTee != null) {
			LOGGER.error("Both associated to and associated from");
			returnStr = "Error";
		}
		//TODO need localization of controlling and controlled by
		if (associatedTo != null) {
			String cheName = associatedTo.getDomainId();
			returnStr = String.format("controlling %s", cheName);
		} else if (associateTee != null) {
			String cheName = associateTee.getDomainId();
			returnStr = String.format("controlled by %s", cheName);
		}
		return returnStr;
	}

	public String getActiveWorkAreaUi() {
		// Stub for later use. Work area is a collection of paths. No Che UX field for this yet.
		// Existing CHE UX Work Area field needs to change, or come to this.
		String returnStr = "";
		Path path = getActivePath();
		if (path != null) {
			WorkArea area = path.getWorkArea();
			if (area != null) {
				returnStr = area.getWorkAreaId();
			}
		}
		return returnStr;
	}

}
