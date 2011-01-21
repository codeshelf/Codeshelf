/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseFactory.java,v 1.2 2011/01/21 01:12:11 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.query;


// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public class ResponseFactory extends ResponseFactoryABC {
	
	/* --------------------------------------------------------------------------
	 * (non-Javadoc)
	 * @see com.gadgetworks.query.QueryFactoryABC#doCreateQuery(int)
	 */
	protected final IResponse doCreateResponse(int inResponseTypeID) {

		IResponse result = null;
		
		ResponseTypeEnum responseType = ResponseTypeEnum.getResponseTypeEnum(inResponseTypeID);
		
		// Now that we know the command ID, create the proper command.
		switch (responseType) {
			case ACTOR_DESCRIPTOR:
				result = new ResponseActorDescriptor();
				break;
			case ACTOR_KVP:
				result = new ResponseActorKVP();
				break;
			default:
				break;
		}
		
		return result;

	}

}
