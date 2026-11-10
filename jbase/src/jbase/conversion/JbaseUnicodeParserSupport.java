package jbase.conversion;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.util.ReplaceRegion;

/** Shared source translation and original-source tracking for generated Jbase parsers. */
public class JbaseUnicodeParserSupport {

	private final Map<IParseResult, String> originalSources =
			Collections.synchronizedMap(new WeakHashMap<>());

	public IParseResult parse(final Reader reader, final Function<Reader, IParseResult> parser) {
		final var source = readSource(reader);
		final var result = parser.apply(new StringReader(JavaUnicodeEscapeTranslator.translate(source)));
		originalSources.put(result, source);
		return result;
	}

	public IParseResult reparse(final IParseResult previousParseResult,
			final ReplaceRegion replaceRegion, final Function<Reader, IParseResult> parser) {
		final var source = Objects.requireNonNull(originalSources.remove(previousParseResult),
				"Parse result was not produced by this parser");
		final var modifiedSource = new StringBuilder(source);
		replaceRegion.applyTo(modifiedSource);
		return parser.apply(new StringReader(modifiedSource.toString()));
	}

	private String readSource(final Reader reader) {
		final var source = new StringWriter();
		try {
			reader.transferTo(source);
		} catch (final IOException e) {
			throw new WrappedException(e);
		}
		return source.toString();
	}
}
