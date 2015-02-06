package com.gadgetworks.codeshelf.platform.persistence;

import java.util.UUID;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Custom Hibernate data type for representing UUIDs according to the dialect being used
 * 
 * @author Ivan C
 *
 */
public class DialectUUIDType extends AbstractSingleColumnStandardBasicType<UUID> {

    /**
	 * 
	 */
	private static final long	serialVersionUID	= -6249771892864698659L;
	private static final SqlTypeDescriptor descriptor;
    static {
    	boolean isPostgres = System.getProperty("db.connectionurl", "").startsWith("jdbc:postgresql");
 
        if(isPostgres) {
            descriptor = PostgresUUIDType.PostgresUUIDSqlTypeDescriptor.INSTANCE;
        } else {
            descriptor = VarcharTypeDescriptor.INSTANCE;
        } 
    }

    public DialectUUIDType() {
        super(DialectUUIDType.descriptor, UUIDTypeDescriptor.INSTANCE);
    }
 
    @Override
    public String getName() {
        return "dialect-uuid";
    }
 
}
