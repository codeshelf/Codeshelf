package com.codeshelf.api;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;

import com.codeshelf.model.domain.IDomainObject;
import com.codeshelf.util.ConverterProvider;
import com.google.common.base.Strings;
import com.sun.jersey.api.representation.Form;

public class ParameterUtils {
	
	public static Map<String, String> toMapOfFirstValues(MultivaluedMap<String, String> functionParams) {
		HashMap<String, String> firstValues = new HashMap<>();
		for (Entry<String, List<String>> entry : functionParams.entrySet()) {
			String value = functionParams.getFirst(entry.getKey());
			firstValues.put(entry.getKey(), value);
		}
		return firstValues;
	}

	public static String getDomainId(MultivaluedMap<String, String> params) {
		return params.getFirst("domainId");
	}

	public static Integer getInteger(MultivaluedMap<String, String> params, String name) {
		String stringValue =  params.getFirst(name);
		if (stringValue != null) {
			return new Integer(stringValue);
		} else {
			return null;
		}
	}

	public static <T> T populate(T bean, MultivaluedMap<String, String> params) throws ReflectiveOperationException {
		BeanUtilsBean beanUtils = new BeanUtilsBean(new ConverterProvider().get());
		for (String key : params.keySet()) {
			String value = Strings.emptyToNull(params.getFirst(key));
			PropertyDescriptor propertyDescriptor = beanUtils.getPropertyUtils().getPropertyDescriptor(bean, key);
			if (isWriteable(propertyDescriptor)) {
				beanUtils.copyProperty(bean, key, value);
			}
		}
		return bean;
	}
	public static MultivaluedMap<String, String> fromMap(Map<String, String> of) {
		Form form = new Form();
		of.entrySet().stream().forEach(e -> 
			form.add(e.getKey(), e.getValue()));
		return form;
		
	}

	public static MultivaluedMap<String, String> fromObject(IDomainObject bean) throws ReflectiveOperationException {
		Form form = new Form();
		
		PropertyUtilsBean propertyUtils = new PropertyUtilsBean();
		PropertyDescriptor[] propertyDescriptors = propertyUtils.getPropertyDescriptors(bean);
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			if (isWriteable(propertyDescriptor)) {
				String name = propertyDescriptor.getName();
				form.add(propertyDescriptor.getName(), propertyUtils.getProperty(bean, name));
			}
		}
		return form;
	}

	private static boolean isWriteable(PropertyDescriptor propertyDescriptor) {
		return propertyDescriptor != null && propertyDescriptor.getWriteMethod() != null;
	}


}
