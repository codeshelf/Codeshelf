package com.codeshelf.platform.persistence;


public interface DatabaseCredentials {
	String getUrl();
	String getUsername();
	String getPassword();
	String getSchemaName();
}
