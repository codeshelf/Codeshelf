/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ILocation.java,v 1.7 2013/04/26 03:26:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gadgetworks.codeshelf.device.LedCmdPath;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;

public interface ILocation<P extends IDomainObject> extends IDomainObjectTree<P> {

	PositionTypeEnum getAnchorPosTypeEnum();

	Double getAnchorPosX();

	Double getAnchorPosY();

	Double getAnchorPosZ();

	Point getAnchorPoint();

	void setAnchorPoint(Point inAnchorPoint);

	Point getAbsoluteAnchorPoint();

	String getDescription();

	void setDescription(String inDescription);

	Double getPosAlongPath();

	void setPosAlongPath(Double inPoseAlongPath);

	PathSegment getAssociatedPathSegment();

	boolean isLightable();
	
	LedController getLedController();

	void setLedController(LedController inLedController);

	Short getLedChannel();

	void setLedChannel(Short inLedChannel);

	Short getFirstLedNumAlongPath();

	void setFirstLedNumAlongPath(Short inFirstLedNum);

	Short getLastLedNumAlongPath();

	void setLastLedNumAlongPath(Short inLastLedNum);

	Short getFirstLedPosForItemId(final String inItemId);

	Short getLastLedPosForItemId(final String inItemId);

	String getPrimaryAliasId();
	
	Boolean isLeftSideTowardsAnchor();
	
	Boolean isLowerLedNearAnchor();
	
	Boolean isPathIncreasingFromAnchor();
	
	Boolean isActive();
	void setActive(Boolean inNowActive);

	// --------------------------------------------------------------------------
	/**
	 * If this is a DDC location then return the first DDC ID for the location.
	 * @return
	 */
	String getFirstDdcId();

	// --------------------------------------------------------------------------
	/**
	 * If this is a DDC location then set the first DDC ID for the location.
	 * @param inFirstDdcId
	 */
	void setFirstDdcId(String inFirstDdcId);

	// --------------------------------------------------------------------------
	/**
	 * If this is a DDC location then return the last DDC ID for the location.
	 * @return
	 */
	String getLastDdcId();

	// --------------------------------------------------------------------------
	/**
	 * If this is a DDC location then set the last DDC ID for the location.
	 * @param inFirstDdcId
	 */
	void setLastDdcId(String inLastDdcId);

	void setPathSegment(PathSegment inPathSegment);

	// --------------------------------------------------------------------------
	/**
	 * Get the vertices that make up this location's outline.  
	 * (Today the vertices always make a rectangle, but you should not assume this will always be true.)
	 * @return
	 */
	List<Vertex> getVertices();

	void setVertices(List<Vertex> inVertices);

	void addVertex(Vertex inVertex);

	void removeVertex(Vertex inVertex);

	// --------------------------------------------------------------------------
	/**
	 * Get the aliases for this location.
	 * @return
	 */
	List<LocationAlias> getAliases();

	void addAlias(LocationAlias inAlias);

	void removeAlias(LocationAlias inAlias);

	// --------------------------------------------------------------------------
	/**
	 * Get the first (Primary) alias for this location.
	 * (It's not clear at all what to do if the customer has multiple aliases for a location - not currently supported.)
	 * @return
	 */
	LocationAlias getPrimaryAlias();

	// --------------------------------------------------------------------------
	/**
	 * Get all of the items contained in this location.
	 * @return
	 */
	Map<String, Item> getStoredItems();

	void addStoredItem(Item inItem);
	
	Item getStoredItem(final String inItemDomainId);
	Item getStoredItemFromMasterIdAndUom(final String inItemMasterId, final String inUom);

	void removeStoredItem(final String inItemDomainId);
	void removeStoredItemFromMasterIdAndUom(final String inItemMasterId, final String inUom);
	// --------------------------------------------------------------------------
	/**
	 * Get all of the DDC item groups in this location (if it is a DDC location and has any DDC groups).
	 * @return
	 */
	List<ItemDdcGroup> getDdcGroups();

	void addItemDdcGroup(ItemDdcGroup inItemDdcGroup);

	ItemDdcGroup getItemDdcGroup(final String inItemDdcGroupId);

	void removeItemDdcGroup(final String inItemDdcGroupId);

	// --------------------------------------------------------------------------
	/**
	 * Get all of the child locations for this location.
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	List<ISubLocation> getChildren();

	// --------------------------------------------------------------------------
	/**
	 * Get all of the ACTIVE child locations for this location. These child locations that have not been soft-deleted
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	List<ISubLocation> getActiveChildren();

	// --------------------------------------------------------------------------
	/**
	 * Get all of the children of this type (no matter how far down the hierarchy).
	 * 
	 * To get it to strongly type the return for you then use this unusual Java construct at the caller:
	 * 
	 * Aisle aisle = facility.<Aisle> getActiveChildrenAtLevel(Aisle.class)
	 * (If calling this method from a generic location type then you need to define it as LocationABC<?> location.)
	 * 
	 * @param inClassWanted
	 * @return
	 */
	<T extends ISubLocation<?>> List<T> getActiveChildrenAtLevel(Class<? extends ISubLocation<?>> inClassWanted);

	// --------------------------------------------------------------------------
	/**
	 * Get the parent of this location at the class level specified.
	 * 
	 * To get it to strongly type the return for you then use this unusual Java construct at the caller:
	 * 
	 * Aisle aisle = bay.<Aisle> getParentAtLevel(Aisle.class)
	 * (If calling this method from a generic location type then you need to define it as LocationABC<?> location.)
	 * 
	 * @param inClassWanted
	 * @return
	 */
	<T extends ILocation<?>> T getParentAtLevel(Class<? extends ILocation<?>> inClassWanted);

	/**
	 * Get the location id up to but not including the facility
	 */
	String getNominalLocationId();

	String getNominalLocationIdExcludeBracket(); // If location is deleted, it would have brackets.

	String getLocationId();

	// --------------------------------------------------------------------------
	/**
	 * Build up a chain of location dotted notation (FQLN) up to the parent class level.
	 * E.g. A1.B2.T3.S1
	 * @param inClassWanted
	 * @return
	 */
	String getLocationIdToParentLevel(Class<? extends ILocation<?>> inClassWanted);

	void setLocationId(String inLocationId);

	// --------------------------------------------------------------------------
	/**
	 * Get a location by its location ID.
	 * @param inLocationId
	 * @return
	 */
	ISubLocation<?> findLocationById(String inLocationId);

	void removeLocation(String inLocationId);

	// --------------------------------------------------------------------------
	/**
	 * Look for any sub-location by it's ID.
	 * The location ID needs to be a dotted notation where the first octet is a child location of "this" location.
	 * @param inLocationId
	 * @return
	 */
	ISubLocation<?> findSubLocationById(String inLocationId);

	// --------------------------------------------------------------------------
	/**
	 * Get all of the sublocations (down the the very tree bottom) of this location, in working order.
	 * The working order gets applied to each level down from this location.
	 * Working order is top-to-bottom and then down-path.
	 * @return
	 */
	List<ILocation<?>> getSubLocationsInWorkingOrder();

	// --------------------------------------------------------------------------
	/**
	 * Get all of the children (one level down) of this location, in working order.
	 * Working order is top-to-bottom and then down-path.
	 * @return
	 */
	List<ISubLocation> getChildrenInWorkingOrder();
	
	/**
	 * Get Items in this location and down the tree by position along the path
	 */
	public List<Item> getInventoryInWorkingOrder();
	
	// --------------------------------------------------------------------------
	/**
	 * Recompute the path distance of this location (and recursively for all of its child locations).
	 */
	void computePosAlongPath(PathSegment inPathSegment);
	
	// --------------------------------------------------------------------------
	/**
	 * The effective controller is this one, or any of its parent up the chain
	 */
	LedController getEffectiveLedController();
	// --------------------------------------------------------------------------
	/**
	 * The effective channel is this one, or any of its parent up the chain
	 */
	Short getEffectiveLedChannel();
	
	Set<LedCmdPath> getAllLedCmdPaths();
	
}
