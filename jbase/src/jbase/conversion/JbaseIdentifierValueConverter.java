package jbase.conversion;

import org.antlr.runtime.TokenSource;
import org.eclipse.xtext.conversion.impl.IDValueConverter;
import org.eclipse.xtext.nodemodel.INode;

/** Validates Unicode identifier code points according to Java's identifier rules. */
public class JbaseIdentifierValueConverter extends IDValueConverter {

	@Override
	public String toValue(final String string, final INode node) {
		return JavaIdentifierValidator.validate(super.toValue(string, node), node);
	}

	/** Validates serialized identifiers with the same Unicode tokens used by the parser. */
	@Override
	protected TokenSource getTokenSource(final String escapedValue) {
		final var lexer = super.getTokenSource(escapedValue);
		if (lexer == null) {
			return null;
		}
		final var identifierTokenType = getTokenDefMap().entrySet().stream()
				.filter(entry -> "RULE_ID".equals(entry.getValue()))
				.findFirst().orElseThrow().getKey();
		return new JavaUnicodeIdentifierTokenSource(lexer, identifierTokenType);
	}
}
