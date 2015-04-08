package com.codeshelf.security;

import java.util.Collection;
import java.util.Set;


public interface UserContext {
	String getUsername();
	Integer getId();
	
	Collection<? extends String> getRoleNames();
	Set<String> getPermissionStrings();

	boolean isSiteController();
}
