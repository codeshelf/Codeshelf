package com.codeshelf.edi;

import java.sql.Timestamp;
import java.util.Random;
import java.util.UUID;

public class Generator {

	public String generateString() {
		return UUID.randomUUID().toString();
	}
	
	public int generateInt() {
		return new Random(System.currentTimeMillis()).nextInt(99);
	}

	public int generateInt(int min, int max) {
		return new Random(System.currentTimeMillis()).nextInt(max-min) + min;
	}
	
	public Timestamp generatePastDate() {
		return new Timestamp(System.currentTimeMillis() - generateInt());
	}
}
