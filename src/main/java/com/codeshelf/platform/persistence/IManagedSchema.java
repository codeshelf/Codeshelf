package com.codeshelf.platform.persistence;

public interface IManagedSchema {

	String getUrl();

	String getUsername();

	String getPassword();

	String getSchemaName();

	String getHibernateConfigurationFilename();

	String getChangeLogName();
}
