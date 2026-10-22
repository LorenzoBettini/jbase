package jbase.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.Strings;

/** Java text blocks normalize line endings and indentation before interpreting escapes. */
public class JbaseTextBlockValueConverter extends AbstractValueConverter<String> {

	private record UnicodeEscape(char character, int endIndex) {
	}

	/**
	 * Translates Unicode escapes and removes delimiters and incidental source indentation.
	 * Other escapes are retained so the compiler can reindent the generated Java block
	 * without changing its value. Line endings are normalized to line feeds.
	 *
	 * @param source the complete text-block literal, including its delimiters
	 * @return normalized content with ordinary Java escapes still present
	 * @throws IllegalArgumentException if a Unicode escape or closing delimiter is invalid
	 */
	public String normalizedContent(final String source) {
		final var normalized = translateUnicodeEscapes(source)
				.replace("\r\n", "\n").replace('\r', '\n');
		final var start = normalized.indexOf('\n') + 1;
		var i = start;
		// Unicode escapes may introduce quotes that were not visible to the lexer.
		while (i < normalized.length()) {
			if (normalized.charAt(i) == '\\') {
				i += 2;
			} else if (normalized.startsWith("\"\"\"", i)) {
				if (i != normalized.length() - 3) {
					throw new IllegalArgumentException("Unescaped text block delimiter in content");
				}
				return normalized.substring(start, i).stripIndent();
			} else {
				i++;
			}
		}
		throw new IllegalArgumentException("Unterminated text block");
	}

	/** Unicode translation precedes even line ending and indentation processing (JLS 3.3). */
	private String translateUnicodeEscapes(final String source) {
		final var result = new StringBuilder();
		var backslashes = 0;
		var wasUnicodeEscape = false;
		var i = 0;
		while (i < source.length()) {
			var character = source.charAt(i);
			final var unicodeEscape = isUnicodeEscape(source, i, backslashes, wasUnicodeEscape);
			if (unicodeEscape) {
				final var escape = parseUnicodeEscape(source, i);
				character = escape.character();
				i = escape.endIndex();
			} else {
				i++;
			}
			result.append(character);
			backslashes = character == '\\' ? backslashes + 1 : 0;
			wasUnicodeEscape = unicodeEscape;
		}
		return result.toString();
	}

	private boolean isUnicodeEscape(final String source, final int index, final int backslashes,
			final boolean wasUnicodeEscape) {
		return source.charAt(index) == '\\'
				&& (wasUnicodeEscape || backslashes % 2 == 0)
				&& source.startsWith("u", index + 1);
	}

	private UnicodeEscape parseUnicodeEscape(final String source, final int index) {
		var start = index + 2;
		while (start < source.length() && source.charAt(start) == 'u') {
			start++;
		}
		final var end = Math.min(start + 4, source.length());
		final var digits = source.substring(start, end);
		if (!digits.matches("[0-9a-fA-F]{4}")) {
			throw new IllegalArgumentException("Invalid Unicode escape in text block");
		}
		return new UnicodeEscape((char) Integer.parseInt(digits, 16), end);
	}

	/**
	 * Computes a text block's value by normalizing its content and interpreting Java escapes.
	 *
	 * @param string the complete text-block literal, including its delimiters
	 * @param node the source node to associate with a conversion error
	 * @return the normalized, unescaped string value
	 * @throws ValueConverterException if the literal or one of its escapes is invalid
	 */
	@Override
	public String toValue(final String string, final INode node) {
		try {
			return normalizedContent(string).translateEscapes();
		} catch (final IllegalArgumentException e) {
			throw new ValueConverterException("Invalid text block: " + e.getMessage(), node, e);
		}
	}

	/**
	 * Serializes a string value as a Java text block.
	 * Spaces are escaped to protect them from incidental-whitespace removal, including
	 * spaces at the ends of lines.
	 *
	 * @param value the string value to preserve
	 * @return a complete text-block literal with escaped content and closing delimiter
	 */
	@Override
	public String toString(final String value) {
		// Protect spaces from incidental whitespace removal, including trailing spaces.
		return "\"\"\"\n" + Strings.convertToJavaString(value, false).replace(" ", "\\s")
				+ "\"\"\"";
	}
}
