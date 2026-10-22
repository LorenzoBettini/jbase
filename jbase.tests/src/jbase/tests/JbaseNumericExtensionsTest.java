package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import jbase.lib.JbaseNumericExtensions;
import org.junit.Test;

public class JbaseNumericExtensionsTest {

	@Test
	public void integralValuesArePreserved() {
		assertEquals(Integer.MIN_VALUE, JbaseNumericExtensions.operator_plus(Integer.MIN_VALUE));
		assertEquals(Long.MAX_VALUE, JbaseNumericExtensions.operator_plus(Long.MAX_VALUE));
	}

	@Test
	public void floatingPointValuesArePreserved() {
		assertEquals(1.5f, JbaseNumericExtensions.operator_plus(1.5f), 0.0f);
		assertEquals(1.5, JbaseNumericExtensions.operator_plus(1.5), 0.0);
		assertEquals(Float.floatToRawIntBits(-0.0f),
			Float.floatToRawIntBits(JbaseNumericExtensions.operator_plus(-0.0f)));
		assertEquals(Double.doubleToRawLongBits(-0.0),
			Double.doubleToRawLongBits(JbaseNumericExtensions.operator_plus(-0.0)));
		assertTrue(Float.isNaN(JbaseNumericExtensions.operator_plus(Float.NaN)));
		assertTrue(Double.isNaN(JbaseNumericExtensions.operator_plus(Double.NaN)));
		assertEquals(Double.POSITIVE_INFINITY,
			JbaseNumericExtensions.operator_plus(Double.POSITIVE_INFINITY), 0.0);
	}
}
