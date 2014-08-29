package com.gadgetworks.codeshelf.multitenancy;

import lombok.Getter;
import lombok.Setter;

public class Shard {
	
	final static int UNDEFINED_SHARD = 0;
	
	@Getter @Setter
	int shardId = UNDEFINED_SHARD;
	
	@Getter @Setter
	String host = "localhost";
	
	@Getter @Setter
	int port = 5432;
	
	@Getter @Setter
	String dbPassword = null;
	
	public Shard() {
	}
	
	@Override
	public String toString() {
		return "shard #"+this.shardId+" on "+this.host;
	}

	public String getShardName() {
		return "shard"+shardId;
	}
}
