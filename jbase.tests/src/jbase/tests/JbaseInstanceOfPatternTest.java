package jbase.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.testlanguage.validation.JbaseTestlanguageValidator;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseInstanceOfPatternTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private JbaseTestlanguageValidator validator;

	@Test
	public void checkedParameterizedNarrowing() throws Exception {
		checkCompilation("""
			java.util.List<String> values = new java.util.ArrayList<String>();
			if (values instanceof java.util.ArrayList<String> list) System.out.println(list);
			boolean ordinary = values instanceof java.util.ArrayList<String>;
			""", """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<String> values = new ArrayList<String>();
			    if ((values instanceof ArrayList<String> list)) {
			      System.out.println(list);
			    }
			    boolean ordinary = (values instanceof ArrayList<String>);
			  }
			}
			""");
	}

	@Test
	public void checkedParameterizedConversions() throws Exception {
		checkCompilation("""
			import java.util.List;
			import java.util.ArrayList;
			import jbase.testlanguage.tests.GenericPatternTypes;
			method test(List<String>[] arrays, List<? extends Number> bounded,
					List<? super Integer> lower, List<List<String>> nested,
					GenericPatternTypes<String>.Inner<Integer> inner) {
				if (arrays instanceof ArrayList<String>[] a) System.out.println(a.length);
				if (bounded instanceof ArrayList<? extends Number> a) System.out.println(a);
				if (lower instanceof ArrayList<? super Integer> a) System.out.println(a);
				if (nested instanceof GenericPatternTypes.Nested<String> a) System.out.println(a);
				List<String> values = new ArrayList<String>();
				if (values instanceof GenericPatternTypes.Extra<String, ?> a) System.out.println(a);
				boolean sameInner = inner instanceof GenericPatternTypes<String>.Inner<Integer>;
				boolean childInner = inner instanceof GenericPatternTypes<String>.Child<Integer>;
				boolean widening = new ArrayList<String>() instanceof List<String>;
				boolean containing = values instanceof ArrayList<? extends CharSequence>;
				boolean nilArray = null instanceof ArrayList<String>[];
				boolean nil = null instanceof ArrayList<String>;
			}
			""", null);
		assertExecuteMain("""
			method testMe() {
				java.util.List<String> values = new java.util.ArrayList<String>();
				values.add("hello");
				if (values instanceof java.util.ArrayList<String> list) {
					list.add("world");
					System.out.println(list.get(0) + " " + list.get(1));
				}
				System.out.println(values instanceof java.util.LinkedList<String>);
			}
			""", "hello world\nfalse\n");
	}

	@Test
	public void uncheckedParameterizedConversionsAreRejected() throws Exception {
		for (final var source : java.util.List.of(
			"Object value = null; boolean b = value instanceof java.util.ArrayList<String>;",
			"String value = null; boolean b = value instanceof java.util.ArrayList<String>;",
			"java.util.List value = null; boolean b = value instanceof java.util.ArrayList<String>;",
			"java.util.List<?> value = null; boolean b = value instanceof java.util.ArrayList<String>;",
			"java.util.List<Integer> value = null; boolean b = value instanceof java.util.ArrayList<String>;",
			"java.util.List<? extends Number> value = null; "
				+ "boolean b = value instanceof java.util.ArrayList<Number>;",
			"Object[] value = null; boolean b = value instanceof java.util.ArrayList<String>[];",
			"Object value = null; boolean b = value instanceof java.util.ArrayList<String>[];",
			"java.util.List<String> value = null; "
				+ "boolean b = value instanceof jbase.testlanguage.tests.GenericPatternTypes.Extra<String, Integer>;")) {
			assertTrue(source, validationTestHelper.validate(parser.parse(source)).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR
					&& issue.getMessage().contains("Cannot perform instanceof")));
		}
	}

	@Test
	public void branchScopesAndJavaGeneration() throws Exception {
		checkCompilation("""
			Object value = "hello";
			if (value instanceof String text) {
				System.out.println(text.length());
			} else {
				String text = "fallback";
				System.out.println(text);
			}
			if (!(value instanceof String text)) {
				String text = "fallback";
				System.out.println(text);
			} else {
				System.out.println(text.length());
			}
			""", null);
	}

	@Test
	public void preservesNativeSyntax() throws Exception {
		checkCompilation("""
			Object value = "hello";
			if (!(value instanceof final String text))
				System.out.println(value);
			else System.out.println(text);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Object value = "hello";
			    if ((!(value instanceof final String text))) {
			      System.out.println(value);
			    } else {
			      System.out.println(text);
			    }
			  }
			}
			""");
	}

	@Test
	public void booleanExpressionsAndNestedBranches() throws Exception {
		checkCompilation("""
			Object value = "hello";
			Object other = "world";
			if (value instanceof String a && a.length() > 0
					&& other instanceof String b && b.length() > a.length()) {
				System.out.println(a + b);
			}
			if (!(value instanceof String a) || a.isEmpty()) {
				System.out.println(value);
			} else {
				if (other instanceof String b) System.out.println(a + b);
			}
			if (!(value instanceof String a && other instanceof String b)) {
				System.out.println(value);
			} else System.out.println(a + b);
			if (!!(value instanceof String a)) System.out.println(a);
			boolean valid = value instanceof String a && a.length() > 1;
			int length = value instanceof String a ? a.length() : 0;
			int negated = !(value instanceof String a) ? 0 : a.length();
			boolean nested = (value instanceof String a ? a.isEmpty() : false) && true;
			boolean ordinary = (true ? true : false) && true;
			if (value instanceof String a && java.util.stream.Stream.of(a)
					.anyMatch(part -> part.equals(a))) System.out.println(a);
			""", null);
	}

	@Test
	public void assignmentsToNestedPatternVariables() throws Exception {
		final var source = """
			method testMe() {
				Object value = " hello ";
				Object other = " world ";
				if (value instanceof String text) {
					if (other instanceof String suffix) {
						suffix = text.trim() + " " + suffix.trim();
						text = suffix.toUpperCase();
						System.out.println(suffix);
						System.out.println(text);
					}
					System.out.println(text);
				}
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "hello world\nHELLO WORLD\nHELLO WORLD\n");
	}

	@Test
	public void guardClausesAndLoopScopes() throws Exception {
		checkCompilation("""
			method length(Object value): int {
				if (!(value instanceof String text)) return 0;
				return text.length();
			}
			method other(Object value): int {
				if (value instanceof String text) System.out.println(text);
				else throw new IllegalArgumentException();
				return text.length();
			}
			Object value = "hello";
			while (value instanceof String text && !text.isEmpty()) {
				System.out.println(text);
				value = null;
			}
			for (; value instanceof String text; System.out.println(text)) {
				System.out.println(text.length());
				value = null;
			}
			for (; value instanceof String text; new String()) value = null;
			""", null);
	}

	@Test
	public void arraysWildcardsAssignmentsAndCaptures() throws Exception {
		validator.setSkipVariableInitializationCheck(false);
		try {
			checkCompilation("""
				Object value = new String[] {"hello"};
				if (value instanceof String[] strings) System.out.println(strings[0]);
				if (value instanceof java.util.List<?> list) System.out.println(list.size());
				if (value instanceof String text) {
					text = text.trim();
					System.out.println(text);
				}
				if (value instanceof String text) {
					java.util.function.Supplier<String> supplier = () -> text;
					java.util.function.IntSupplier length = text::length;
					System.out.println(supplier.get());
				}
				""", null);
		} finally {
			validator.setSkipVariableInitializationCheck(true);
		}
	}

	@Test
	public void scopesAfterLoopConditions() throws Exception {
		checkCompilation("""
			Object value = "hello";
			while (!(value instanceof String text)) value = "hello";
			System.out.println(text);
			{
				Object other = "hello";
				do { other = "hello"; } while (!(other instanceof String found));
				System.out.println(found);
			}
			{
				Object other = "hello";
				for (; !(other instanceof String found);) other = "hello";
				System.out.println(found);
			}
			""", null);
	}

	@Test
	public void abruptBranchesAndNestedGuards() throws Exception {
		checkCompilation("""
			method guards(Object value, boolean flag) {
				if (!(value instanceof String text)) {
					if (flag) return;
					else throw new IllegalArgumentException();
				} else System.out.println(text);
				System.out.println(text);
			}
			method protectedGuard(Object value) {
				if (!(value instanceof String text)) {
					try { throw new IllegalArgumentException(); }
					finally { System.out.println(value); }
				}
				System.out.println(text);
			}
			method synchronizedGuard(Object value) {
				if (!(value instanceof String text)) synchronized (value) { return; }
				System.out.println(text);
			}
			method infiniteGuard(Object value) {
				if (!(value instanceof String text)) while (true) {}
				System.out.println(text);
			}
			method conditionalGuard(Object value, boolean flag) {
				if (!(value instanceof String text)) {
					if (flag) return;
					throw new IllegalArgumentException();
				}
				System.out.println(text);
			}
			method catchGuard(Object value) {
				if (!(value instanceof String text)) {
					try { throw new IllegalArgumentException(); }
					catch (RuntimeException e) { return; }
				}
				System.out.println(text);
			}
			method infiniteForGuard(Object value) {
				if (!(value instanceof String text)) for (;;) {}
				System.out.println(text);
			}
			method constantForGuard(Object value) {
				if (!(value instanceof String text)) for (;true;) {}
				System.out.println(text);
			}
			method breakGuard(Object value) {
				for (;;) {
					if (!(value instanceof String text)) break;
					System.out.println(text);
				}
			}
			""", null);
	}

	@Test
	public void abruptFinallyStillComputesTheJavaBindingScope() throws Exception {
		// Xbase separately forbids exits from finally. Verify the binding and generated
		// Java despite that existing diagnostic, without accepting other validation errors.
		compilationTestHelper.compile("""
			method guarded(Object value) {
				if (!(value instanceof String text)) {
					try { System.out.println(value); } finally { throw new RuntimeException(); }
				}
				System.out.println(text);
			}
			""", result -> {
			final var errors = result.getErrorsAndWarnings().stream()
				.filter(issue -> issue.getSeverity() == Severity.ERROR).toList();
			assertEquals(1, errors.size());
			assertEquals(org.eclipse.xtext.xbase.validation.IssueCodes.INVALID_EARLY_EXIT,
				errors.getFirst().getCode());
			assertGeneratedJavaCodeCompiles(result);
		});
	}

	@Test
	public void nestedBreakTargetsDoNotHideSuccessfulExitBinding() throws Exception {
		checkCompilation("""
			Object value = null;
			while (!(value instanceof String text)) {
				while (true) { break; }
				for (;;) { break; }
				for (Object item : new Object[] {value}) { break; }
				switch (0) { case 0: break; default: break; }
				value = "hello";
			}
			System.out.println(text);
			""", null);
	}

	@Test
	public void fieldShadowingAndExpressionEvaluation() throws Exception {
		assertExecuteMain("""
			text: int
			method source(): Object {
				this.text++;
				return "hello";
			}
			method testMe() {
				if (source() instanceof String text) {
					System.out.println(text);
					System.out.println(this.text);
				}
			}
			""", "hello\n1\n");
	}

	@Test
	public void patternNamesCannotCollideWithGeneratedLocals() throws Exception {
		checkCompilation("""
			Object value = "hello";
			if (value instanceof String _function) {
				java.util.function.Supplier<String> supplier = () -> _function;
				System.out.println(supplier.get());
			}
			if (value instanceof String _length) {
				System.out.println(_length.length() + _length.length());
			}
			if (value instanceof String _function) System.out.println(_function);
			""", null);
	}

	@Test
	public void executePatternMatching() throws Exception {
		assertExecuteMain("""
			method testMe() {
				Object value = "hello";
				if (value instanceof String text && text.length() > 0)
					System.out.println(text);
				if (!(value instanceof String text)) System.out.println("wrong");
				else System.out.println(text.length());
				value = null;
				if (value instanceof String text) System.out.println("wrong");
				else System.out.println("null");
			}
			""", "hello\n5\nnull\n");
	}

	@Test
	public void rejectOutOfScopeBindings() throws Exception {
		for (final var statement : new String[] {
			"if (value instanceof String text) {} else System.out.println(text);",
			"if (!(value instanceof String text)) System.out.println(text);",
			"if (value instanceof String text) {} System.out.println(text);",
			"if (value instanceof String text || text.isEmpty()) {}",
			"if (value instanceof String text & text.isEmpty()) {}",
			"if (value instanceof String text | text.isEmpty()) {}",
			"if (value instanceof String text == true) System.out.println(text);",
			"boolean b = value instanceof String text; if (b) System.out.println(text);",
			"if (value instanceof String text) { value.length(); }",
			"if (value instanceof String text) { text = 1; }",
			"if (value instanceof final String text) { text = \"x\"; }",
			"String text = \"\"; boolean b = value instanceof String text;",
			"if (value instanceof String text && value instanceof String text) {}",
			"while (value instanceof String text) {} System.out.println(text);",
			"int n = value instanceof String text ? 0 : text.length();",
			"boolean b = value instanceof final String;",
			"if (-(value instanceof String s)) System.out.println(s);",
			"boolean b = value instanceof int n;",
			"boolean b = value instanceof java.util.List<String> list;",
			"boolean b = value instanceof String s || value instanceof String s;",
			"boolean b = !(value instanceof String s) && !(value instanceof String s);",
			"boolean b = value instanceof String s ? true : value instanceof String s;",
			"while (!(value instanceof String s)) { break; } System.out.println(s);",
			"if (!(value instanceof String s)) { try { throw new RuntimeException(); } "
				+ "catch (RuntimeException e) {} } System.out.println(s);",
			"if (!(value instanceof String s)) { try { System.out.println(value); } "
				+ "catch (RuntimeException e) { return; } } System.out.println(s);",
			"if (!(value instanceof String s)) while (false) {} System.out.println(s);",
			"if (!(value instanceof String s)) while (true) { break; } System.out.println(s);",
			"if (!(value instanceof String s)) for (;false;) {} System.out.println(s);",
			"if (!(value instanceof String s)) for (;;) { break; } System.out.println(s);",
			"if (!(value instanceof String s)) { if (true) System.out.println(value); } "
				+ "System.out.println(s);"

		}) {
			final var source = "Object value = \"hello\"; " + statement;
			assertTrue(source, validationTestHelper.validate(parser.parse(source)).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
	}
}
