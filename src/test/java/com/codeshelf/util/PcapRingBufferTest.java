package com.codeshelf.util;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.testframework.MinimalTest;

public class PcapRingBufferTest extends MinimalTest {
	@Test
	public void fillAndEmpty() {
		PcapRingBuffer pb = new PcapRingBuffer(129,16); // enough room for slightly more than 3.5 packets (16 hdr+16 buffer)

		try {
			byte[] packet = new byte[16];
			for(int rep=0; rep<13; rep++) {
				for(int i=0;i<5;i++) {
					Arrays.fill(packet, (byte)i);
					pb.put(new PcapRecord(packet));
				}
				PcapRecord[] recs = pb.records();
				Assert.assertEquals(recs.length, 3);
				Assert.assertTrue(pb.get().getPacket()[0] == (byte)0x02);
				Assert.assertTrue(recs[0].getPacket()[0] == (byte)0x02);
				Assert.assertTrue(pb.get().getPacket()[0] == (byte)0x03);
				Assert.assertTrue(recs[1].getPacket()[0] == (byte)0x03);
				Assert.assertTrue(pb.get().getPacket()[0] == (byte)0x04);
				Assert.assertTrue(recs[2].getPacket()[0] == (byte)0x04);
				Assert.assertNull(pb.get());
			}
		} catch (IOException e) {
			Assert.fail();
		}
	}
}
