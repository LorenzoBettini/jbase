package jbase.conversion;

import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.nodemodel.INode;

/** Applies Java identifier start and part rules to a converted identifier. */
final class JavaIdentifierValidator {

	private JavaIdentifierValidator() {
	}

	static String validate(final String value, final INode node) {
		if (value == null) {
			return null;
		}
		final var identifier = value.startsWith("^") ? value.substring(1) : value;
		if (identifier.isEmpty()) {
			throw invalidIdentifier(node);
		}
		var index = 0;
		var codePoint = identifier.codePointAt(index);
		if (!Character.isJavaIdentifierStart(codePoint)) {
			throw invalidIdentifier(node);
		}
		index += Character.charCount(codePoint);
		while (index < identifier.length()) {
			codePoint = identifier.codePointAt(index);
			if (!Character.isJavaIdentifierPart(codePoint)) {
				throw invalidIdentifier(node);
			}
			index += Character.charCount(codePoint);
		}
		return identifier;
	}

	private static ValueConverterException invalidIdentifier(final INode node) {
		return new ValueConverterException("Invalid Java identifier", node, null);
	}
}
