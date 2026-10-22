package jbase.conversion;

import java.math.BigInteger;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;

/** Maps Java radix literals to Xbase's hexadecimal model representation. */
public class JbaseNumberValueConverter extends AbstractValueConverter<String> {

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

	@Override
	public String toString(final String value) {
		return value.replace("#L", "L").replace("#l", "l");
	}
}
