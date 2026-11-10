package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.junit.Test;

import jbase.conversion.JavaUnicodeIdentifierTokenSource;

public class JavaUnicodeIdentifierTokenSourceTest {

	private static final int IDENTIFIER = 100;
	private static final int OTHER = 101;

	@Test
	public void preservesEofEmptyAndNullTokens() {
		final var eof = token(Token.EOF, "<EOF>", -1, -1);
		final var source = source(token(OTHER, null, 0, -1), token(OTHER, "", 1, 0), eof);
		assertEquals("test", source.getSourceName());
		assertNull(source.nextToken().getText());
		assertEquals("", source.nextToken().getText());
		assertSame(eof, source.nextToken());
	}

	@Test
	public void combinesUnicodeIdentifiersAndTheirParts() {
		final var source = source(token(OTHER, "a", 0, 0),
				token(OTHER, "é", 1, 1), token(OTHER, "1", 2, 2));
		final var identifier = source.nextToken();
		assertEquals(IDENTIFIER, identifier.getType());
		assertEquals("aé1", identifier.getText());

		final var supplementary = new String(Character.toChars(0x10400));
		final var codePoints = supplementary.toCharArray();
		final var supplementarySource = source(
				token(OTHER, String.valueOf(codePoints[0]), 0, 0),
				token(OTHER, String.valueOf(codePoints[1]), 1, 1));
		assertEquals(supplementary, supplementarySource.nextToken().getText());

		final var partSource = source(token(OTHER, "a", 0, 0),
				token(OTHER, String.valueOf(codePoints[0]), 1, 1),
				token(OTHER, String.valueOf(codePoints[1]), 2, 2));
		assertEquals("a" + supplementary, partSource.nextToken().getText());
	}

	@Test
	public void preservesAsciiAndNonAdjacentTokens() {
		final var first = token(IDENTIFIER, "a", 0, 0);
		final var unicode = token(OTHER, "é", 2, 2);
		final var nonAdjacentSource = source(first, unicode);
		assertSame(first, nonAdjacentSource.nextToken());
		assertEquals("é", nonAdjacentSource.nextToken().getText());

		final var unknownStop = token(IDENTIFIER, "a", 0, -1);
		assertSame(unknownStop, source(unknownStop, token(OTHER, ".", 1, 1)).nextToken());

		final Token bareFirst = tokenWithoutCommonToken(IDENTIFIER, "a");
		assertSame(bareFirst, source(bareFirst).nextToken());
		final Token bareNext = tokenWithoutCommonToken(OTHER, ".");
		final var sourceWithBareNext = source(token(IDENTIFIER, "a", 0, 0), bareNext);
		assertEquals("a", sourceWithBareNext.nextToken().getText());
		assertSame(bareNext, sourceWithBareNext.nextToken());
	}

	@Test
	public void handlesEscapedIdentifiersAndInvalidInitialSegments() {
		final var escaped = source(token(OTHER, "^", 0, 0), token(OTHER, "é", 1, 1));
		assertEquals("^é", escaped.nextToken().getText());

		final var nonAdjacent = source(token(OTHER, "^", 0, 0), token(IDENTIFIER, "name", 2, 5));
		assertEquals("^", nonAdjacent.nextToken().getText());
		assertEquals("name", nonAdjacent.nextToken().getText());

		final var nullSegment = source(token(OTHER, "^", 0, 0), token(OTHER, null, 1, 0));
		assertEquals("^", nullSegment.nextToken().getText());
		assertNull(nullSegment.nextToken().getText());

		final var emptySegment = source(token(OTHER, "^", 0, 0), token(OTHER, "", 1, 0));
		assertEquals("^", emptySegment.nextToken().getText());
		assertEquals("", emptySegment.nextToken().getText());
	}

	@Test
	public void validatesIdentifierStartsAndParts() {
		assertEquals("1", source(token(OTHER, "1", 0, 0)).nextToken().getText());
		assertEquals("a-", source(token(OTHER, "a-", 0, 1)).nextToken().getText());
		assertEquals(".", source(token(OTHER, ".", 0, 0)).nextToken().getText());

		final var nullPart = source(token(OTHER, "é", 0, 0), token(OTHER, null, 1, 0));
		assertEquals("é", nullPart.nextToken().getText());
		assertNull(nullPart.nextToken().getText());

		final var emptyPart = source(token(OTHER, "é", 0, 0), token(OTHER, "", 1, 0));
		assertEquals("é", emptyPart.nextToken().getText());
		assertEquals("", emptyPart.nextToken().getText());
	}

	@Test
	public void handlesInvalidAndIncompleteSurrogatePairs() {
		final var high = Character.highSurrogate(0x10400);
		final var low = Character.lowSurrogate(0x10400);
		final var badStartHigh = Character.highSurrogate(0x1f600);
		final var badStart = source(token(OTHER, String.valueOf(badStartHigh), 0, 0),
				token(OTHER, String.valueOf(Character.lowSurrogate(0x1f600)), 1, 1));
		assertEquals(String.valueOf(badStartHigh), badStart.nextToken().getText());

		final var nonAdjacent = source(token(OTHER, String.valueOf(high), 0, 0),
				token(OTHER, String.valueOf(low), 2, 2));
		assertEquals(String.valueOf(high), nonAdjacent.nextToken().getText());

		final var nullLow = source(token(OTHER, String.valueOf(high), 0, 0),
				token(OTHER, null, 1, 0));
		assertEquals(String.valueOf(high), nullLow.nextToken().getText());

		final var emptyLow = source(token(OTHER, String.valueOf(high), 0, 0),
				token(OTHER, "", 1, 0));
		assertEquals(String.valueOf(high), emptyLow.nextToken().getText());

		final var nonLow = source(token(OTHER, String.valueOf(high), 0, 0),
				token(OTHER, "x", 1, 1));
		assertEquals(String.valueOf(high), nonLow.nextToken().getText());

		final var longLow = source(token(OTHER, String.valueOf(high), 0, 0),
				token(OTHER, "xy", 1, 2));
		assertEquals(String.valueOf(high), longLow.nextToken().getText());

		final var badPart = source(token(OTHER, "a", 0, 0),
				token(OTHER, String.valueOf(Character.highSurrogate(0x1f600)), 1, 1),
				token(OTHER, String.valueOf(Character.lowSurrogate(0x1f600)), 2, 2));
		assertEquals("a", badPart.nextToken().getText());
	}

	@Test
	public void recognizesSupplementaryEscapedIdentifiers() {
		final var supplementary = new String(Character.toChars(0x10400));
		final var pair = supplementary.toCharArray();
		final var source = source(token(OTHER, "^", 0, 0),
				token(OTHER, String.valueOf(pair[0]), 1, 1),
				token(OTHER, String.valueOf(pair[1]), 2, 2));
		assertEquals("^" + supplementary, source.nextToken().getText());
	}

	private static JavaUnicodeIdentifierTokenSource source(final Token... input) {
		final var tokens = new ArrayList<>(Arrays.asList(input));
		if (tokens.isEmpty() || tokens.getLast().getType() != Token.EOF) {
			tokens.add(token(Token.EOF, "<EOF>", -1, -1));
		}
		final TokenSource delegate = new TokenSource() {
			private int index;

			@Override
			public Token nextToken() {
				return index < tokens.size() ? tokens.get(index++) : tokens.getLast();
			}

			@Override
			public String getSourceName() {
				return "test";
			}
		};
		return new JavaUnicodeIdentifierTokenSource(delegate, IDENTIFIER);
	}

	private static CommonToken token(final int type, final String text, final int start,
			final int stop) {
		final var token = new CommonToken(type, text);
		token.setStartIndex(start);
		token.setStopIndex(stop);
		return token;
	}

	private static Token tokenWithoutCommonToken(final int type, final String text) {
		return (Token) Proxy.newProxyInstance(Token.class.getClassLoader(),
				new Class<?>[] { Token.class },
				(proxy, method, arguments) -> switch (method.getName()) {
					case "getType" -> type;
					case "getText" -> text;
					default -> throw new UnsupportedOperationException(method.getName());
				});
	}
}
