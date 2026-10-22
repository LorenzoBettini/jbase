package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.formatter.FormatterTestHelper;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.compiler.GeneratorConfig;
import org.eclipse.xtext.xbase.compiler.ImportManager;
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator;
import org.eclipse.xtext.xbase.compiler.XbaseCompiler;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.conversion.JbaseTextBlockValueConverter;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;

/** Checks both the model value and the value of the emitted native Java text block. */
@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseTextBlockTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private ISerializer serializer;

	@Inject
	private FormatterTestHelper formatter;

	@Inject
	private JbaseTextBlockValueConverter converter;

	@Inject
	private XbaseCompiler compiler;

	@Inject
	private JvmModelGenerator generator;

	private record Example(String literal, String value) {
	}

	@Test
	public void javaValuesAndGeneratedJavaAgree() throws Exception {
		final var examples = List.of(
			new Example("\"\"\"\n\"\"\"", ""),
			new Example("\"\"\" \t\f\n  \"\"\"", ""),
			new Example("\"\"\"\n    first\n      second\n    \"\"\"", "first\n  second\n"),
			new Example("\"\"\"\n    first\n      second\n  \"\"\"", "  first\n    second\n"),
			new Example("\"\"\"\n    first\n      second\n\"\"\"", "    first\n      second\n"),
			new Example("\"\"\"\n    first\n      second\"\"\"", "first\n  second"),
			new Example("\"\"\"\n  text\n      \"\"\"", "text\n"),
			new Example("\"\"\"\n  first  \t\n \n  second\t \n  \"\"\"", "first\n\nsecond\n"),
			new Example("\"\"\"\n \n\t\n  \"\"\"", "\n\n"),
			new Example("\"\"\"\n\t  first\n  \t  second\n\t  \"\"\"", "first\n  second\n"),
			new Example("\"\"\"\n\f first\n  \"\"\"", "first\n"),
			new Example("\"\"\"\r\n  first\r\n  second\r\n  \"\"\"", "first\nsecond\n"),
			new Example("\"\"\"\r  first\r  second\r  \"\"\"", "first\nsecond\n"),
			new Example("\"\"\"\n  first\\\n    second\\\n  third\n  \"\"\"",
				"first  secondthird\n"),
			new Example("\"\"\"\r\n  first\\\r\n  second\"\"\"", "firstsecond"),
			new Example("\"\"\"\r  first\\\r  second\"\"\"", "firstsecond"),
			new Example("\"\"\"\n  first\\s\\s\n  \\s second\\040\n  \"\"\"",
				"first  \n  second \n"),
			new Example("\"\"\"\n  \\b\\t\\n\\f\\r\\'\\\"\\\\\\0\\12\\377\\400\n  \"\"\"",
				"\b\t\n\f\r'\"\\\0\n\377 0\n"),
			new Example("\"\"\"\n  \"quoted\" and \"\"pairs\"\" and \\\"\"\"triple\"\"\\\"\n  \"\"\"",
				"\"quoted\" and \"\"pairs\"\" and \"\"\"triple\"\"\"\n"),
			new Example("\"\"\"\n  \"\\\"\" then \"\"\\\" and \\\\ \n  \"\"\"",
				"\"\"\" then \"\"\" and \\\n"),
			new Example("\"\"\"\n  café Ελληνικά 😀\n  \"\"\"", "café Ελληνικά 😀\n"),
			new Example("\"\"\"\n  \\u0041\\uuuu0042\\u0022quoted\\u0022\n  \"\"\"",
				"AB\"quoted\"\n"),
			new Example("\"\"\"\n\\u0020\\u0020  first\n    second\n    \"\"\"",
				"first\nsecond\n"),
			new Example("\"\"\"\n  first\\u000a    second\\u000d\\u000a  third\n  \"\"\"",
				"first\n  second\nthird\n"),
			new Example("\"\"\"\n  \\\\u0041 \\\\\\u0041 \\u005c\\u005c\\u006e\n  \"\"\"",
				"\\u0041 \\A \\n\n"),
			new Example("\"\"\"\n  // /* not comments */ { ; }\n  \"\"\"",
				"// /* not comments */ { ; }\n"));
		final var input = new StringBuilder();
		for (var i = 0; i < examples.size(); i++) {
			input.append("method value").append(i).append("() : String { return ")
				.append(examples.get(i).literal()).append("; }\n");
		}
		final var model = parser.parse(input);
		assertTrue(model.eResource().getErrors().toString(),
			model.eResource().getErrors().isEmpty());
		final var literals = EcoreUtil2.getAllContentsOfType(model, XStringLiteral.class);
		assertEquals(examples.size(), literals.size());
		for (var i = 0; i < examples.size(); i++) {
			assertEquals("model value " + i, examples.get(i).value(), literals.get(i).getValue());
		}
		final var classes = new ArrayList<Class<?>>();
		compilationTestHelper.compile(input, result -> {
			assertNoValidationErrors(result);
			assertEquals(examples.size(),
				result.getSingleGeneratedCode().split("return \"\"\"\\R", -1).length - 1);
			classes.add(result.getCompiledClass());
		});
		final var instance = classes.getFirst().getConstructor().newInstance();
		for (var i = 0; i < examples.size(); i++) {
			assertEquals("Java value " + i, examples.get(i).value(),
				instance.getClass().getMethod("value" + i).invoke(instance));
		}
	}

	@Test
	public void nativeTextBlocksAreReindentedAtTheirGeneratedNestingLevel() throws Exception {
		checkCompilation("""
			method text(boolean nested) : String {
				if (nested) {
					return \"\"\"
						<div class="example">
						  content\\s
						</div>
						\"\"\";
				}
				return \"\"\"
				  fallback\"\"\";
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public String text(boolean nested) {
			    if (nested) {
			      return \"\"\"
			      <div class="example">
			        content\\s
			      </div>
			      \"\"\";
			    }
			    return \"\"\"
			    fallback\"\"\";
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void stringOperationsArraysAnnotationsAndConstantSwitchLabels() throws Exception {
		checkCompilation("""
			method @SuppressWarnings(\"\"\"
			  unused\"\"\") text(String input) : String {
				String[] values = {\"\"\"
				  first\"\"\", \"\"\"
				  second\"\"\"};
				switch (input) {
					case \"\"\"
					  first\"\"\": return values[0];
					default: return \"\"\"
					  second\"\"\".trim() + values[1];
				}
			}
			""", null);
	}

	@Test
	public void openingDelimiterRequiresALineTerminator() throws Exception {
		for (final var literal : List.of(
			"\"\"\"text\"\"\"", "\"\"\" text \"\"\"", "\"\"\" \t\ftext\"\"\"",
			"\"\"\"\"\"\"", "\"\"\"   \"\"\"", "\"\"\"text\n\"\"\"",
			"\"\"\"\\ntext\"\"\"")) {
			for (final var context : List.of(
				"String text = %s;",
				"method text() : String { return %s; }",
				"System.out.println(%s);",
				"String text = \"prefix\" + %s;")) {
				final var source = context.formatted(literal);
				final var model = parser.parse(source);
				assertTrue(source, validationTestHelper.validate(model).stream()
					.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
			}
		}
	}

	@Test
	public void closingDelimiterMayFollowContentOnTheSameLine() throws Exception {
		assertExecuteMain("""
			method testMe() {
				System.out.print(\"\"\"
				  single line\"\"\");
			}
			""", "single line");
	}

	@Test
	public void malformedTextBlocksHaveParsingErrors() throws Exception {
		for (final var literal : List.of(
			"\"\"\"same line\"\"\"", "\"\"\" \"\"\"", "\"\"\"\nunterminated",
			"\"\"\"\n  \\q\n  \"\"\"", "\"\"\"\n  \\8\n  \"\"\"",
			"\"\"\"\n  \\u005cu005a\n  \"\"\"",
			"\"\"\"\n  \\u0022\\u0022\\u0022\n  \"\"\"",
			"\"\"\"\n  \\u0022\"\"\"", "\"\"\"\n  \\u005c\"\"\"",
			"\"\"\"\n  \\\"\"\"", "\"\"\"\u000b\ntext\"\"\"")) {
			final var model = parser.parse("String text = " + literal + ";");
			assertFalse(literal, model.eResource().getErrors().isEmpty());
		}
	}

	@Test(expected = ValueConverterException.class)
	public void invalidEscapesAreReportedByTheConverter() {
		converter.toValue("\"\"\"\n\\q\"\"\"", null);
	}

	@Test(expected = ValueConverterException.class)
	public void malformedUnicodeEscapesAreReportedByTheConverter() {
		converter.toValue("\"\"\"\n\\uu12xy\"\"\"", null);
	}

	@Test(expected = ValueConverterException.class)
	public void incompleteUnicodeEscapesAreReportedByTheConverter() {
		converter.toValue("\"\"\"\n\\u", null);
	}

	@Test
	public void converterSerializationPreservesSignificantWhitespace() {
		for (final var value : List.of("", "  leading and trailing  ", "\n\n", "\r\n\t\f\b",
			"\"\"\" \\ café\n  ")) {
			assertEquals(value, converter.toValue(converter.toString(value), null));
		}
	}

	@Test
	public void serializationRetainsTheOriginalTextBlock() throws Exception {
		final var input = "var text = \"\"\"\n\t  first\\s\n\t    second\n\t  \"\"\";\n";
		final var model = parser.parse(input);
		assertEquals(input, serializer.serialize(model));
		final var copied = EcoreUtil.copy(model);
		final var reparsed = parser.parse(serializer.serialize(copied));
		assertTrue(reparsed.eResource().getErrors().toString(),
			reparsed.eResource().getErrors().isEmpty());
		assertEquals("first \n  second\n",
			EcoreUtil2.getAllContentsOfType(reparsed, XStringLiteral.class).getFirst().getValue());
	}

	@Test
	public void formattingPreservesTextBlockContent() {
		formatter.assertFormatted(request -> {
			request.setToBeFormatted(
				"String text=\"\"\"\n  first\n    second\\s\n  \"\"\";"
					.replace("\n", System.lineSeparator()));
			request.setExpectation(
				"String text = \"\"\"\n  first\n    second\\s\n  \"\"\";\n"
					.replace("\n", System.lineSeparator()));
		});
	}

	@Test
	public void syntheticStringsStillCompileWithoutANodeModel() throws Exception {
		final var model = parser.parse("method text() : String { return \"old\"; }");
		final var original = EcoreUtil2.getAllContentsOfType(model, XStringLiteral.class)
			.getFirst();
		final var replacement = XbaseFactory.eINSTANCE.createXStringLiteral();
		replacement.setValue("new\nvalue");
		EcoreUtil.replace(original, replacement);
		final var appendable = generator.createAppendable(
			model, new ImportManager(true), new GeneratorConfig());
		compiler.toJavaExpression(replacement, appendable);
		assertEquals("\"new\\nvalue\"", appendable.getContent());
	}

	@Test
	public void changedModelValuesTakePrecedenceOverTheOriginalTextBlock() throws Exception {
		final var model = parser.parse("method text() : String { return \"\"\"\n  old\"\"\"; }");
		final var literal = EcoreUtil2.getAllContentsOfType(model, XStringLiteral.class).getFirst();
		literal.setValue("new\nvalue ");
		final var appendable = generator.createAppendable(
			model, new ImportManager(true), new GeneratorConfig());
		compiler.toJavaExpression(literal, appendable);
		assertEquals("new\nvalue ", converter.toValue(appendable.getContent(), null));
	}
}
