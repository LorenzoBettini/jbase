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

	/**
	 * Applies Java unary plus to an {@code int} operand. Byte, short, and char operands
	 * use this overload after promotion.
	 *
	 * @param value the operand after any required primitive promotion or unboxing
	 * @return {@code +value}, with type {@code int}
	 */
	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static int operator_plus(final int value) {
		return +value;
	}

	/**
	 * Applies Java unary plus to a {@code long} operand.
	 *
	 * @param value the operand after any required primitive promotion or unboxing
	 * @return {@code +value}, with type {@code long}
	 */
	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static long operator_plus(final long value) {
		return +value;
	}

	/**
	 * Applies Java unary plus to a {@code float} operand.
	 *
	 * @param value the operand after any required primitive promotion or unboxing
	 * @return {@code +value}, with type {@code float}
	 */
	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static float operator_plus(final float value) {
		return +value;
	}

	/**
	 * Applies Java unary plus to a {@code double} operand.
	 *
	 * @param value the operand after any required primitive promotion or unboxing
	 * @return {@code +value}, with type {@code double}
	 */
	@Pure
	@Inline(value = "(+$1)", constantExpression = true)
	public static double operator_plus(final double value) {
		return +value;
	}
}
