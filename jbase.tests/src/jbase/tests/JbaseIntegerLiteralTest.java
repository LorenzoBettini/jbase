package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;

import com.google.inject.Inject;

import jbase.conversion.JbaseNumberValueConverter;

public class JbaseIntegerLiteralTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private ISerializer serializer;

	@Inject
	private JbaseNumberValueConverter converter;

	private record Example(String literal, String type, Number value) {
	}

	@Test
	public void generatedValuesMatchJavaRadixSemantics() throws Exception {
		final var examples = List.of(
			new Example("0b1010", "int", 10),
			new Example("0b0", "int", 0),
			new Example("0B1__010", "int", 10),
			new Example("0b1010L", "long", 10L),
			new Example("0B1_010l", "long", 10L),
			new Example("0b" + "1".repeat(32), "int", -1),
			new Example("0B1" + "0".repeat(31), "int", Integer.MIN_VALUE),
			new Example("0b" + "1".repeat(64) + "L", "long", -1L),
			new Example("0B1" + "0".repeat(63) + "l", "long", Long.MIN_VALUE),
			new Example("0xFFL", "long", 255L),
			new Example("0Xf__fl", "long", 255L),
			new Example("0x7fff_ffffL", "long", 2147483647L),
			new Example("0x8000_0000L", "long", 2147483648L),
			new Example("0xffff_ffff", "int", -1),
			new Example("0x7fff_ffff_ffff_ffffL", "long", Long.MAX_VALUE),
			new Example("0X8000_0000_0000_0000l", "long", Long.MIN_VALUE),
			new Example("0xffff_ffff_ffff_ffffL", "long", -1L),
			new Example("077", "int", 63),
			new Example("0__77", "int", 63),
			new Example("077L", "long", 63L),
			new Example("077l", "long", 63L),
			new Example("00", "int", 0),
			new Example("0L", "long", 0L),
			new Example("017777777777", "int", Integer.MAX_VALUE),
			new Example("020000000000", "int", Integer.MIN_VALUE),
			new Example("037777777777", "int", -1),
			new Example("0777777777777777777777L", "long", Long.MAX_VALUE),
			new Example("01000000000000000000000L", "long", Long.MIN_VALUE),
			new Example("01777777777777777777777L", "long", -1L),
			new Example("1_000", "int", 1000),
			new Example("077.0", "double", 77.0),
			new Example("077e1", "double", 770.0),
			new Example("077f", "float", 77f));
		final var input = new StringBuilder();
		for (var i = 0; i < examples.size(); i++) {
			final var example = examples.get(i);
			input.append("method value").append(i).append("() : ").append(example.type())
				.append(" { return ").append(example.literal()).append("; }\n");
		}
		assertValues(input, examples);
		// The converter's model representation must serialize back to valid Java syntax.
		assertValues(serializer.serialize(parser.parse(input)), examples);
	}

	private void assertValues(final CharSequence input, final List<Example> examples)
			throws Exception {
		final var classes = new ArrayList<Class<?>>();
		compilationTestHelper.compile(input, result -> {
			assertNoValidationErrors(result);
			classes.add(result.getCompiledClass());
		});
		final var instance = classes.getFirst().getConstructor().newInstance();
		for (var i = 0; i < examples.size(); i++) {
			assertEquals(examples.get(i).literal(), examples.get(i).value(),
				instance.getClass().getMethod("value" + i).invoke(instance));
		}
	}

	@Test
	public void invalidRadixLiteralsAreRejected() throws Exception {
		for (final var literal : List.of("08", "09L", "0b102", "0b_1", "0b1_",
				"0x_FF", "0xFF_", "0x1_0000_0000", "0x1_0000_0000_0000_0000L",
				"0b1" + "0".repeat(32), "0b1" + "0".repeat(64) + "L",
				"040000000000", "02000000000000000000000L")) {
			final var type = literal.endsWith("L") ? "long" : "int";
			final var model = parser.parse(type + " value = " + literal + ";");
			assertTrue(literal, validationTestHelper.validate(model).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
	}

	@Test(expected = ValueConverterException.class)
	public void invalidOctalConversionThrows() {
		converter.toValue("08", null);
	}

	@Test
	public void hexadecimalModelRepresentationIsPreserved() {
		final var value = converter.toValue("0xFF#L", null);
		assertEquals("0xFF#L", value);
		assertEquals("0xFFL", converter.toString(value));
	}
}
