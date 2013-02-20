/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: QueryFactory.java,v 1.1 2013/02/20 08:28:23 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class QueryFactory extends QueryFactoryABC {

	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.QueryFactoryABC#doCreateQuery(int)
	 */
	protected IQuery doCreateQuery(int inQueryID) {

		IQuery result = null;
		
		QueryTypeEnum queryType = QueryTypeEnum.getQueryTypeEnum(inQueryID);
		
		// Now that we know the command ID, create the proper command.
		switch (queryType) {
			case ACTOR_DESCRIPTOR:
				result = new QueryActorDescriptor();
				break;
			default:
				break;
		}
		
		return result;

	}
}
