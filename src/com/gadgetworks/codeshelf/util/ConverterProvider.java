package com.gadgetworks.codeshelf.util;

import java.util.UUID;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.AbstractConverter;

import com.google.inject.Provider;

public class ConverterProvider implements Provider<ConvertUtilsBean> {

	@Override
	public ConvertUtilsBean get() {
		ConvertUtilsBean convertUtils = new ConvertUtilsBean();
		convertUtils.register(new UUIDConverter(), UUID.class);
		return convertUtils;
	}
	
	@SuppressWarnings("rawtypes")
	private static class UUIDConverter extends AbstractConverter {
		@Override
		protected Object convertToType(Class arg0, Object inValue) throws Throwable {
			return UUID.fromString(String.valueOf(inValue));
		}

		@Override
		protected Class getDefaultType() {
			// TODO Auto-generated method stub
			return UUID.class;
		}
	}

}
