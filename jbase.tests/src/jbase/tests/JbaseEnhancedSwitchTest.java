package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XSwitchExpression;

import com.google.inject.Inject;

import jbase.controlflow.JbaseSureReturnComputer;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;

/** Compiles native Java 21 switches and executes their generated code. */
@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseEnhancedSwitchTest extends JbaseAbstractCompilerTest {
	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private JbaseSureReturnComputer sureReturnComputer;

	@Test
	public void arrowExpressionsAndGroupedLabels() throws Exception {
		final var classes = new java.util.ArrayList<Class<?>>();
		compilationTestHelper.compile("""
			method length(int n) : int {
				return switch (n) {
					case 0, 1 -> 10;
					default -> 20;
				};
			}
			""", result -> {
			assertNoValidationErrors(result);
			assertTrue(result.getSingleGeneratedCode().contains("switch (n)"));
			assertTrue(result.getSingleGeneratedCode().contains("case 0, 1 ->"));
			classes.add(result.getCompiledClass());
		});
		final var instance = classes.getFirst().getConstructor().newInstance();
		assertEquals(10, instance.getClass().getMethod("length", int.class).invoke(instance, 1));
		assertEquals(20, instance.getClass().getMethod("length", int.class).invoke(instance, 5));
	}

	@Test
	public void patternsGuardsAndNullDefault() throws Exception {
		checkCompilation("""
			Object value = "hello";
			int length = switch (value) {
				case String s when s.length() > 2 -> s.length();
				case String s -> 0;
				case Integer n -> n.intValue();
				case null, default -> -1;
			};
			""", null);
	}

	@Test
	public void generatedJavaPreservesPatternsGuardsAndYield() throws Exception {
		checkCompilation("""
			Object value = "hello";
			int length = switch (value) {
				case String s when s.length() > 2 -> s.length();
				case String s -> 0;
				case Integer n -> {
					int result = n.intValue();
					yield result;
				}
				case null, default -> -1;
			};
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Object value = "hello";
			    int length = switch (value) {
			      case String s when (s.length() > 2) -> {
			        yield s.length();
			      }
			      case String s -> {
			        yield 0;
			      }
			      case Integer n -> {
			        int result = n.intValue();
			        yield result;
			      }
			      case null, default -> {
			        yield -1;
			      }
			    };
			  }
			}
			""");
	}

	@Test
	public void yieldBlocksAndNestedExpressions() throws Exception {
		checkCompilation("""
			int n = 1;
			var result = switch (n) {
				case 0 -> { yield 10; }
				case 1 -> {
					int next = n + 1;
					if (next > 1) yield next;
					yield -1;
				}
				default -> switch (n + 1) {
					case 2 -> 20;
					default -> 30;
				};
			};
			int doubled = result * 2;
			""", null);
	}

	@Test
	public void arrowStatementsAndDefaultInMiddle() throws Exception {
		checkCompilation("""
			int n = 1;
			switch (n) {
				case 0, 1 -> System.out.println(n);
				default -> { System.out.println("other"); }
				case 2 -> throw new IllegalArgumentException();
			}
			""", null);
	}

	@Test
	public void colonExpressionAndEnumExhaustiveness() throws Exception {
		checkCompilation("""
			import java.time.DayOfWeek;
			DayOfWeek day = DayOfWeek.MONDAY;
			int n = switch (day) {
				case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY: yield 1;
				case SATURDAY, SUNDAY: yield 2;
			};
			""", null);
	}

	@Test
	public void traditionalSwitchStatementsDoNotRequireExhaustiveness() throws Exception {
		checkCompilation("""
			import java.time.DayOfWeek;
			import static java.time.DayOfWeek.*;
			DayOfWeek day = DayOfWeek.MONDAY;
			switch (day) {
				case MONDAY: break;
			}
			switch (day) {
				case MONDAY: break;
				case TUESDAY: break;
				case WEDNESDAY: break;
				case THURSDAY: break;
				case FRIDAY: break;
				case SATURDAY: break;
				case SUNDAY: break;
			}
			""", null);
	}

	@Test
	public void patternScopeAndNullExecution() throws Exception {
		assertExecuteMain("""
			method describe(Object value) : String {
				return switch (value) {
					case null -> "null";
					case String s when s.length() > 2 -> s;
					case String s -> "short";
					default -> "other";
				};
			}
			method testMe() {
				System.out.println(describe(null));
				System.out.println(describe("hello"));
				System.out.println(describe("a"));
				System.out.println(describe(1));
			}
			""", """
			null
			hello
			short
			other
			""");
	}

	@Test
	public void expressionBranchesHaveIndependentScopes() throws Exception {
		checkCompilation("""
			Object value = "hello";
			int length = switch (value) {
				case String s -> s.length();
				case Object o -> o.hashCode();
			};
			""", null);
	}

	@Test
	public void arrowBranchesInitializeLocals() throws Exception {
		checkCompilation("""
			int n;
			switch (args.length) {
				case 0 -> { n = 1; }
				default -> { n = 2; }
			}
			System.out.println(n);
			""", null);
	}

	@Test
	public void reportsInvalidSwitches() throws Exception {
		assertError("int n = switch (1) { case 0 -> 1; };", "must be exhaustive");
		assertError("""
			Object value = "hello";
			int length = switch (value) { case String text -> text.length(); };
			""", "must be exhaustive");
		assertError("switch (1) { case 0 -> {} default: break; }", "Cannot mix");
		assertError("switch (1) { case 0: break; default -> {} }", "Cannot mix");
		assertError("switch (1L) { case 0 -> {} }", "selector type");
		assertError("switch (1) { case null -> {} default -> {} }", "null label");
		assertError("yield 1;", "requires an enclosing");
		assertError("switch (1) { default -> { yield 1; } }", "requires an enclosing");
		assertError("int n = switch (1) { default -> {} };", "must yield");
		assertError("int n = switch (1) { case 0 -> {} default -> 1; };", "must yield");
		assertError("int n = switch (1) { default -> { return 1; } };", "Cannot return");
		assertError("int n = switch (1) { default -> { break; } };", "Cannot jump");
		assertError("while (true) { int n = switch (1) { default -> { continue; } }; }",
			"Cannot jump");
		assertError("int n = switch (1) { default -> { Runnable r = () -> { yield 1; }; yield 2; } };",
			"requires an enclosing");
	}

	@Test
	public void patternVariablesDoNotEscapeTheirCase() throws Exception {
		assertError("""
			Object value = "hello";
			switch (value) {
				case String s -> System.out.println(s);
				default -> System.out.println(s);
			}
			""", "s is undefined");
	}

	@Test
	public void finalPatternsAndConstantTrueGuards() throws Exception {
		checkCompilation("""
			Object value = "hello";
			int n = switch (value) {
				case final String s -> s.length();
				case Object o when true -> o.hashCode();
			};
			""", null);
		assertError("""
			Object value = "hello";
			switch (value) {
				case final String s -> { s = "other"; }
				default -> {}
			}
			""", "final pattern variable");
		assertError("""
			Object value = "hello";
			switch (value) {
				case Object o when true -> {}
				case String s -> {}
			}
			""", "dominated");
	}

	@Test
	public void switchExpressionsInJavaContexts() throws Exception {
		checkCompilation("""
			int n = 1;
			System.out.println(switch (n) { default -> 1; });
			n = switch (n) { default -> 2; };
			int[] values = {switch (n) { default -> 3; }};
			final int captured = n;
			java.util.function.IntSupplier supplier = () -> switch (captured) {
				default -> 4;
			};
			""", null);
	}

	@Test
	public void throwRulesAndNestedControlFlow() throws Exception {
		checkCompilation("""
			int n = switch (args.length) {
				case 0 -> throw new IllegalArgumentException();
				default -> {
					for (int i = 0; i < 2; i++) {
						if (i == 1) break;
						continue;
					}
					java.util.function.IntSupplier supplier = () -> { return 2; };
					yield supplier.getAsInt();
				}
			};
			""", null);
	}

	@Test
	public void arrowStatementsReturnFromMethods() throws Exception {
		checkCompilation("""
			method size(int n) : int {
				switch (n) {
					case 0 -> { return 1; }
					default -> { return 2; }
				}
			}
			""", null);
		assertError("""
			method size(int n) : int {
				switch (n) {
					case 0 -> {}
					default -> { return 2; }
				}
			}
			""", "Missing return");
	}

	@Test
	public void contextualKeywordsRemainIdentifiers() throws Exception {
		checkCompilation("""
			int yield = 1;
			int when = 2;
			int n = switch (yield) { case 0 -> when; default -> yield; };
			""", null);
	}

	@Test
	public void exhaustivePatternStatementReturns() throws Exception {
		checkCompilation("""
			method size(Object value) : int {
				switch (value) {
					case String s -> { return s.length(); }
					case Object o -> { return 0; }
				}
			}
			""", null);
	}

	@Test
	public void yieldCanCrossNestedSwitchStatements() throws Exception {
		checkCompilation("""
			int n = switch (args.length) {
				default -> {
					switch (args.length) { default: yield 2; }
				}
			};
			""", null);
	}

	@Test
	public void guardsMustReadInitializedLocals() throws Exception {
		assertError("""
			Object value = "hello";
			int n;
			switch (value) {
				case String s when n > 0 -> {}
				default -> {}
			}
			""", "may not have been initialized");
	}

	@Test
	public void colonDefaultPositionPreservesFallThrough() throws Exception {
		assertExecuteMain("""
			method testMe() {
				int n = 5;
				switch (n) {
					default: System.out.println("default");
					case 1: System.out.println("one"); break;
					case 2: System.out.println("two"); break;
				}
			}
			""", """
			default
			one
			""");
	}

	@Test
	public void guardsRetainConditionalExpressions() throws Exception {
		checkCompilation("""
			Object value = "hello";
			int n = switch (value) {
				case String s when (s.isEmpty() ? false : s.length() > 1) -> s.length();
				default -> 0;
			};
			""", null);
	}

	@Test
	public void conditionalValuesArePreparedInsideTheirBranch() throws Exception {
		checkCompilation("""
			int first = switch (args.length) {
				default -> args.length > 1 ? 10 : 20;
			};
			int second = switch (args.length) {
				default -> { yield args.length > 1 ? 10 : 20; }
			};
			""", null);
	}

	@Test
	public void switchesAsConditionsAndStatementBodies() throws Exception {
		checkCompilation("""
			if (switch (args.length) { default -> true; })
				switch (args.length) { default -> System.out.println("yes"); }
			else switch (args.length) { default -> System.out.println("no"); }
			int n = true ? switch (args.length) { default -> 1; } : 2;
			""", null);
	}

	@Test
	public void yieldDoesNotReturnFromTheEnclosingMethod() throws Exception {
		final var model = parser.parse("int n = switch (1) { default -> { yield 2; } };");
		validationTestHelper.assertNoErrors(model);
		final var expression = EcoreUtil2.getAllContentsOfType(model, XSwitchExpression.class)
			.getFirst();
		assertFalse(sureReturnComputer.isSureReturn(expression));
	}

	@Test
	public void legacySelectorTypesWithArrowRules() throws Exception {
		checkCompilation("""
			Integer n = 1;
			String text = "hello";
			java.time.DayOfWeek day = java.time.DayOfWeek.MONDAY;
			Object object = text;
			switch (n) { case 1 -> {} default -> {} }
			switch (text) { case "hello" -> {} default -> {} }
			switch (day) { case MONDAY -> {} default -> {} }
			switch (object) { default -> {} }
			""", null);
	}

	@Test
	public void mixedCaseKindsAreRejected() throws Exception {
		assertError("switch (1) { case 0 -> {} case 1: break; }", "Cannot mix");
		assertError("switch (missing) { default -> {} }", "undefined");
		assertError("switch (1) { case default: break; }", "Missing case label");
		assertError("String s = \"a\"; switch (s) { case null, \"a\" -> {} default -> {} }",
			"null label must stand alone");
		assertError("Integer n = 1; switch (n) { case 1, null -> {} default -> {} }",
			"null label must stand alone");
	}

	@Test
	public void duplicateDefaultsAreSyntaxErrors() throws Exception {
		for (final var label : java.util.List.of("default", "case null, default")) {
			for (final var other : java.util.List.of("default", "case null, default")) {
				for (final var body : java.util.List.of(" -> {} ", ": break; ")) {
					final var source = "Object value = null; switch (value) { "
						+ label + body + other + body + "}";
					final var issues = validationTestHelper.validate(parseHelper.parse(source));
					assertTrue(issues.toString(), issues.stream()
						.anyMatch(issue -> Diagnostic.SYNTAX_DIAGNOSTIC.equals(issue.getCode())));
				}
			}
		}
	}

	@Test
	public void arrayWildcardAndMutableTypePatterns() throws Exception {
		checkCompilation("""
			Object value = "hello";
			int n = switch (value) {
				case String[] array -> array.length;
				case java.util.List<?> list -> list.size();
				case String text -> { text = text.trim(); yield text.length(); }
				case null, default -> 0;
			};
			""", null);
		assertError("""
			Object value = "hello";
			switch (value) {
				case String text -> System.out.println(value.length());
				default -> {}
			}
			""", "undefined");
	}

	@Test
	public void mutableInstanceOfAndSwitchPatternsExecuteTogether() throws Exception {
		assertExecuteMain("""
			method testMe() {
				Object value = " hello ";
				if (value instanceof String text) {
					text = text.trim();
					value = text;
				}
				String result = switch (value) {
					case String text -> {
						text = text.toUpperCase();
						yield text;
					}
					default -> "other";
				};
				result = result + "!";
				System.out.println(result);
			}
			""", "HELLO!\n");
	}

	@Test
	public void switchesAsLoopBodies() throws Exception {
		checkCompilation("""
			while (args.length > 5) switch (args.length) { default -> System.out.println("x"); }
			for (String a : args) switch (a) { case "x" -> System.out.println(a); default -> {} }
			for (int i = 0; i < 1; i++) switch (i) { default -> {} }
			""", null);
	}

	@Test
	public void boxedSelectorsPreserveReferencePatterns() throws Exception {
		final var source = """
			method testMe() {
				Long integral = 1L;
				Boolean flag = true;
				Float single = 2.0f;
				Double decimal = 3.0;
				int result = switch (integral) { case Long n -> n.intValue(); };
				result += switch (flag) { case Boolean b -> b.booleanValue() ? 1 : 0; };
				result += switch (single) { case Float n -> n.intValue(); };
				result += switch (decimal) { case Double n -> n.intValue(); };
				System.out.println(result);
				integral = null;
				switch (integral) {
					case null -> System.out.println("null");
					case Long n when n.longValue() > 0L -> System.out.println(n);
					default -> System.out.println("other");
				}
				switch (flag) { case Boolean b: System.out.println(b); }
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "7\nnull\ntrue\n");
	}

	@Test
	public void invalidSelectorTypes() throws Exception {
		assertError("boolean b = true; switch (b) { default -> {} }", "Invalid Java 21");
		assertError("long l = 1; switch (l) { default -> {} }", "Invalid Java 21");
		assertError("float f = 1; switch (f) { default -> {} }", "Invalid Java 21");
		assertError("double d = 1; switch (d) { default -> {} }", "Invalid Java 21");
	}

	@Test
	public void nonTotalPatternsAreNotExhaustive() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			Object o = "";
			switch (o) { case CharSequence c when false -> {} case String s -> {} default -> {} }
			"""));
		assertError("""
			Object o = "";
			switch (o) {
				case String s when s.isEmpty() -> {}
				case Integer i when false -> {}
				case Object x when false -> {}
			}
			""", "must be exhaustive");
	}

	@Test
	public void enumWithNullLabelIsNotExhaustive() throws Exception {
		assertError("""
			java.time.DayOfWeek d = java.time.DayOfWeek.MONDAY;
			switch (d) { case null -> {} case MONDAY -> {} }
			""", "must be exhaustive");
	}

	@Test
	public void nonExhaustiveStatementsMayFallOut() throws Exception {
		assertError("""
			method m(java.time.DayOfWeek d) : int {
				switch (d) { case MONDAY -> { return 1; } }
			}
			""", "return");
		checkCompilation("""
			method m(java.time.DayOfWeek d) : int {
				switch (d) { case MONDAY -> { return 1; } }
				return 2;
			}
			method n(int n) : int {
				switch (n) {
					case 1: System.out.println();
					case 2: return 2;
					default: return 3;
				}
			}
			""", null);
	}

	@Test
	public void throwingBlocksYieldNoValue() throws Exception {
		checkCompilation("""
			int n = switch (args.length) {
				case 1 -> { throw new RuntimeException(); }
				case 2 -> args.length > 0 ? 1 : 2;
				default -> 3;
			};
			""", null);
	}

	@Test
	public void statementSwitchRulesAreNotValues() throws Exception {
		assertError("Integer n = 1; switch (n) { case 1 -> n; default -> {} }", "side effects");
	}

	@Test
	public void colonLabelsOfLegacyStyleSwitches() throws Exception {
		checkCompilation("""
			Integer n = 1;
			switch (n) { case 1, 2: break; default: break; }
			switch (n) { case null: break; default: break; }
			""", null);
		assertError("int n = switch (1) { case 1 -> { System.out.println(); } default -> 2; };",
			"must yield");
	}

	@Test
	public void colonDefaultInSwitchExpression() throws Exception {
		checkCompilation("""
			int n = switch (args.length) {
				case 1: System.out.println(); yield 1;
				default: yield 3;
			};
			""", null);
	}

	private void assertError(final String source, final String message) throws Exception {
		final var issues = validationTestHelper.validate(parseHelper.parse(source));
		assertTrue(issues.toString(), issues.stream()
			.anyMatch(issue -> issue.getMessage().contains(message)));
	}

}
