/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DomainObjectMap.java,v 1.1 2012/10/24 01:00:59 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model;

import java.util.HashMap;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;

/**
 * @author jeffw
 *
 */
public class DomainObjectMap<V extends IDomainObject> extends HashMap<String, V> implements IDomainObjectMap<V> {

	private static final long	serialVersionUID	= 459857573940281452L;

	private IDomainObject				mParentDomainObject;

	public DomainObjectMap(final IDomainObject inParentDomainObject) {
		super();
		mParentDomainObject = inParentDomainObject;
	}

	// --------------------------------------------------------------------------
	/**
	 * @param inValue
	 * @return
	 */
	public final V put(V inValue) {
		return super.put(inValue.getFullDomainId(), inValue);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final V put(String inKey, V inValue) {
		return super.put(inValue.getFullDomainId(), inValue);
	}

	// --------------------------------------------------------------------------
	/**
	 * When we look for a domain object by it's ID short ID we need to expand it to the full domainId for the lookup.
	 * @param inKey
	 * @return
	 */
	public final V get(String inKey) {
		// We need to "cook" the key, so that it's a fully qualified domainId.
		inKey = inKey.toUpperCase();
		if (!(inKey.startsWith(mParentDomainObject.getFullDomainId()))) {
			inKey = mParentDomainObject.getFullDomainId() + "." + inKey;
		}

		return super.get(inKey);
	}

	// --------------------------------------------------------------------------
	/* (non-Javadoc)
	 * @see java.util.HashMap#get(java.lang.Object)
	 */
	@Override
	public final V get(Object inKey) {
		if (inKey instanceof String) {
			return super.get((String) inKey);
		} else {
			return null;
		}
	}
}
