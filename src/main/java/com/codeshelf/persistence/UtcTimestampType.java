package com.codeshelf.persistence;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.LiteralType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.VersionType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JdbcTimestampTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.TimestampTypeDescriptor;

import java.sql.*;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

@SuppressWarnings("serial")
public class UtcTimestampType extends AbstractSingleColumnStandardBasicType<Date> implements VersionType<Date>, LiteralType<Date> {
	protected static UtcTimestampType	INSTANCE	= new UtcTimestampType();

	public UtcTimestampType() {
		super(UtcTimestampTypeDescriptor.INSTANCE, JdbcTimestampTypeDescriptor.INSTANCE);
	}

	public String getName() {
		return TimestampType.INSTANCE.getName();
	}

	@Override
	public String[] getRegistrationKeys() {
		return TimestampType.INSTANCE.getRegistrationKeys();
	}

	public Date next(Date current, SessionImplementor session) {
		return TimestampType.INSTANCE.next(current, session);
	}

	public Date seed(SessionImplementor session) {
		return TimestampType.INSTANCE.seed(session);
	}

	public Comparator<Date> getComparator() {
		return TimestampType.INSTANCE.getComparator();
	}

	public String objectToSQLString(Date value, Dialect dialect) throws Exception {
		return TimestampType.INSTANCE.objectToSQLString(value, dialect);
	}

	public Date fromStringValue(String xml) throws HibernateException {
		return TimestampType.INSTANCE.fromStringValue(xml);
	}
}