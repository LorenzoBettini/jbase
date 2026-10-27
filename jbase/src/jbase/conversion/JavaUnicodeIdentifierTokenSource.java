package jbase.conversion;

import java.util.ArrayDeque;
import java.util.Deque;

import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;

/** Joins Unicode identifier characters that the generated ASCII lexer emits separately. */
public final class JavaUnicodeIdentifierTokenSource implements TokenSource {

	private record Segment(String text, Token lastToken) {
	}

	private final TokenSource delegate;
	private final int identifierTokenType;
	private final Deque<Token> bufferedTokens = new ArrayDeque<>();

	public JavaUnicodeIdentifierTokenSource(final TokenSource delegate,
			final int identifierTokenType) {
		this.delegate = delegate;
		this.identifierTokenType = identifierTokenType;
	}

	@Override
	public Token nextToken() {
		final var first = readToken();
		if (first.getType() == Token.EOF) {
			return first;
		}
		final var firstText = first.getText();
		if (firstText == null || firstText.isEmpty()) {
			return first;
		}

		final var candidate = new StringBuilder(firstText);
		var lastToken = first;
		var escapedIdentifier = firstText.equals("^");
		final var identifierText = firstText.startsWith("^")
				? firstText.substring(1) : firstText;
		if (escapedIdentifier) {
			final var initial = readInitialSegment(first);
			if (initial == null) {
				return first;
			}
			candidate.append(initial.text());
			lastToken = initial.lastToken();
		} else if (!isIdentifier(identifierText)) {
			if (!isSingleHighSurrogate(identifierText)) {
				return first;
			}
			final var initial = readSurrogateSegment(first, true, false);
			if (initial == null) {
				return first;
			}
			candidate.setLength(0);
			candidate.append(initial.text());
			lastToken = initial.lastToken();
		}

		while (true) {
			final var next = readPartSegment(lastToken);
			if (next == null) {
				break;
			}
			candidate.append(next.text());
			lastToken = next.lastToken();
		}

		if (!escapedIdentifier && !containsNonAscii(identifierText)
				&& !containsNonAscii(candidate)) {
			return first;
		}
		return combinedIdentifier(first, lastToken, candidate.toString());
	}

	@Override
	public String getSourceName() {
		return delegate.getSourceName();
	}

	private Segment readInitialSegment(final Token previous) {
		final var next = readToken();
		if (!isAdjacent(previous, next)) {
			buffer(next);
			return null;
		}
		final var text = next.getText();
		if (isSingleHighSurrogate(text)) {
			return readSurrogateSegment(next, true, true);
		}
		if (isIdentifier(text)) {
			return new Segment(text, next);
		}
		buffer(next);
		return null;
	}

	private Segment readPartSegment(final Token previous) {
		final var next = readToken();
		if (!isAdjacent(previous, next)) {
			buffer(next);
			return null;
		}
		final var text = next.getText();
		if (isSingleHighSurrogate(text)) {
			return readSurrogateSegment(next, false, true);
		}
		if (isIdentifierPartSequence(text)) {
			return new Segment(text, next);
		}
		buffer(next);
		return null;
	}

	private Segment readSurrogateSegment(final Token highToken, final boolean start,
			final boolean bufferHighOnFailure) {
		final var lowToken = readToken();
		final var lowText = lowToken.getText();
		final var highText = highToken.getText();
		if (isAdjacent(highToken, lowToken) && lowText != null && lowText.length() == 1
				&& Character.isLowSurrogate(lowText.charAt(0))) {
			final var codePoint = Character.toCodePoint(highText.charAt(0), lowText.charAt(0));
			if (start ? Character.isJavaIdentifierStart(codePoint)
					: Character.isJavaIdentifierPart(codePoint)) {
				return new Segment(highText + lowText, lowToken);
			}
		}
		buffer(lowToken);
		if (bufferHighOnFailure) {
			buffer(highToken);
		}
		return null;
	}

	private Token readToken() {
		return bufferedTokens.isEmpty() ? delegate.nextToken() : bufferedTokens.removeFirst();
	}

	private void buffer(final Token token) {
		bufferedTokens.addFirst(token);
	}

	private boolean isAdjacent(final Token previous, final Token next) {
		if (!(previous instanceof CommonToken previousCommon)
				|| !(next instanceof CommonToken nextCommon)) {
			return false;
		}
		return previousCommon.getStopIndex() >= 0
				&& nextCommon.getStartIndex() == previousCommon.getStopIndex() + 1;
	}

	private boolean isIdentifier(final String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		var offset = 0;
		var codePoint = value.codePointAt(offset);
		if (!Character.isJavaIdentifierStart(codePoint)) {
			return false;
		}
		offset += Character.charCount(codePoint);
		while (offset < value.length()) {
			codePoint = value.codePointAt(offset);
			if (!Character.isJavaIdentifierPart(codePoint)) {
				return false;
			}
			offset += Character.charCount(codePoint);
		}
		return true;
	}

	private boolean isIdentifierPartSequence(final String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		var offset = 0;
		while (offset < value.length()) {
			final var codePoint = value.codePointAt(offset);
			if (!Character.isJavaIdentifierPart(codePoint)) {
				return false;
			}
			offset += Character.charCount(codePoint);
		}
		return true;
	}

	private boolean isSingleHighSurrogate(final String value) {
		return value != null && value.length() == 1 && Character.isHighSurrogate(value.charAt(0));
	}

	private boolean containsNonAscii(final CharSequence value) {
		for (var i = 0; i < value.length(); i++) {
			if (value.charAt(i) > 0x7f) {
				return true;
			}
		}
		return false;
	}

	private Token combinedIdentifier(final Token first, final Token last,
			final String identifier) {
		final var result = new CommonToken(identifierTokenType, identifier);
		result.setInputStream(first.getInputStream());
		result.setStartIndex(((CommonToken) first).getStartIndex());
		result.setStopIndex(((CommonToken) last).getStopIndex());
		result.setLine(first.getLine());
		result.setCharPositionInLine(first.getCharPositionInLine());
		result.setChannel(first.getChannel());
		result.setTokenIndex(-1);
		return result;
	}
}
