package com.gadgetworks.codeshelf.multitenancy;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

@Entity
public class Tenant {
	
	@Id
    private UUID persistentId = UUID.randomUUID();
	
	Date createdOn = new Date();
	
	@Getter @Setter
	@Column(unique=true,length=50)
	String name;
	
	@Getter
	@Column(unique=true,columnDefinition = "serial")
	@Generated(GenerationTime.INSERT)
	int tenantId;

	@Getter
	@Column(unique=true,length=36)
	String dbPassword = UUID.randomUUID().toString();
	
	@Getter @Setter
	int shardId = 0;
	
	public Tenant() {
	}
	
	public void setShard(Shard shard) {
		this.shardId = shard.getShardId();
	}
	
	@Override
	public String toString() {
		return "tenant #"+this.tenantId;
	}	
}
