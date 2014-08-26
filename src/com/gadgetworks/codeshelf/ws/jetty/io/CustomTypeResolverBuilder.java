package com.gadgetworks.codeshelf.ws.jetty.io;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

public class CustomTypeResolverBuilder extends DefaultTypeResolverBuilder
{
	private static final long	serialVersionUID	= -4214172376734797907L;

	public CustomTypeResolverBuilder() {
        super(DefaultTyping.NON_FINAL);
    }

    @Override
    public boolean useForType(JavaType t)
    {
        if (t.getRawClass().getName().startsWith("com.gadgetworks")) {
            return true;
        }
        if (t.getRawClass().equals(Object.class)) {
            return true;
        }
        return false;
    }
}
