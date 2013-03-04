/*******************************************************************************
 *  FlyWeightController
 *  Copyright (c) 2005, 2006, Jeffrey B. Williams, All rights reserved
 *  $Id: NBitIntegerSaTest.java,v 1.1 2013/03/04 04:47:29 jeffw Exp $
 *******************************************************************************/

package com.gadgetworks.flyweight.bitfields;

import junit.framework.TestCase;

import com.gadgetworks.flyweight.bitfields.IllegalBoundsException;
import com.gadgetworks.flyweight.bitfields.NBitInteger;
import com.gadgetworks.flyweight.bitfields.OutOfRangeException;

public class NBitIntegerSaTest extends TestCase {

	public NBitIntegerSaTest(final String inArg0) {
		super(inArg0);
	}

	public static void main(String[] inArgs) {
	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.NBitInteger(int, int)'
	 */
	public void testNBitInteger() {

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.setValue(int)'
	 */
	public final void testConstructor() throws OutOfRangeException {

		@SuppressWarnings("unused")
		NBitInteger testInt1;
		@SuppressWarnings("unused")
		NBitInteger testInt2;

		testInt1 = new NBitInteger((byte) 3, (byte) 1);

		try {
			testInt2 = new NBitInteger((byte) 3, (byte) -1);
			fail();
		} catch (IllegalBoundsException e) {
			fail();
		} catch (OutOfRangeException e) {
			// Expected case.
		}

		try {
			testInt2 = new NBitInteger((byte) 3, (byte) 17);
			fail();
		} catch (IllegalBoundsException e) {
			fail();
		} catch (OutOfRangeException e) {
			// Expected case.
		}

		try {
			testInt2 = new NBitInteger((byte) -1, (byte) 0);
			fail();
		} catch (IllegalBoundsException e) {
			// Expected case.
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			testInt2 = new NBitInteger((byte) 17, (byte) 0);
			fail();
		} catch (IllegalBoundsException e) {
			// Expected case.
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.setValue(int)'
	 */
	public final void testSetValue() {

		try {
			NBitInteger testInt = new NBitInteger((byte) 3, (byte) 1);

			testInt.setValue((byte) 0);
			testInt.setValue((byte) 1);
			testInt.setValue((byte) 7);

			try {
				testInt.setValue((byte) 8);
				fail();
			} catch (OutOfRangeException e) {
				// Expected condition.
			}

			try {
				testInt.setValue((byte) 9);
				fail();
			} catch (OutOfRangeException e) {
				// Expected condition.
			}

			try {
				testInt.setValue((byte) -1);
				fail();
			} catch (OutOfRangeException e) {
				// Expected condition.
			}

			// Expected case.
		} catch (IllegalBoundsException e) {
			fail();
		} catch (OutOfRangeException e) {
			fail();
		}

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getValue()'
	 */
	public final void testGetValue() {

		try {
			// Test with 0, 1, 7
			NBitInteger testInt = new NBitInteger((byte) 3, (byte) 0);
			assertEquals(0, testInt.getValue());

			testInt.setValue((byte) 1);
			assertEquals(1, testInt.getValue());

			testInt.setValue((byte) 7);
			assertEquals(7, testInt.getValue());

		} catch (IllegalBoundsException e) {
			fail();
		} catch (OutOfRangeException e) {
			fail();
		}

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getBitLen()'
	 */
	public void testGetBitLen() {

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getMin()'
	 */
	public final void testGetMin() {
		try {
			NBitInteger testInt = new NBitInteger((byte) 1, (byte) 0);
			assertEquals(0, testInt.getMin());
		} catch (IllegalBoundsException e) {
			fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 2, (byte) 0);
			assertEquals(0, testInt.getMin());
		} catch (IllegalBoundsException e) {
			fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 7, (byte) 0);
			assertEquals(0, testInt.getMin());
		} catch (IllegalBoundsException e) {
			fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

	}

	/*
	 * Test method for 'com.gadgetworks.bitfields.NBitInteger.getMax()'
	 */
	public final void testGetMax() {
		try {
			NBitInteger testInt = new NBitInteger((byte) 1, (byte) 0);
			assertEquals(1, testInt.getMax());
		} catch (IllegalBoundsException e) {
			fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 2, (byte) 0);
			assertEquals(3, testInt.getMax());
		} catch (IllegalBoundsException e) {
			fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 7, (byte) 0);
			assertEquals(127, testInt.getMax());
		} catch (IllegalBoundsException e) {
			fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

		try {
			NBitInteger testInt = new NBitInteger((byte) 8, (byte) 0);
			assertEquals(255, testInt.getMax());
		} catch (IllegalBoundsException e) {
			fail();
			//		} catch (OutOfRangeException e) {
			//			fail();
		}

	}

}
