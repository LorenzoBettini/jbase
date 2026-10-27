package jbase.conversion;

/** Performs Java's source-level Unicode escape translation before tokenization. */
public final class JavaUnicodeEscapeTranslator {

	private record UnicodeEscape(char character, int endIndex) {
	}

	private JavaUnicodeEscapeTranslator() {
	}

	/**
	 * Translates eligible, well-formed Unicode escapes. Malformed candidates remain in the
	 * source so the lexer or literal converter can report them in context.
	 */
	public static String translate(final String source) {
		final var result = new StringBuilder();
		var backslashes = 0;
		var wasUnicodeEscape = false;
		var i = 0;
		while (i < source.length()) {
			var character = source.charAt(i);
			final var unicodeEscape = isUnicodeEscape(source, i, backslashes, wasUnicodeEscape);
			if (unicodeEscape) {
				final var escape = parseUnicodeEscape(source, i);
				if (escape != null) {
					character = escape.character();
					i = escape.endIndex();
				} else {
					i++;
				}
			} else {
				i++;
			}
			result.append(character);
			backslashes = character == '\\' ? backslashes + 1 : 0;
			wasUnicodeEscape = unicodeEscape;
		}
		return result.toString();
	}

	private static boolean isUnicodeEscape(final String source, final int index,
			final int backslashes, final boolean wasUnicodeEscape) {
		return source.charAt(index) == '\\'
				&& (wasUnicodeEscape || backslashes % 2 == 0)
				&& source.startsWith("u", index + 1);
	}

	private static UnicodeEscape parseUnicodeEscape(final String source, final int index) {
		var start = index + 2;
		while (start < source.length() && source.charAt(start) == 'u') {
			start++;
		}
		final var end = Math.min(start + 4, source.length());
		final var digits = source.substring(start, end);
		if (!digits.matches("[0-9a-fA-F]{4}")) {
			return null;
		}
		return new UnicodeEscape((char) Integer.parseInt(digits, 16), end);
	}
}
