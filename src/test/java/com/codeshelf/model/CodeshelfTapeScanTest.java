package com.codeshelf.model;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.testframework.MinimalTest;

public class CodeshelfTapeScanTest extends MinimalTest {
	@Test
	public void parseCodeshelfTape() {

		// won't parse bad tape
		CodeshelfTape tape = CodeshelfTape.scan("");
		Assert.assertNull(tape);
		tape = CodeshelfTape.scan("%0000000000000"); // too long
		Assert.assertNull(tape);
		tape = CodeshelfTape.scan("%00000000000"); // too short
		Assert.assertNull(tape);
		tape = CodeshelfTape.scan("%OOOOOOOOOOOO"); // invalid character
		Assert.assertNull(tape);
		tape = CodeshelfTape.scan("$000000000000"); // invalid character
		Assert.assertNull(tape);
		tape = CodeshelfTape.scan("%000000000001"); // invalid reserved digit
		Assert.assertNull(tape);

		// parses good tape
		tape = CodeshelfTape.scan("%999999999990"); 
		Assert.assertEquals(99999999, tape.getGuid());
		Assert.assertEquals(99, tape.getManufacturerId());
		Assert.assertEquals(999999, tape.getManufacturerSerialNumber());
		Assert.assertEquals(999, tape.getOffsetCm());
		tape = CodeshelfTape.scan("%000003001500"); 
		Assert.assertEquals(300, tape.getGuid());
		Assert.assertEquals(0, tape.getManufacturerId());
		Assert.assertEquals(300, tape.getManufacturerSerialNumber());
		Assert.assertEquals(150, tape.getOffsetCm());
		
		// get tape ID from base32
		Assert.assertEquals(-1,CodeshelfTape.extractGuid("")); // empty
		Assert.assertEquals(-1,CodeshelfTape.extractGuid("001")); // too short
		Assert.assertEquals(-1,CodeshelfTape.extractGuid("0XTNKGZ")); // too long
		Assert.assertEquals(-1,CodeshelfTape.extractGuid("ERRUR")); // invalid character U
		Assert.assertEquals(99999999,CodeshelfTape.extractGuid("2ZBR7Z"));
		Assert.assertEquals(5412005,CodeshelfTape.extractGuid("55555"));
		Assert.assertEquals(0,CodeshelfTape.extractGuid("0000"));
		Assert.assertEquals(393249,CodeshelfTape.extractGuid("C011"));
		Assert.assertEquals(393249,CodeshelfTape.extractGuid("Coil"));
		Assert.assertEquals(393249,CodeshelfTape.extractGuid("COII"));
		
		// convert base32 to tape ID
		Assert.assertEquals("2ZBR7Z",CodeshelfTape.intToBase32(99999999));
		Assert.assertEquals("55555",CodeshelfTape.intToBase32(5412005));
		Assert.assertEquals("0000",CodeshelfTape.intToBase32(0));
		Assert.assertEquals("C011",CodeshelfTape.intToBase32(393249));		
	}
}
