/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: ResponseFactory.java,v 1.3 2011/01/21 04:25:54 jeffw Exp $
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
	protected final IResponse doCreateResponse(ResponseTypeEnum inResponseType) {

		IResponse result = null;
		
		// Now that we know the command ID, create the proper command.
		switch (inResponseType) {
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
