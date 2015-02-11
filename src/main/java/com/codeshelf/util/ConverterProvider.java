package com.codeshelf.util;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.AbstractConverter;

import com.codeshelf.model.OrderTypeEnum;
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
			long timeInMillis = -1;
			if (inValue instanceof String) {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
				Date time = format.parse(String.valueOf(inValue));
				timeInMillis = time.getTime();
			} else if (inValue instanceof Number) {
				timeInMillis = Long.valueOf(String.valueOf(inValue));
			} 
			
			if (timeInMillis >= 0) {
				return new Timestamp(timeInMillis);
			}
			else {
				return null;
			}
		}

		@Override
		protected Class getDefaultType() {
			return Timestamp.class;
		}
	}
}
