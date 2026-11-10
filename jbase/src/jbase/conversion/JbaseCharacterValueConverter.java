package jbase.conversion;

/** Converts Java character literals using the same escape rules as string literals. */
public class JbaseCharacterValueConverter extends JbaseStringValueConverter {

	@Override
	protected char delimiter() {
		return '\'';
	}
}
