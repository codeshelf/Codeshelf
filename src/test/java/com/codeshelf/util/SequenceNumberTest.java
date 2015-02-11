package com.codeshelf.util;

import org.junit.Assert;
import org.junit.Test;

public class SequenceNumberTest {

	@Test
	public void testSequenceNumberGeneration() {
		long prevSeq=0;
		long seq=0;
		for (int i=0;i<10000;i++) {
			seq = SequenceNumber.generate();
			Assert.assertTrue(seq>prevSeq);
			prevSeq = seq;
		}
	}
}
