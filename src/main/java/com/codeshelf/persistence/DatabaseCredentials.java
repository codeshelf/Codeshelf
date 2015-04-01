package com.codeshelf.persistence;


public interface DatabaseCredentials {
	String getUrl();
	String getUsername();
	String getPassword();
	String getSchemaName();
}
