package jbase.lib;

import org.eclipse.xtext.xbase.lib.Inline;
import org.eclipse.xtext.xbase.lib.Pure;

/**
 * Java unary plus, including promotion of byte, short and char operands to int.
 * The Xbase numeric extensions only provide binary plus overloads.
 */
public final class JbaseNumericExtensions {

	private JbaseNumericExtensions() {
	}

	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static int operator_plus(final int value) {
		return +value;
	}

	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static long operator_plus(final long value) {
		return +value;
	}

	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static float operator_plus(final float value) {
		return +value;
	}

	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static double operator_plus(final double value) {
		return +value;
	}
}
