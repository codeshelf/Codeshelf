/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: NBitIntegerSaTest.java,v 1.2 2013/07/22 04:30:36 jeffw Exp $
 *******************************************************************************/

package com.codeshelf.flyweight.bitfields;

import org.junit.Assert;
import org.junit.Test;

import com.codeshelf.testframework.MinimalTest;

public class NBitIntegerSaTest extends MinimalTest {

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.NBitInteger(int, int)'
	 */
	public void testNBitInteger() {

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.setValue(int)'
	 */
	@Test
	public final void testConstructor() throws OutOfRangeException {

		@SuppressWarnings("unused")
		NBitInteger testInt2;

		new NBitInteger((byte) 3, (byte) 1);

		try {
			testInt2 = new NBitInteger((byte) 3, (byte) -1);
			Assert.fail();
		} catch (IllegalBoundsException e) {
			Assert.fail();
		} catch (OutOfRangeException e) {
			// Expected case.
		}

		try {
			testInt2 = new NBitInteger((byte) 3, (byte) 17);
			Assert.fail();
		} catch (IllegalBoundsException e) {
			Assert.fail();
		} catch (OutOfRangeException e) {
			// Expected case.
		}

		try {
			testInt2 = new NBitInteger((byte) -1, (byte) 0);
			Assert.fail();
		} catch (IllegalBoundsException e) {
			// Expected case.
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			testInt2 = new NBitInteger((byte) 17, (byte) 0);
			Assert.fail();
		} catch (IllegalBoundsException e) {
			// Expected case.
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.setValue(int)'
	 */
	@Test
	public final void testSetValue() {

		try {
			NBitInteger testInt = new NBitInteger((byte) 3, (byte) 1);

			testInt.setValue((byte) 0);
			testInt.setValue((byte) 1);
			testInt.setValue((byte) 7);

			try {
				testInt.setValue((byte) 8);
				Assert.fail();
			} catch (OutOfRangeException e) {
				// Expected condition.
			}

			try {
				testInt.setValue((byte) -1);
				Assert.fail();
			} catch (OutOfRangeException e) {
				// Expected condition.
			}

			// Expected case.
		} catch (IllegalBoundsException e) {
			Assert.fail();
		} catch (OutOfRangeException e) {
			Assert.fail();
		}

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getValue()'
	 */
	@Test
	public final void testGetValue() {

		try {
			// Test with 0, 1, 7
			NBitInteger testInt = new NBitInteger((byte) 3, (byte) 0);
			Assert.assertEquals(0, testInt.getValue());

			testInt.setValue((byte) 1);
			Assert.assertEquals(1, testInt.getValue());

			testInt.setValue((byte) 7);
			Assert.assertEquals(7, testInt.getValue());

		} catch (IllegalBoundsException e) {
			Assert.fail();
		} catch (OutOfRangeException e) {
			Assert.fail();
		}
	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getValue()' for an uninitialized value.
	 */
	@Test
	public final void testUnitialized() {

		try {
			NBitInteger testInt = new NBitInteger((byte) 3);

			try {
				@SuppressWarnings("unused")
				short value = testInt.getValue();
				Assert.fail();
			} catch (OutOfRangeException e) {
				// Expected condition.
			}

			testInt.setValue((byte) 1);
			Assert.assertEquals(1, testInt.getValue());

		} catch (IllegalBoundsException e) {
			Assert.fail();
		} catch (OutOfRangeException e) {
			Assert.fail();
		}
	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getBitLen()'
	 */
	@Test
	public void testGetBitLen() {

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getMin()'
	 */
	@Test
	public final void testGetMin() {
		try {
			NBitInteger testInt = new NBitInteger((byte) 1, (byte) 0);
			Assert.assertEquals(0, testInt.getMin());
		} catch (IllegalBoundsException e) {
			Assert.fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 2, (byte) 0);
			Assert.assertEquals(0, testInt.getMin());
		} catch (IllegalBoundsException e) {
			Assert.fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 7, (byte) 0);
			Assert.assertEquals(0, testInt.getMin());
		} catch (IllegalBoundsException e) {
			Assert.fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getMax()'
	 */
	@Test
	public final void testGetMax() {
		try {
			NBitInteger testInt = new NBitInteger((byte) 1, (byte) 0);
			Assert.assertEquals(1, testInt.getMax());
		} catch (IllegalBoundsException e) {
			Assert.fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 2, (byte) 0);
			Assert.assertEquals(3, testInt.getMax());
		} catch (IllegalBoundsException e) {
			Assert.fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 7, (byte) 0);
			Assert.assertEquals(127, testInt.getMax());
		} catch (IllegalBoundsException e) {
			Assert.fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 8, (byte) 0);
			Assert.assertEquals(255, testInt.getMax());
		} catch (IllegalBoundsException e) {
			Assert.fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

	}

}
