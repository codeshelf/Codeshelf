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

	PositionTypeEnum getPosTypeEnum();

	void setPosTypeEnum(PositionTypeEnum inPosTypeEnum);

	Double getPosX();

	void setPosX(Double inPosX);

	Double getPosY();

	void setPosY(Double inPosY);

	Double getPosZ();

	void setPosZ(Double inPosZ);

	String getDescription();

	void setDescription(String inDescription);

	// --------------------------------------------------------------------------
	/**
	 * Get this location's starting position along its path.
	 * @return
	 */
	Double getPosAlongPath();

	// --------------------------------------------------------------------------
	/**
	 * Set this location's starting position along its path.
	 * @param inPoseAlongPath
	 */
	void setPosAlongPath(Double inPoseAlongPath);
	
	// --------------------------------------------------------------------------
	/**
	 * Return the path segment for this location.
	 * @return
	 */
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

	// --------------------------------------------------------------------------
	/**
	 * Compute the first LED position for the item in this location.
	 * @param inItemId
	 * @return
	 */
	Short getFirstLedPosForItemId(final String inItemId);

	// --------------------------------------------------------------------------
	/**
	 * Compute the first LED position for the item in this location.
	 * @param inItemId
	 * @return
	 */
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

	void setPosTypeByStr(String inPosTypeStr);

	void addVertex(Vertex inVertex);

	void removeVertex(Vertex inVertex);

	// --------------------------------------------------------------------------
	/**
	 * Get all of the items contained in this location.
	 * @return
	 */
	Map<String, Item> getItems();

	void setItems(Map<String, Item> inItems);

	void addItem(Item inItem);

	Item getItem(String inItemId);

	void removeItem(String inItemId);

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

	void setLocationId(String inLocationId);

	void addLocation(ISubLocation inLocation);

	// --------------------------------------------------------------------------
	/**
	 * Get a sub location by its location ID.
	 * @param inLocationId
	 * @return
	 */
	ISubLocation getLocation(String inLocationId);

	void removeLocation(String inLocationId);

	// --------------------------------------------------------------------------
	/**
	 * Look for any sub-location by it's ID.
	 * The location ID needs to be a dotted notation where the first octet is a child location of "this" location.
	 * @param inLocationId
	 * @return
	 */
	ILocation getSubLocationById(String inLocationId);

	void setPathSegment(PathSegment inPathSegment);

	// --------------------------------------------------------------------------
	/**
	 * Recompute the path distance of this location (and recursively for all of its child locations).
	 */
	void computePathDistance();

}
