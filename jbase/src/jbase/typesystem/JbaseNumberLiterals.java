package jbase.typesystem;

import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.typesystem.computation.NumberLiterals;

/**
 * Handles Java hexadecimal floating-point literals alongside Xbase numeric literals.
 */
public class JbaseNumberLiterals extends NumberLiterals {

	/**
	 * Returns the primitive type selected by a hexadecimal literal's suffix.
	 *
	 * @param literal the numeric literal
	 * @return {@code float} for an {@code f/F} suffix, otherwise {@code double}
	 */
	@Override
	public Class<? extends Number> getJavaType(final XNumberLiteral literal) {
		if (!isHexadecimalFloatingPoint(literal)) {
			return super.getJavaType(literal);
		}
		final var value = literal.getValue();
		return value.endsWith("f") || value.endsWith("F") ? Float.TYPE : Double.TYPE;
	}

	/**
	 * Parses a hexadecimal floating-point literal while leaving other number formats to Xbase.
	 *
	 * @param literal the numeric literal
	 * @param numberType the literal's inferred Java number type
	 * @return the parsed numeric value
	 */
	@Override
	public Number numberValue(final XNumberLiteral literal, final Class<?> numberType) {
		if (!isHexadecimalFloatingPoint(literal)) {
			return super.numberValue(literal, numberType);
		}
		final var value = literal.getValue().replace("_", "");
		return numberType == Float.TYPE ? Float.valueOf(value) : Double.valueOf(value);
	}

	/**
	 * Preserves Java syntax for hexadecimal floating-point literals in generated source.
	 *
	 * @param literal the numeric literal
	 * @param formattingFlag a flag used by Xbase when formatting numeric literals
	 * @return the original hexadecimal literal or Xbase's ordinary Java spelling
	 */
	@Override
	public String toJavaLiteral(final XNumberLiteral literal, final boolean formattingFlag) {
		return isHexadecimalFloatingPoint(literal) ? literal.getValue()
			: super.toJavaLiteral(literal, formattingFlag);
	}

	private boolean isHexadecimalFloatingPoint(final XNumberLiteral literal) {
		final var value = literal.getValue();
		return (value.startsWith("0x") || value.startsWith("0X"))
			&& (value.contains("p") || value.contains("P"));
	}
}
