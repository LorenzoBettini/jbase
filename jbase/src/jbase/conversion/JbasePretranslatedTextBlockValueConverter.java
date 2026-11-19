package jbase.conversion;

/** Normalizes text blocks from a parser that has already translated Unicode escapes. */
public class JbasePretranslatedTextBlockValueConverter extends JbaseTextBlockValueConverter {

	@Override
	protected String translateSource(final String source) {
		return source;
	}
}
