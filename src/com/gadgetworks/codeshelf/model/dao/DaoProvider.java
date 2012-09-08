/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: DaoProvider.java,v 1.8 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.model.dao;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gadgetworks.codeshelf.model.domain.IDomainObject;
import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * @author jeffw
 *
 */
public class DaoProvider implements IDaoProvider {

	private Injector	mInjector;

	@Inject
	public DaoProvider(final Injector inInjector) {
		mInjector = inInjector;
	}

	public final <T extends IDomainObject> ITypedDao<T> getDaoInstance(final Class<T> inDomainObjectClass) {

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

	public final <T extends IDomainObject> List<ITypedDao<T>> getAllDaos() {

		List<ITypedDao<T>> result = new ArrayList<ITypedDao<T>>();

		Map<Key<?>, Binding<?>> bindings = mInjector.getBindings();

		for (Binding<?> binding : bindings.values()) {
			if (GenericDaoABC.class.isAssignableFrom(binding.getProvider().get().getClass())) {
				result.add((ITypedDao<T>) binding.getProvider().get());
			}
		}

		return result;
	}
}
