package com.gadgetworks.codeshelf.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gadgetworks.codeshelf.device.LedCmdGroup;
import com.gadgetworks.codeshelf.device.LedCmdGroupSerializer;
import com.gadgetworks.codeshelf.device.LedSample;
import com.gadgetworks.codeshelf.model.LedRange;
import com.gadgetworks.codeshelf.model.domain.Bay;
import com.gadgetworks.codeshelf.model.domain.CodeshelfNetwork;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.ILocation;
import com.gadgetworks.codeshelf.model.domain.ISubLocation;
import com.gadgetworks.codeshelf.model.domain.Item;
import com.gadgetworks.codeshelf.model.domain.LedController;
import com.gadgetworks.codeshelf.model.domain.LocationABC;
import com.gadgetworks.codeshelf.model.domain.SiteController;
import com.gadgetworks.codeshelf.model.domain.Tier;
import com.gadgetworks.codeshelf.model.domain.User;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.LightLedsMessage;
import com.gadgetworks.codeshelf.ws.jetty.protocol.message.MessageABC;
import com.gadgetworks.codeshelf.ws.jetty.server.CsSession;
import com.gadgetworks.codeshelf.ws.jetty.server.SessionManager;
import com.gadgetworks.flyweight.command.ColorEnum;
import com.google.common.collect.ArrayListMultimap;

public class LightService {
	
	private static final int	LIGHT_LOCATION_DURATION_SECS = 20;
	private static final Logger	LOGGER				    	 = LoggerFactory.getLogger(LightService.class);
	
	private final SessionManager sessionManager;
	
	public LightService() {
		this(SessionManager.getInstance());
	}

	public LightService(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public void lightAllControllers(final String inColorStr, final String facilityPersistentId, final String inLocationNominalId) {
		
		ColorEnum theColor = ColorEnum.valueOf(inColorStr);
		if (theColor == ColorEnum.INVALID) {
			LOGGER.error("lightOneLocation called with unknown color");
			return;
		}

		Facility facility = Facility.DAO.findByPersistentId(facilityPersistentId);
		if (facility == null) {
			LOGGER.error("lightAllControllers called with unknown facility");
			return;
		}

		
		ISubLocation<?> theLocation = facility.findSubLocationById(inLocationNominalId);
		if (theLocation == null || theLocation instanceof Facility) {
			LOGGER.error("lightAllControllers called with unknown location");
			return;
		}

		HashMap<ControllerChannelBayKey, Tier> lastLocationWithinBay = new HashMap<ControllerChannelBayKey, Tier>();
		List<Tier> tiers = theLocation.getActiveChildrenAtLevel(Tier.class);
		if (tiers.isEmpty()) {
			LOGGER.error("lightAllControllers called with location with no tiers");
			return;
		}
		for (Tier tier : tiers) {
			ControllerChannelBayKey key = new ControllerChannelBayKey(tier.getEffectiveLedController(), tier.getEffectiveLedChannel(), (Bay) tier.getParent());
			ISubLocation<?> lastLocation = lastLocationWithinBay.get(key);

			if (lastLocation == null || lastLocation.getLastLedNumAlongPath() < tier.getLastLedNumAlongPath()) {
				lastLocationWithinBay.put(key, tier);
			}
		}
		
		ArrayList<LedCmdGroup> ledCmdGroups = new ArrayList<LedCmdGroup>();
		for (Entry<ControllerChannelBayKey, Tier> keyedTier : lastLocationWithinBay.entrySet()) {
			Tier lastTierInBayForControllerChannel = keyedTier.getValue();
			List<LedCmdGroup> ledCmdGroupList = getLedCmdGroupListForLocation(2, theColor, lastTierInBayForControllerChannel);
			ledCmdGroups.addAll(ledCmdGroupList);
		} 
		
		if (ledCmdGroups.size() == 0) {
			LOGGER.info("light a location for each controller channel called with incomplete LED configuration");
			return;
		}
		sendToAllSiteControllers(facility, ledCmdGroups);
	}
	
	// --------------------------------------------------------------------------
	/**
	 * Light one location transiently. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent. 
	 */
	public void lightOneLocation(final String inColorStr, final String facilityPersistentId, final String inLocationNominalId) {
				
		ColorEnum theColor = ColorEnum.valueOf(inColorStr);
		if (theColor == ColorEnum.INVALID) {
			LOGGER.error("lightOneLocation called with unknown color: " + theColor);
			return;
		}

		Facility facility = Facility.DAO.findByPersistentId(facilityPersistentId);
		if (facility == null) {
			LOGGER.error("lightOneLocation called with unknown facility: " + facilityPersistentId);
			return;
		}

		ISubLocation<?> theLocation = facility.findSubLocationById(inLocationNominalId);
		if (theLocation == null || theLocation instanceof Facility) {
			LOGGER.error("lightOneLocation called with unknown location: " + theLocation);
			return;
		}

		// IMPORTANT. When DEV-411 resumes, change to 4.  For now, we want only 3 LED lit at GoodEggs.
		List<LedCmdGroup> ledCmdGroupList = getLedCmdGroupListForLocation(3, theColor, theLocation);
		if (ledCmdGroupList.size() == 0) {
			LOGGER.info("lightOneLocation called for location with incomplete LED configuration: " + theLocation);
			return;
		} 
		
		sendToAllSiteControllers(facility, ledCmdGroupList);
	}

	// --------------------------------------------------------------------------
	/**
	 * Light one item. Any subsequent activity on the aisle controller will wipe this away.
	 * May be called with BLACK to clear whatever you just sent.
	 */
	public void lightOneItem(final String inColorStr, final String facilityPersistentId, final String inItemPersistentId) {

		ColorEnum theColor = ColorEnum.valueOf(inColorStr);
		if (theColor == ColorEnum.INVALID) {
			LOGGER.error("lightOneItem called with unknown color");
			return;
		}

		Facility facility = Facility.DAO.findByPersistentId(facilityPersistentId);
		if (facility == null) {
			LOGGER.error("lightOneItem called with unknown facility");
			return;
		}
		
		Item theItem = Item.DAO.findByPersistentId(inItemPersistentId);
		if (theItem == null) {
			LOGGER.error("lightOneItem called with unknown item");
			return;
		}

		// IMPORTANT. When DEV-411 resumes, change to 4.  For now, we want only 3 LED lit at GoodEggs.
		List<LedCmdGroup> ledCmdGroupList = getLedCmdGroupListForItem(3, theColor, theItem);
		if (ledCmdGroupList.size() == 0) {
			LOGGER.info("lightOneItem called for location with incomplete LED configuration: " + theItem);
			return;
		}
		else {
			sendToAllSiteControllers(facility, ledCmdGroupList);
		}

	}

	private final void sendToAllSiteControllers(Facility facility, List<LedCmdGroup> ledCmdGroupList) {
		ArrayListMultimap<String, LedCmdGroup> byController = ArrayListMultimap.<String, LedCmdGroup>create();
		for (LedCmdGroup ledCmdGroup : ledCmdGroupList) {
			byController.put(ledCmdGroup.getControllerId(), ledCmdGroup);
		}
		
		for (String theGuidStr : byController.keys()) {
			List<LedCmdGroup> ledCmdGroups = byController.get(theGuidStr);
			
			String theLedCommands = LedCmdGroupSerializer.serializeLedCmdString(ledCmdGroups);
			LightLedsMessage theMessage = new LightLedsMessage(theGuidStr, LIGHT_LOCATION_DURATION_SECS, theLedCommands);
			LOGGER.debug("Sending LightLedsMessage to all site controllers: " + theMessage);
			sendToAllSiteControllers(facility, theMessage);
		}

	}
	
	private final int sendToAllSiteControllers(Facility facility, MessageABC message) {
		Set<User> users = this.getSiteControllerUsers(facility);
		Set<CsSession> sessions = this.sessionManager.getSessions(users);
		for (CsSession session : sessions) {
			session.sendMessage(message);
		}
		return sessions.size();
	}
	
	private final Set<SiteController> getSiteControllers(Facility facility) {
		Set<SiteController> siteControllers = new HashSet<SiteController>();

		for (CodeshelfNetwork network : facility.getNetworks()) {
			siteControllers.addAll(network.getSiteControllers().values());
		}
		return siteControllers;
	}

	public final Set<User> getSiteControllerUsers(Facility facility) {
		Set<User> users = new HashSet<User>();

		for (SiteController sitecon : this.getSiteControllers(facility)) {
			User user = User.DAO.findByDomainId(sitecon.getParent().getParent().getParentOrganization(), sitecon.getDomainId());
			if (user != null) {
				users.add(user);
			} else {
				LOGGER.warn("Couldn't find user for site controller " + sitecon.getDomainId());
			}
		}
		return users;
	}

	private List<LedCmdGroup> getLedCmdGroupListForItem(int maxNumLeds, final ColorEnum inColor, final Item inItem) {
		// Use our utility function to get the leds for the item
		LedRange theRange = inItem.getFirstLastLedsForItem().capLeds(maxNumLeds);
		LocationABC<?> locationABC = inItem.getStoredLocation();
		return getLedCmdGroupListForRange(inColor, locationABC.getEffectiveLedController(), locationABC.getEffectiveLedChannel(), theRange);
	}

	
	private List<LedCmdGroup> getLedCmdGroupListForLocation(int maxNumLeds, final ColorEnum inColor, final ILocation<?> inLocation) {
		LedRange theRange = ((LocationABC<?>) inLocation).getFirstLastLedsForLocation().capLeds(maxNumLeds);;
		LedController theLedController = inLocation.getEffectiveLedController();
		inLocation.getEffectiveLedChannel();
		return getLedCmdGroupListForRange(inColor, theLedController, inLocation.getEffectiveLedChannel(), theRange);
	}
	/**
	 * Utility function to create LED command group. Will return a list, which may be empty if there is nothing to send. Caller should check for empty list.
	 * Called now for setting WI LED pattern for inventory pick.
	 * Also called for directly lighting inventory item or location
	 * @param inNetGuidStr
	 * @param inItem
	 * @param inColor
	 */
	private List<LedCmdGroup> getLedCmdGroupListForRange(final ColorEnum inColor, LedController controller, short controllerChannel, final LedRange ledRange) {

		short firstLedPosNum = ledRange.getFirstLedToLight();
		short lastLedPosNum = ledRange.getLastLedToLight();
		List<LedCmdGroup> ledCmdGroupList = new ArrayList<LedCmdGroup>();


		// if the led number is zero, we do not have tubes or lasers there. Do not proceed.
		if (firstLedPosNum == 0)
			return ledCmdGroupList;

		// This is how we send LED data to the remote controller. In this case, only one led sample range.
		List<LedSample> ledSamples = new ArrayList<LedSample>();
		for (short ledPos = firstLedPosNum; ledPos <= lastLedPosNum; ledPos++) {
			LedSample ledSample = new LedSample(ledPos, inColor);
			ledSamples.add(ledSample);
		}
		LedCmdGroup ledCmdGroup = new LedCmdGroup(controller.getDeviceGuidStr(), controllerChannel, firstLedPosNum, ledSamples);
		ledCmdGroupList.add(ledCmdGroup);
		return ledCmdGroupList;
	}

	
	@EqualsAndHashCode
	private class ControllerChannelBayKey  {
		
		@Getter
		private String controllerGuid;
		
		@Getter
		private short channel;
		private String bayId;
		
		public ControllerChannelBayKey(LedController controller, short channel, Bay bay) {
			this.controllerGuid = controller.getDeviceGuidStr();
			this.channel = channel;
			this.bayId = bay.getLocationId();
		}
	}
	
}
