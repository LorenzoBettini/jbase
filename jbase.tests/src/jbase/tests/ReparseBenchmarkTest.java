package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.StringReader;
import java.util.Arrays;

import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.ReplaceRegion;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.testlanguage.parser.antlr.JbaseTestlanguageParser;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;

/** A reproducible parser-only comparison; timings are reported, never asserted. */
@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class ReparseBenchmarkTest {

	@Inject
	private IParser unicodeParser;

	@Inject
	private JbaseTestlanguageParser partialParser;

	@Test
	public void comparePartialAndUnicodeReparsing() {
		for (final int methods : new int[] { 100, 1_000, 3_000 }) {
			final var source = source(methods);
			final var target = "value" + methods / 2 + " = \"";
			final var offset = source.indexOf(target) + target.length();
			final var partial = measure(partialParser, source, offset);
			final var unicode = measure(unicodeParser, source, offset);
			assertEquals(30, partial.reusedRoots());
			assertEquals(0, unicode.reusedRoots());
			System.out.printf("BENCH methods=%d chars=%d partial_us=%d/%d "
					+ "unicode_us=%d/%d%n", methods, source.length(),
					partial.medianMicros(), partial.p90Micros(),
					unicode.medianMicros(), unicode.p90Micros());
		}
	}

	private Result measure(final IParser parser, final String source, final int offset) {
		IParseResult result = parser.parse(new StringReader(source));
		assertFalse(result.hasSyntaxErrors());
		final long[] samples = new long[30];
		var reusedRoots = 0;
		for (var i = 0; i < 42; i++) {
			final var change = new ReplaceRegion(offset, 1, (i & 1) == 0 ? "b" : "a");
			final long start = System.nanoTime();
			final IParseResult next = parser.reparse(result, change);
			final long elapsed = System.nanoTime() - start;
			assertFalse(next.hasSyntaxErrors());
			if (i >= 12) {
				samples[i - 12] = elapsed;
				if (next.getRootNode() == result.getRootNode()) {
					reusedRoots++;
				}
			}
			result = next;
		}
		Arrays.sort(samples);
		return new Result(samples[15] / 1_000, samples[26] / 1_000, reusedRoots);
	}

	private String source(final int methods) {
		final var builder = new StringBuilder();
		for (var i = 0; i < methods; i++) {
			builder.append("method m").append(i).append("() { var value")
					.append(i).append(" = \"a\"; }\n");
		}
		return builder.append("System.out.println(1);\n").toString();
	}

	private record Result(long medianMicros, long p90Micros, int reusedRoots) {}
}
