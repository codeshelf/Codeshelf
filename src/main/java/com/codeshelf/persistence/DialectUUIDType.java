package com.codeshelf.persistence;

import java.util.UUID;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor;

/**
 * Custom Hibernate data type for representing UUIDs according to the dialect being used
 * 
 * Uses "shard default" settings!
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
    	// NOTE: this is using the "shard default" settings as system-wide!
    	boolean isPostgres = System.getProperty("shard.default.db.url", "").startsWith("jdbc:postgresql");
 
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
