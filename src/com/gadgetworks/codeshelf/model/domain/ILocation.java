/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: ILocation.java,v 1.4 2013/04/09 07:58:20 jeffw Exp $
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

	 Double getPathDistance();

	 void setPathDistance(Double inPathDistance);

	 PathSegment getPathSegment();

	 Organization getParentOrganization();

	 void setParentOrganization(Organization inParentOrganization);

	 LedController getLedController();

	 void setLedController(LedController inLedController);

	 Integer getLedChannel();

	 void setLedChannel(Integer inLedChannel);

	 Integer getFirstLedPos();

	 void setFirstLedPos(Integer inFirstLedPos);

	 Integer getLastLedPos();

	 void setLastLedPos(Integer inLastLedPos);

	 List<Vertex> getVertices();

	 void setVertices(List<Vertex> inVertices);

//	 Map<String, ISubLocation> getLocations();
//
//	 void setLocations(Map<String, ISubLocation> inLocations);

	 Map<String, Item> getItems();

	 void setItems(Map<String, Item> inItems);

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

	 void setPosTypeByStr(String inPosTypeStr);

	 void addVertex(Vertex inVertex);

	 void removeVertex(Vertex inVertex);

	 void addItem(String inItemId, Item inItem);

	 Item getItem(String inItemId);

	 void removeItem(String inItemId);

}
