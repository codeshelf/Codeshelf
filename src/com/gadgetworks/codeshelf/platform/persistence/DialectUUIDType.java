package com.gadgetworks.codeshelf.platform.persistence;

import java.io.IOException;
import java.util.Properties;
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

    private static final SqlTypeDescriptor descriptor;
    static {
        Properties properties = new Properties();
        try {
            properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("database.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database properties", e);
        }
 
        if(properties.getProperty("dialect").endsWith("PostgreSQLDialect")) {
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
