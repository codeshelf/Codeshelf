package com.codeshelf.persistence;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

@SuppressWarnings("serial")
public class UtcTimestampTypeDescriptor extends TimestampTypeDescriptor {
	protected static final UtcTimestampTypeDescriptor	INSTANCE		= new UtcTimestampTypeDescriptor();
	private TimeZone									TIMEZONE_UTC	= TimeZone.getTimeZone("UTC");

	@Override
	public <X> ValueBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>(javaTypeDescriptor, this) {
			@Override
			protected void doBind(PreparedStatement stmt, X value, int index, WrapperOptions wrapperOptions) throws SQLException {
				stmt.setTimestamp(index,
					javaTypeDescriptor.unwrap(value, Timestamp.class, wrapperOptions),
					Calendar.getInstance(TIMEZONE_UTC));
			}
		};
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>(javaTypeDescriptor, this) {
			@Override
			protected X doExtract(CallableStatement stmt, String name, WrapperOptions wrapperOptions) throws SQLException {
				return javaTypeDescriptor.wrap(stmt.getTimestamp(name, Calendar.getInstance(TIMEZONE_UTC)), wrapperOptions);
			}

			@Override
			protected X doExtract(CallableStatement stmt, int index, WrapperOptions wrapperOptions) throws SQLException {
				return javaTypeDescriptor.wrap(stmt.getTimestamp(index, Calendar.getInstance(TIMEZONE_UTC)), wrapperOptions);
			}

			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions wrapperOptions) throws SQLException {
				return javaTypeDescriptor.wrap(rs.getTimestamp(name, Calendar.getInstance(TIMEZONE_UTC)), wrapperOptions);
			}
		};
	}

}
