package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.parser.IParser;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.util.ReplaceRegion;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.conversion.JavaUnicodeEscapeTranslator;
import jbase.conversion.JbaseCharacterValueConverter;
import jbase.conversion.JbaseIdentifierValueConverter;
import jbase.conversion.JbaseStringValueConverter;
import jbase.jbase.XJCharLiteral;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseJavaEscapeTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> modelParser;

	@Inject
	private IParser parser;

	@Inject
	private IGrammarAccess grammarAccess;

	@Inject
	private ISerializer serializer;

	@Inject
	private JbaseStringValueConverter stringConverter;

	@Inject
	private JbaseCharacterValueConverter characterConverter;

	@Inject
	private JbaseIdentifierValueConverter identifierConverter;

	@Test
	public void javaStringAndCharacterEscapesAreConvertedAndSerialized() throws Exception {
		final var source = """
			{
				String space = "\\s";
				char octal = '\\141';
				char unicode = '\\uuuu0061';
				String café = "café";
				String v\\u0061lue = space + octal + unicode + café;
			}
			""";
		final var model = modelParser.parse(source);
		assertTrue(model.eResource().getErrors().toString(), model.eResource().getErrors().isEmpty());
		final var strings = EcoreUtil2.getAllContentsOfType(model, XStringLiteral.class);
		final var characters = EcoreUtil2.getAllContentsOfType(model, XJCharLiteral.class);
		final var variables = EcoreUtil2.getAllContentsOfType(model, XVariableDeclaration.class);
		assertEquals(" ", strings.getFirst().getValue());
		assertEquals(List.of("a", "a"), characters.stream().map(XJCharLiteral::getValue).toList());
		assertTrue(variables.stream().anyMatch(variable -> variable.getName().equals("café")));
		assertTrue(variables.stream().anyMatch(variable -> variable.getName().equals("value")));

		final var serialized = serializer.serialize(model);
		final var reparsed = modelParser.parse(serialized);
		assertTrue(reparsed.eResource().getErrors().toString(),
			reparsed.eResource().getErrors().isEmpty());
		assertEquals(" ", EcoreUtil2.getAllContentsOfType(reparsed, XStringLiteral.class)
				.getFirst().getValue());
	}

	@Test
	public void escapedIdentifiersAndLiteralsCompileAndRun() throws Exception {
		assertExecuteMain("""
			method testMe() {
				String space = "\\s";
				char octal = '\\141';
				char unicode = '\\uuuu0061';
				String café = "café";
				String v\\u0061lue = space + octal + unicode + " " + café;
				System.out.println(v\\u0061lue);
			}
			""", " aa café\n");
	}

	@Test
	public void malformedJavaEscapesAndIdentifiersAreReported() throws Exception {
		assertFalse(modelParser.parse("\"\\q\";").eResource().getErrors().isEmpty());
		assertFalse(modelParser.parse("int a😀 = 1;").eResource().getErrors().isEmpty());
		assertThrows(ValueConverterException.class,
			() -> stringConverter.toValue("\"line\nbreak\"", null));
		assertThrows(ValueConverterException.class,
			() -> stringConverter.toValue("\"line\rbreak\"", null));
		assertThrows(ValueConverterException.class,
			() -> stringConverter.toValue("\"", null));
		assertThrows(ValueConverterException.class,
			() -> stringConverter.toValue("text\"", null));
		assertThrows(ValueConverterException.class,
			() -> stringConverter.toValue("\"text", null));
		assertThrows(ValueConverterException.class,
			() -> characterConverter.toValue("\"wrong delimiter\"", null));
	}

	@Test
	public void convertersRoundTripAndValidateJavaIdentifierCodePoints() {
		for (final var value : List.of("", "space and \\ slash", "\n\t", "café")) {
			assertEquals(value, stringConverter.toValue(stringConverter.toString(value), null));
		}
		for (final var value : List.of("'", "\\", "a")) {
			assertEquals(value, characterConverter.toValue(characterConverter.toString(value), null));
		}
		assertNull(stringConverter.toValue(null, null));
		assertNull(identifierConverter.toValue(null, null));
		assertEquals("café", identifierConverter.toValue("café", null));
		assertEquals("café", identifierConverter.toString("café"));
		final var supplementaryIdentifier = new String(Character.toChars(0x10400));
		assertEquals(supplementaryIdentifier,
				identifierConverter.toString(supplementaryIdentifier));
		assertEquals("name", identifierConverter.toValue("^name", null));
		assertEquals("name", identifierConverter.toValue("^^name", null));
		assertTrue(Character.isJavaIdentifierStart(0x10400));
		assertEquals(new String(Character.toChars(0x10400)),
			identifierConverter.toValue(new String(Character.toChars(0x10400)), null));
		assertThrows(ValueConverterException.class, () -> identifierConverter.toValue("", null));
		assertThrows(ValueConverterException.class,
			() -> identifierConverter.toValue("?name", null));
		assertThrows(ValueConverterException.class,
			() -> identifierConverter.toValue("name-", null));
	}

	@Test
	public void supplementaryUnicodeIdentifierCodePointsAreTokenizedTogether() throws Exception {
		final var supplementary = new String(Character.toChars(0x10400));
		for (final var identifier : List.of(supplementary, "a" + supplementary)) {
			final var source = "int " + identifier + " = 1;";
			final var parsed = modelParser.parse(source);
			assertTrue(parsed.eResource().getErrors().toString(),
					parsed.eResource().getErrors().isEmpty());
			assertTrue(EcoreUtil2.getAllContentsOfType(parsed, XVariableDeclaration.class).stream()
					.anyMatch(variable -> variable.getName().equals(identifier)));
		}
		final var escapedIdentifier = modelParser.parse("int ^é = 1;");
		assertTrue(escapedIdentifier.eResource().getErrors().toString(),
				escapedIdentifier.eResource().getErrors().isEmpty());
		assertTrue(EcoreUtil2.getAllContentsOfType(escapedIdentifier, XVariableDeclaration.class)
				.stream().anyMatch(variable -> variable.getName().equals("é")));
	}

	@Test
	public void unicodeTranslationHonorsBackslashEligibilityAndMalformedCandidates() {
		assertEquals("a", JavaUnicodeEscapeTranslator.translate("\\" + "uuuu0061"));
		assertEquals("\\".repeat(2) + "u0061",
			JavaUnicodeEscapeTranslator.translate("\\".repeat(2) + "u0061"));
		assertEquals("\\".repeat(2) + "a",
			JavaUnicodeEscapeTranslator.translate("\\".repeat(3) + "u0061"));
		assertEquals("\\" + "u12xz",
			JavaUnicodeEscapeTranslator.translate("\\" + "u12xz"));
		assertEquals("\\" + "u",
			JavaUnicodeEscapeTranslator.translate("\\" + "u"));
		assertEquals("\\Z",
			JavaUnicodeEscapeTranslator.translate("\\u005c\\u005a"));
		assertEquals("", JavaUnicodeEscapeTranslator.translate(""));
	}

	@Test
	public void unicodeTranslationMapsEditorOffsets() {
		final var translation = JavaUnicodeEscapeTranslator.translateWithOffsets(
				"caf\\u00e9 = 1\\u003b café");
		assertEquals("café = 1; café", translation.text());
		assertEquals(0, translation.originalOffset(0));
		assertEquals(3, translation.originalOffset(3));
		assertEquals(9, translation.originalOffset(4));
		assertEquals(20, translation.originalOffset(10));
		assertEquals(3, translation.translatedOffset(5));
		assertEquals(4, translation.translatedOffset(9));
		assertEquals(8, translation.translatedOffset(15));
		assertEquals(10, translation.translatedOffset(20));
		assertEquals(translation.text().length(),
				translation.translatedOffset("caf\\u00e9 = 1\\u003b café".length()));
	}

	@Test
	public void everyParserEntryPointTranslatesAndReparsesOriginalSource() {
		final var source = "int v\\u0061lue = 1;";
		final var rule = grammarAccess.getGrammar().getRules().stream()
				.filter(ParserRule.class::isInstance)
				.map(ParserRule.class::cast)
				.filter(candidate -> candidate.getName().equals("JbaseTestLanguageModel"))
				.findFirst().orElseThrow();
		assertNoSyntaxErrors(parser.parse(new StringReader(source)));
		assertNoSyntaxErrors(parser.parse(rule, new StringReader(source)));
		final var ruleCall = EcoreUtil2.getAllContentsOfType(grammarAccess.getGrammar(), RuleCall.class)
				.stream()
				.filter(candidate -> candidate.getRule() instanceof ParserRule)
				.filter(candidate -> candidate.getRule().getName().equals("XJStatementOrBlock"))
				.findFirst().orElseThrow();
		assertNoSyntaxErrors(parser.parse(ruleCall, new StringReader(source), 0));

		final var initial = parser.parse(new StringReader(source));
		final var updated = parser.reparse(initial,
				new ReplaceRegion(source.lastIndexOf('1'), 1, "2"));
		assertNoSyntaxErrors(updated);
		assertEquals("2", EcoreUtil2.getAllContentsOfType(updated.getRootASTElement(),
				org.eclipse.xtext.xbase.XNumberLiteral.class).getFirst().getValue());
	}

	@Test
	public void parserWrapsReaderFailures() {
		final Reader failingReader = new Reader() {
			@Override
			public int read(final char[] buffer, final int offset, final int length)
					throws IOException {
				throw new IOException("read failed");
			}

			@Override
			public void close() {
				// no-op
			}
		};
		assertThrows(RuntimeException.class, () -> parser.parse(failingReader));
	}

	private void assertThrows(final Class<? extends RuntimeException> expected,
			final Runnable action) {
		try {
			action.run();
		} catch (final RuntimeException exception) {
			assertTrue(expected.isInstance(exception));
			return;
		}
		throw new AssertionError("Expected " + expected.getName());
	}

	private void assertNoSyntaxErrors(final org.eclipse.xtext.parser.IParseResult result) {
		assertFalse(result.getSyntaxErrors().iterator().hasNext());
	}
}
