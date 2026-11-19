package jbase.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.STRINGValueConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.Strings;

/** Converts Java string and character escape sequences after Unicode translation. */
public class JbaseStringValueConverter extends STRINGValueConverter {

	protected char delimiter() {
		return '"';
	}

	@Override
	public String toValue(final String literal, final INode node) {
		if (literal == null) {
			return null;
		}
		final var delimiter = delimiter();
		if (literal.length() < 2 || literal.charAt(0) != delimiter
				|| literal.charAt(literal.length() - 1) != delimiter) {
			throw new ValueConverterException("Literal is not properly closed", node, null);
		}
		final var body = literal.substring(1, literal.length() - 1);
		if (body.indexOf('\n') >= 0 || body.indexOf('\r') >= 0) {
			throw new ValueConverterException("Line terminator in literal", node, null);
		}
		try {
			return body.translateEscapes();
		} catch (final IllegalArgumentException e) {
			throw new ValueConverterException("Invalid Java escape sequence", node, e);
		}
	}

	@Override
	protected String toEscapedString(final String value) {
		final var delimiter = delimiter();
		return delimiter + Strings.convertToJavaString(value, false) + delimiter;
	}
}
