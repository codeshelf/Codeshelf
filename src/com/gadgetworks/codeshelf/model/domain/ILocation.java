/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ILocation.java,v 1.7 2013/04/26 03:26:04 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import java.util.List;
import java.util.Map;

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
	
	PathSegment getPathSegment();

	Organization getParentOrganization();

	void setParentOrganization(Organization inParentOrganization);

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

	void setAliases(List<LocationAlias> inAliases);

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

	void setStoredItems(Map<String, Item> inItems);

	void addStoredItem(Item inItem);

	Item getStoredItem(String inItemId);

	void removeStoredItem(String inItemId);

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
	List<ISubLocation> getChildren();

	// --------------------------------------------------------------------------
	/**
	 * Get all of the children of this type (no matter how far down the hierarchy).
	 * 
	 * To get it to strongly type the return for you then use this unusual Java construct at the caller:
	 * 
	 * Aisle aisle = facility.<Aisle> getChildrenAtLevel(Aisle.class)
	 * (If calling this method from a generic location type then you need to define it as LocationABC<?> location.)
	 * 
	 * @param inClassWanted
	 * @return
	 */
	<T extends ISubLocation> List<T> getChildrenAtLevel(Class<? extends ISubLocation> inClassWanted);

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
	<T extends ILocation> T getParentAtLevel(Class<? extends ILocation> inClassWanted);

	String getLocationId();
	
	// --------------------------------------------------------------------------
	/**
	 * Build up a chain of location dotted notation (FQLN) up to the parent class level.
     * E.g. A1.B2.T3.S1
	 * @param inClassWanted
	 * @return
	 */
	String getLocationIdToParentLevel(Class<? extends ILocation> inClassWanted);

	void setLocationId(String inLocationId);

	void addLocation(ISubLocation inLocation);

	// --------------------------------------------------------------------------
	/**
	 * Get a location by its location ID.
	 * @param inLocationId
	 * @return
	 */
	ISubLocation findLocationById(String inLocationId);

	void removeLocation(String inLocationId);

	// --------------------------------------------------------------------------
	/**
	 * Look for any sub-location by it's ID.
	 * The location ID needs to be a dotted notation where the first octet is a child location of "this" location.
	 * @param inLocationId
	 * @return
	 */
	ISubLocation<?> findSubLocationById(String inLocationId);

	void setPathSegment(PathSegment inPathSegment);

	// --------------------------------------------------------------------------
	/**
	 * Recompute the path distance of this location (and recursively for all of its child locations).
	 */
	void computePathDistance();

}
