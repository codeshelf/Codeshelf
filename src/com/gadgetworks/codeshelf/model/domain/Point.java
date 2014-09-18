/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Point.java,v 1.7 2013/03/16 08:03:08 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author jeffw
 *
 */

@Embeddable
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE)
@ToString(doNotUseGetters = true)
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property = "className")
@EqualsAndHashCode(doNotUseGetters=true)
public class Point {

	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@NonNull
	@Getter
	@JsonProperty
	private PositionTypeEnum	posTypeEnum;

	@NonNull
	@Getter
	@JsonProperty
	private Double				x;

	@NonNull
	@Getter
	@JsonProperty
	private Double				y;

	@NonNull
	@Getter
	@JsonProperty
	private Double				z;

	public Point() {

	}

	public Point(final PositionTypeEnum inPosType, final Double inX, final Double inY, final Double inZ) {
		posTypeEnum = inPosType;
		x = (inX == null) ? 0.0 : inX;
		y = (inY == null) ? 0.0 : inY;
		z = (inZ == null) ? 0.0 : inZ;
	}

	public Point(final String inPosTypeStr, final Double inX, final Double inY, final Double inZ) {
		posTypeEnum = PositionTypeEnum.valueOf(inPosTypeStr);
		x = (inX == null) ? 0.0 : inX;
		y = (inY == null) ? 0.0 : inY;
		z = (inZ == null) ? 0.0 : inZ;
	}

	public Point(final Point inClonePoint) {
		posTypeEnum = inClonePoint.getPosTypeEnum();
		x = inClonePoint.getX();
		y = inClonePoint.getY();
		z = inClonePoint.getZ();
	}

	public static PositionTypeEnum getPosTypeByStr(String inPosTypeStr) {
		return PositionTypeEnum.valueOf(inPosTypeStr);
	}

	public static Point getZeroPoint() {
		return new Point(PositionTypeEnum.METERS_FROM_PARENT, 0.0, 0.0, 0.0);
	}

	public final Point add(final Point inAddPoint) {
		if (inAddPoint.getPosTypeEnum().equals(posTypeEnum)) {
			return add(inAddPoint.getX(), inAddPoint.getY(), inAddPoint.getZ());
		}
		else {
			throw new IllegalArgumentException("inAddPoint is not the same point type: " + inAddPoint.getPosTypeEnum());
		}
	}

	/**
	 * BigDecimal style, immutable add
	 */
	public final Point add(double inX, double inY) {
		return add(inX, inY, 0);
	}
	
	public final Point add(double inX, double inY, double inZ) {
		return new Point(posTypeEnum, x+inX, y+inY, z+inZ);
	}

	// These are legacy from the front-end GUI.
	// We need to change the GUI to send a Point object to create a location.
	public final void setAnchorPosX(Double inAnchorPosX) {
		x = inAnchorPosX;
	}

	public final void setAnchorPosY(Double inAnchorPosY) {
		y = inAnchorPosY;
	}

	public final void setAnchorPosZ(Double inAnchorPosZ) {
		z = inAnchorPosZ;
	}

	public Double distance(final Point inPoint) {
		Double xDiff = getX() - inPoint.getX();
		Double yDiff = getY() - inPoint.getY();
		return Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
	}
	
	public void setX(Double x) {
		this.x = x;
	}
	
	public void setY(Double y) {
		this.y = y;
	}
	
	public void setZ(Double z) {
		this.z = z;
	}
	
	public void setPosTypeEnum(PositionTypeEnum posTypeEnum) {
		this.posTypeEnum = posTypeEnum;
	}

}
