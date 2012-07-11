/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2011, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoProvider.java,v 1.4 2012/07/11 07:15:42 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.gadgetworks.codeshelf.model.persist.PersistABC;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * @author jeffw
 *
 */
public class DaoProvider  implements IDaoProvider {

	private Injector	mInjector;

	@Inject
	public DaoProvider(final Injector inInjector) {
		mInjector = inInjector;
	}

	public final <T extends PersistABC> ITypedDao<T> getDaoInstance(final Class<T> inDomainObjectClass) {

		final ParameterizedType parameterizedDaoType = new ParameterizedType() {

			public Type[] getActualTypeArguments() {
				return new Type[] { inDomainObjectClass };
			}

			public Type getOwnerType() {
				return null;
			}

			public Type getRawType() {
				return ITypedDao.class;
			}
		};

		TypeLiteral<?> type = TypeLiteral.get(parameterizedDaoType);

		if (mInjector.findBindingsByType(type).size() > 0) {
			return (ITypedDao<T>) mInjector.findBindingsByType(type).get(0).getProvider().get();
		}

		return null;
	}
}
