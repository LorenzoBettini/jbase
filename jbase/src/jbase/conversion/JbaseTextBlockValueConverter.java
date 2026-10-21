package jbase.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.conversion.impl.AbstractValueConverter;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.util.Strings;

/** Java text blocks normalize line endings and indentation before interpreting escapes. */
public class JbaseTextBlockValueConverter extends AbstractValueConverter<String> {

	/**
	 * Retains escapes while removing source indentation. The compiler can then add
	 * the generated Java indentation uniformly, without changing the block's value.
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
			final var unicodeEscape = character == '\\'
					&& (wasUnicodeEscape || backslashes % 2 == 0)
					&& source.startsWith("u", i + 1);
			if (unicodeEscape) {
				var start = i + 2;
				while (start < source.length() && source.charAt(start) == 'u') {
					start++;
				}
				final var end = Math.min(start + 4, source.length());
				final var digits = source.substring(start, end);
				if (!digits.matches("[0-9a-fA-F]{4}")) {
					throw new IllegalArgumentException("Invalid Unicode escape in text block");
				}
				character = (char) Integer.parseInt(digits, 16);
				i = end;
			} else {
				i++;
			}
			result.append(character);
			backslashes = character == '\\' ? backslashes + 1 : 0;
			wasUnicodeEscape = unicodeEscape;
		}
		return result.toString();
	}

	@Override
	public String toValue(final String string, final INode node) {
		try {
			return normalizedContent(string).translateEscapes();
		} catch (final IllegalArgumentException e) {
			throw new ValueConverterException("Invalid text block: " + e.getMessage(), node, e);
		}
	}

	@Override
	public String toString(final String value) {
		// Protect spaces from incidental whitespace removal, including trailing spaces.
		return "\"\"\"\n" + Strings.convertToJavaString(value, false).replace(" ", "\\s")
				+ "\"\"\"";
	}
}
