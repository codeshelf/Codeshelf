/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Point.java,v 1.3 2012/11/03 03:24:35 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import javax.persistence.Embeddable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author jeffw
 *
 */

//@Embeddable
@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class Point {

	@NonNull
	@Getter
	@Setter
	private PositionTypeEnum	posTypeEnum;

	@NonNull
	@Getter
	@Setter
	private Double				x;

	@NonNull
	@Getter
	@Setter
	private Double				y;

	@Getter
	@Setter
	private Double				z;

	public Point(final PositionTypeEnum inPosType, final Double inX, final Double inY, final Double inZ) {
		posTypeEnum = inPosType;
		x = inX;
		y = inY;
		z = inZ;
	}
}
