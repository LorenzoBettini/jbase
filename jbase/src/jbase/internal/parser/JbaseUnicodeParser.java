package jbase.internal.parser;

import java.io.Reader;

import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.util.ReplaceRegion;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.TokenSource;

import jbase.conversion.JavaUnicodeIdentifierTokenSource;
import jbase.conversion.JbaseUnicodeParserSupport;
import jbase.parser.antlr.JbaseParser;
import jbase.parser.antlr.internal.InternalJbaseLexer;

/** Applies Java Unicode translation before every parser entry point tokenizes its input. */
public class JbaseUnicodeParser extends JbaseParser {

	private final JbaseUnicodeParserSupport parserSupport = new JbaseUnicodeParserSupport();

	@Override
	protected TokenSource createLexer(final CharStream input) {
		final var lexer = super.createLexer(input);
		return new JavaUnicodeIdentifierTokenSource(lexer,
				InternalJbaseLexer.RULE_ID);
	}

	@Override
	public IParseResult doParse(final Reader reader) {
		return parserSupport.parse(reader, super::doParse);
	}

	@Override
	public IParseResult parse(final ParserRule rule, final Reader reader) {
		return parserSupport.parse(reader,
				translatedReader -> super.parse(rule, translatedReader));
	}

	@Override
	public IParseResult parse(final RuleCall ruleCall, final Reader reader,
			final int initialLookAhead) {
		return parserSupport.parse(reader,
				translatedReader -> super.parse(ruleCall, translatedReader, initialLookAhead));
	}

	@Override
	protected IParseResult doReparse(final IParseResult previousParseResult,
			final ReplaceRegion replaceRegion) {
		return parserSupport.reparse(previousParseResult, replaceRegion, this::doParse);
	}

	@Override
	protected boolean isReparseSupported() {
		return true;
	}
}
