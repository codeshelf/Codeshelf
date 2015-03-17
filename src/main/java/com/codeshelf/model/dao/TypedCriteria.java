package com.codeshelf.model.dao;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.ToString;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@ToString(of={"query"})
public class TypedCriteria {

	@Getter
	private String	query;
	@Getter
	private Map<String, Class<?>>	parameterTypes;

	public TypedCriteria(String query, Map<String, Class<?>> parameterTypes) {
		this.query = query;
		this.parameterTypes = parameterTypes;
	}
		
	public TypedCriteria(String query, String paramName1, Class<?> paramType1) {
		this(query, ImmutableMap.<String, Class<?>>of(paramName1, paramType1));
	}

	public TypedCriteria(String query, String paramName1, Class<?> paramType1,
												  String paramName2, Class<?> paramType2) {
		this(query, ImmutableMap.of(paramName1, paramType1, paramName2, paramType2));

	}

	public TypedCriteria addEqualsRestriction(String fieldName, String parameterName, Class<?> parameterType) {
		// TODO relatively safe but the queries are just strings
		String newQuery = this.query + String.format(" and %s = :%s", fieldName, parameterName);
		HashMap<String, Class<?>> newParameterTypes = Maps.newHashMap(parameterTypes);
		newParameterTypes.put(parameterName, parameterType);
		return new TypedCriteria(newQuery, newParameterTypes);
	}

}