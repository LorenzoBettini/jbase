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
		final var radix = value.startsWith("0b") || value.startsWith("0B") ? 2
			: (value.startsWith("0x") || value.startsWith("0X")) && !value.contains("#") ? 16
			: value.matches("0[0-9]+[lL]?") ? 8 : 10;
		if (radix == 10) {
			return string;
		}
		final var isLong = value.endsWith("l") || value.endsWith("L");
		final var digits = value.substring(radix == 8 ? 0 : 2,
			value.length() - (isLong ? 1 : 0));
		try {
			final var number = new BigInteger(digits, radix);
			return (radix == 16 ? value.substring(0, value.length() - (isLong ? 1 : 0))
				: "0x" + number.toString(16)) + (isLong ? "#L" : "");
		} catch (NumberFormatException e) {
			throw new ValueConverterException("Invalid integer literal: " + string, node, e);
		}
	}

	@Override
	public String toString(final String value) {
		return value.replace("#L", "L").replace("#l", "l");
	}
}
