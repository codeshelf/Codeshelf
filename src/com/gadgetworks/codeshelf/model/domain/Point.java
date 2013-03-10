/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Point.java,v 1.5 2013/03/10 08:58:43 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.domain;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;

/**
 * @author jeffw
 *
 */

//@Embeddable
@JsonAutoDetect(getterVisibility = Visibility.NONE)
@ToString
public class Point {

	@NonNull
	@Getter
	@Setter
	protected PositionTypeEnum	posTypeEnum;

	@NonNull
	@Getter
	@Setter
	protected Double			x;

	@NonNull
	@Getter
	@Setter
	protected Double			y;

	@Getter
	@Setter
	protected Double			z;

	public Point(final PositionTypeEnum inPosType, final Double inX, final Double inY, final Double inZ) {
		posTypeEnum = inPosType;
		x = inX;
		y = inY;
		z = inZ;
	}
}
