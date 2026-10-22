package jbase.conversion;

import java.math.BigInteger;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;

/** Maps Java radix literals to Xbase's hexadecimal model representation. */
public class JbaseNumberValueConverter extends AbstractValueConverter<String> {

	/**
	 * Converts Java binary, octal, and hexadecimal integer literals to Xbase model spelling.
	 * Separators are removed for radix conversion, and Java long suffixes become
	 * {@code #L}. Decimal literals and existing Xbase notation retain their source spelling.
	 *
	 * @param string the complete numeric token
	 * @param node the source node to associate with a conversion error
	 * @return the model spelling, using hexadecimal notation for converted radix literals
	 * @throws ValueConverterException if the digits of a converted integer are invalid
	 */
	@Override
	public String toValue(final String string, final INode node) {
		final var value = string.replace("_", "");
		final var radix = radix(value);
		if (radix == 10) {
			return string;
		}
		final var isLong = value.endsWith("l") || value.endsWith("L");
		final var end = value.length() - (isLong ? 1 : 0);
		final var digits = value.substring(radix == 8 ? 0 : 2, end);
		try {
			final var number = new BigInteger(digits, radix);
			final var hexadecimal = radix == 16 ? value.substring(0, end)
				: "0x" + number.toString(16);
			return hexadecimal + (isLong ? "#L" : "");
		} catch (NumberFormatException e) {
			throw new ValueConverterException("Invalid integer literal: " + string, node, e);
		}
	}

	private int radix(final String value) {
		if (value.startsWith("0b") || value.startsWith("0B")) {
			return 2;
		}
		if ((value.startsWith("0x") || value.startsWith("0X")) && !value.contains("#")) {
			return 16;
		}
		return value.matches("0\\d+[lL]?") ? 8 : 10;
	}

	/**
	 * Converts an Xbase long suffix to its Java spelling for serialization.
	 * The model's hexadecimal representation is retained.
	 *
	 * @param value the numeric model spelling
	 * @return the spelling with {@code #L} or {@code #l} replaced by its Java suffix
	 */
	@Override
	public String toString(final String value) {
		return value.replace("#L", "L").replace("#l", "l");
	}
}
