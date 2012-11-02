/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Point.java,v 1.2 2012/11/02 20:57:13 jeffw Exp $
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

	public Point(final PositionTypeEnum inPosType, final Double inX, final double inY) {
		posTypeEnum = inPosType;
		x = inX;
		y = inY;
	}

}
