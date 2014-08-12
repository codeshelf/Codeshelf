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
import lombok.Setter;
import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author jeffw
 *
 */

@Embeddable
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(doNotUseGetters=true)
public class Point {

	@Column(nullable = false)
	@Enumerated(value = EnumType.STRING)
	@NonNull
	@Getter
	@Setter
	@JsonProperty
	private PositionTypeEnum	posTypeEnum;

	@NonNull
	@Getter
	@Setter
	@JsonProperty
	private Double				x;

	@NonNull
	@Getter
	@Setter
	@JsonProperty
	private Double				y;

	@NonNull
	@Getter
	@Setter
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

	public final void add(final Point inAddPoint) {
		if (inAddPoint.getPosTypeEnum().equals(posTypeEnum)) {
			x += inAddPoint.getX();
			y += inAddPoint.getY();
			z += inAddPoint.getZ();
		}
	}

	public final void translateX(final Double inOffset) {
		x += inOffset;
	}

	public final void translateY(final Double inOffset) {
		y += inOffset;
	}

	public final void translateZ(final Double inOffset) {
		z += inOffset;
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

}
