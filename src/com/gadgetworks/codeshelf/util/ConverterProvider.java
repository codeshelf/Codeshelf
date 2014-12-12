package com.gadgetworks.codeshelf.util;

import java.sql.Timestamp;
import java.util.UUID;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.AbstractConverter;

import com.gadgetworks.codeshelf.model.OrderTypeEnum;
import com.google.inject.Provider;

public class ConverterProvider implements Provider<ConvertUtilsBean> {

	@Override
	public ConvertUtilsBean get() {
		ConvertUtilsBean convertUtils = new ConvertUtilsBean();
		convertUtils.register(new UUIDConverter(), UUID.class);
		convertUtils.register(new TimestampConverter(), Timestamp.class);
		convertUtils.register(new EnumConverter(), OrderTypeEnum.class);
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
			return UUID.class;
		}
	}

	
	@SuppressWarnings("rawtypes")
	private static class EnumConverter extends AbstractConverter {
		@SuppressWarnings("unchecked")
		@Override
		protected Object convertToType(Class inClass, Object inValue) throws Throwable {
			return Enum.valueOf(inClass, String.valueOf(inValue));
		}

		@Override
		protected Class getDefaultType() {
			return Enum.class;
		}
	}
	
	@SuppressWarnings("rawtypes")
	private static class TimestampConverter extends AbstractConverter {
		@Override
		protected Object convertToType(Class inClass, Object inValue) throws Throwable {
			return new Timestamp(Long.valueOf(String.valueOf(inValue)));
		}

		@Override
		protected Class getDefaultType() {
			return Timestamp.class;
		}
	}
}
