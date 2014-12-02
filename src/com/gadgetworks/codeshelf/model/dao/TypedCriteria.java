package com.gadgetworks.codeshelf.model.dao;

import java.util.Map;

import lombok.Getter;

import com.google.common.collect.ImmutableMap;


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

}