package jbase.tests;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XCastedExpression;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.testlanguage.validation.JbaseTestlanguageValidator;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import jbase.validation.JbaseIssueCodes;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseLambdaTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> modelParser;

	@Inject
	private IBatchTypeResolver typeResolver;

	@Inject
	private JbaseTestlanguageValidator validator;

	// the validator is shared and other tests may leave the check disabled
	@Before
	public void initValidator() {
		validator.setSkipVariableInitializationCheck(false);
	}

	@Test
	public void varParameterTypesAreInferredFromTarget() throws Exception {
		final var model = modelParser.parse("""
			java.util.function.IntUnaryOperator identity = (var n) -> n;
			java.util.function.Function<String, Integer> length = (final var s) -> s.length();
			java.util.function.ToIntFunction<int[]> first = (var values) -> values[0];
			""");
		validationTestHelper.assertNoErrors(model);
		final var closures = EcoreUtil2.getAllContentsOfType(model, XClosure.class);
		final var types = typeResolver.resolveTypes(model);
		assertEquals(3, closures.size());
		for (final var closure : closures) {
			assertNull(closure.getDeclaredFormalParameters().getFirst().getParameterType());
		}
		assertEquals(List.of("int", "java.lang.String", "int[]"), closures.stream()
			.map(closure -> types.getActualType(closure.getDeclaredFormalParameters().getFirst())
				.getIdentifier()).toList());
	}

	@Test
	public void varParametersCompileInTargetTypingContexts() throws Exception {
		checkCompilation("""
			import java.util.function.*;
			import java.util.stream.Stream;
			method identity(): IntUnaryOperator { return (var n) -> n; }
			IntUnaryOperator mutable = (var n) -> { n++; return n; };
			IntBinaryOperator sum = (final var x, var y) -> x + y;
			Function<Integer, Function<Integer, Integer>> add = (var x) -> (var y) -> x + y;
			Stream.of("a", "bb").map((var s) -> s.length()).forEach((var n) ->
				System.out.println(n));
			IntUnaryOperator assigned;
			assigned = (var n) -> n;
			var cast = (IntUnaryOperator) (var n) -> n;
			IntUnaryOperator conditional = true ? (var n) -> n : (var n) -> -n;
			Consumer raw = (var x) -> System.out.println(x);
			Consumer<? super String> wildcard = (var x) -> System.out.println(x.length());
			IntUnaryOperator namedVar = (var var) -> var;
			""", null);
	}

	@Test
	public void generatedJavaUsesInferredVarParameterType() throws Exception {
		checkCompilation("""
			java.util.function.IntUnaryOperator identity = (var n) -> n;
			""", """
			package jbasetestlanguage;

			import java.util.function.IntUnaryOperator;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    final IntUnaryOperator _function = (int n) -> {
			      return n;
			    };
			    IntUnaryOperator identity = _function;
			  }
			}
			""");
	}

	@Test
	public void executeVarParameterLambdas() throws Exception {
		assertExecuteMain("""
			method testMe() {
				final int offset = 2;
				java.util.function.IntBinaryOperator add = (final var x, var y) -> {
					y++;
					return x + y + offset;
				};
				java.util.function.Function<String, Integer> length = (var s) -> s.length();
				System.out.println(add.applyAsInt(3, 4));
				System.out.println(length.apply("hello"));
			}
			""", "10\n5\n");
	}

	@Test
	public void varParametersCannotMixWithExplicitTypes() throws Exception {
		for (final var parameters : new String[] { "var x, int y", "int x, var y" }) {
			assertLambdaError("java.util.function.IntBinaryOperator sum = ("
				+ parameters + ") -> x + y;", JbaseIssueCodes.INVALID_LAMBDA);
		}
	}

	@Test
	public void varParameterRestrictions() throws Exception {
		assertLambdaError("var identity = (var n) -> n;", JbaseIssueCodes.INVALID_LAMBDA);
		assertHasErrors("java.util.function.IntUnaryOperator f = (final var n) -> ++n;");
		assertHasErrors("java.util.function.Function<String, Integer> f = (var s) -> s;");
		for (final var parameters : new String[] {
				"(var x, y)", "(x, var y)", "(x, int y)", "(int x, y)" }) {
			final var model = modelParser.parse("java.util.function.IntBinaryOperator f = "
				+ parameters + " -> 1;");
			assertTrue(validationTestHelper.validate(model).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
		for (final var parameters : new String[] { "var x", "(var[] x)", "(var... x)" }) {
			final var model = modelParser.parse("java.util.function.IntUnaryOperator f = "
				+ parameters + " -> 1;");
			assertTrue(validationTestHelper.validate(model).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
	}

	@Test
	public void lambdasWithInferredLocals() throws Exception {
		assertExecuteMain("""
			method testMe() {
				final var offset = 2;
				var add = (java.util.function.IntUnaryOperator) n -> {
					var result = n + offset;
					return result;
				};
				for (var value : new int[] {1, 2}) {
					java.util.function.IntSupplier read = () -> add.applyAsInt(value);
					System.out.println(read.getAsInt());
				}
			}
			""", "3\n4\n");
	}

	@Test
	public void inferredLocalLambdaNeedsTargetType() throws Exception {
		assertLambdaError("var task = () -> {};", JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("final var value = n -> n;", JbaseIssueCodes.INVALID_LAMBDA);
	}

	@Test
	public void inferredCapturesMustBeEffectivelyFinal() throws Exception {
		assertLambdaError("""
			var n = 1;
			java.util.function.IntSupplier read = () -> n;
			n++;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
	}

	@Test
	public void inferredParameterType() throws Exception {
		final var model = modelParser.parse("""
			java.util.function.Function<String, Integer> length = s -> s.length();
			""");
		validationTestHelper.assertNoErrors(model);
		final var closure = EcoreUtil2.getAllContentsOfType(model, XClosure.class).getFirst();
		assertTrue(closure.isExplicitSyntax());
		assertEquals(1, closure.getFormalParameters().size());
		assertEquals("java.lang.String", typeResolver.resolveTypes(model)
			.getActualType(closure.getFormalParameters().getFirst()).getIdentifier());
	}

	@Test
	public void expressionAndBlockBodies() throws Exception {
		checkCompilation("""
			import java.util.function.*;
			Function<String, Integer> a = s -> s.length();
			Function<String, Integer> b = (s) -> s.length();
			Function<String, Integer> c = (String s) -> { return s.length(); };
			BiFunction<Integer, Integer, Integer> sum = (x, y) -> x + y;
			Supplier<String> text = () -> "hello";
			Runnable task = () -> { System.out.println(text.get()); };
			Consumer<String> print = s -> System.out.println(s);
			""", null);
	}

	@Test
	public void nestedLambdasAndArguments() throws Exception {
		checkCompilation("""
			import java.util.function.*;
			import java.util.stream.*;
			Function<Integer, Function<Integer, Integer>> add = x -> y -> x + y;
			Stream.of("a", "bb").map(s -> s.length()).forEach(n -> System.out.println(n));
			Thread thread = new Thread(() -> System.out.println(add.apply(1).apply(2)));
			""", null);
	}

	@Test
	public void generatedJavaForNestedLambdasWithCapturedLocal() throws Exception {
		checkCompilation("""
			import java.util.function.IntFunction;
			import java.util.function.IntUnaryOperator;
			int offset;
			offset = 2;
			IntFunction<IntUnaryOperator> add = x -> y -> {
				y++;
				return x + y + offset;
			};
			""", """
			package jbasetestlanguage;

			import java.util.function.IntFunction;
			import java.util.function.IntUnaryOperator;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int offset;
			    offset = 2;
			    final IntFunction<IntUnaryOperator> _function = (int x) -> {
			      final IntUnaryOperator _function_1 = (int y) -> {
			        y++;
			        return ((x + y) + offset);
			      };
			      return _function_1;
			    };
			    IntFunction<IntUnaryOperator> add = _function;
			  }
			}
			""");
	}

	@Test
	public void assignmentCastConditionalAndReturn() throws Exception {
		checkCompilation("""
			import java.util.function.*;
			method supplier(): Supplier<String> { return () -> "returned"; }
			Supplier<String> s;
			s = () -> "assigned";
			Object o = (Runnable) () -> System.out.println(s.get());
			Supplier<String> c = true ? () -> "yes" : () -> "no";
			""", null);
	}

	@Test
	public void mutableParametersAndExplicitFinal() throws Exception {
		checkCompilation("""
			import java.util.function.*;
			IntUnaryOperator increment = x -> { x++; return x; };
			IntUnaryOperator typed = (int x) -> { x = x + 1; return x; };
			IntUnaryOperator immutable = (final int x) -> x + 1;
			""", null);
	}

	@Test
	public void effectivelyFinalCapture() throws Exception {
		checkCompilation("""
			int n = 2;
			java.util.function.IntSupplier supplier = () -> n;
			System.out.println(supplier.getAsInt());
			""", null);
	}

	@Test
	public void executeLambdaReturnsLocally() throws Exception {
		assertExecuteMain("""
			method testMe() {
				java.util.function.IntUnaryOperator abs = n -> {
					if (n < 0) return -n;
					return n;
				};
				System.out.println(abs.applyAsInt(-3));
				System.out.println("after");
			}
			""", "3\nafter\n");
	}

	@Test
	public void invalidTargetTypes() throws Exception {
		assertLambdaError("() -> 1;", JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("Object o = () -> 1;", JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("java.util.TimerTask task = () -> {};", JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("java.util.List<String> list = () -> 1;", JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("jbase.tests.util.LambdaFixtures.GenericOperation f = x -> x;",
			JbaseIssueCodes.INVALID_LAMBDA);
	}

	@Test
	public void missingReturnInBlock() throws Exception {
		assertLambdaError("java.util.function.IntSupplier f = () -> {};",
			JbaseIssueCodes.MISSING_RETURN);
		assertLambdaError("java.util.function.IntSupplier f = () -> { Math.abs(1); };",
			JbaseIssueCodes.MISSING_RETURN);
		assertLambdaError("""
			java.util.function.IntUnaryOperator f = n -> { if (n > 0) return n; };
			""", JbaseIssueCodes.MISSING_RETURN);
		assertLambdaError("java.util.function.IntSupplier f = () -> { for (;;) { break; } };",
			JbaseIssueCodes.MISSING_RETURN);
		assertLambdaError("""
			java.util.function.IntUnaryOperator f = n -> {
				switch (n) { case 0: break; default: return 1; }
			};
			""", JbaseIssueCodes.MISSING_RETURN);
	}

	@Test
	public void invalidVoidExpressionBody() throws Exception {
		assertLambdaError("Runnable r = () -> 1;", JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("java.util.function.Consumer<String> c = s -> s;",
			JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("java.util.function.IntConsumer c = n -> n + 1;",
			JbaseIssueCodes.INVALID_LAMBDA);
	}

	@Test
	public void incompatibleParameterAndReturnTypes() throws Exception {
		assertHasErrors("java.util.function.Function<String, Integer> f = s -> s;");
		assertHasErrors("java.util.function.IntUnaryOperator f = (String s) -> 1;");
		assertHasErrors("Runnable r = x -> System.out.println(x);");
		assertHasErrors("java.util.function.Consumer<String> c = () -> {};");
		assertHasErrors("Runnable r = () -> { return 1; };");
		assertHasErrors("java.util.function.IntSupplier s = () -> { return; };");
		assertHasErrors("java.util.function.IntUnaryOperator f = (final int n) -> ++n;");
		assertLambdaError("java.util.function.Consumer<String> c = (Object s) -> {};",
			JbaseIssueCodes.INVALID_LAMBDA);
	}

	@Test
	public void capturesMustBeEffectivelyFinal() throws Exception {
		for (final var write : new String[] { "n = 1", "n++", "++n", "n += 1" }) {
			assertLambdaError("int n = 0; " + write
				+ "; java.util.function.IntSupplier f = () -> n;",
				JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		}
		assertLambdaError("""
			int n = 0;
			java.util.function.IntSupplier f = () -> n;
			n = 1;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("""
			int n = 0;
			Runnable r = () -> { n = 1; };
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("""
			method m(int n) {
				n++;
				java.util.function.IntSupplier f = () -> n;
			}
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
	}

	@Test
	public void breakAndContinueCannotCrossLambdaBoundary() throws Exception {
		for (final var instruction : new String[] { "break", "continue" }) {
			assertLambdaError("while (true) { Runnable r = () -> { " + instruction + "; }; }",
				JbaseIssueCodes.INVALID_BRANCHING_STATEMENT);
		}
	}

	@Test
	public void lambdaParametersAreScoped() throws Exception {
		assertHasErrors("java.util.function.IntUnaryOperator f = n -> n; int x = n;");
		assertHasErrors("java.util.function.IntBinaryOperator f = (n, n) -> n;");
		assertHasErrors("int n = 0; java.util.function.IntUnaryOperator f = n -> n;");
		assertHasErrors("java.util.function.IntUnaryOperator f = n -> { int n = 1; return n; };");
	}

	@Test
	public void moreThanSixParameters() throws Exception {
		checkCompilation("""
			jbase.tests.util.LambdaFixtures.SevenArguments sum =
				(a, b, c, d, e, f, g) -> a + b + c + d + e + f + g;
			System.out.println(sum.apply(1, 2, 3, 4, 5, 6, 7));
			""", null);
	}

	@Test
	public void blankLocalAndArrayCaptures() throws Exception {
		checkCompilation("""
			int n;
			n = 2;
			java.util.function.IntSupplier supplier = () -> n;
			int[] values = new int[1];
			Runnable update = () -> { values[0] = supplier.getAsInt(); };
			""", null);
	}

	@Test
	public void blankLocalInitializedInEitherBranch() throws Exception {
		checkCompilation("""
			int n;
			if (System.currentTimeMillis() > 0) n = 1; else n = 2;
			java.util.function.IntSupplier supplier = () -> n;
			""", null);
	}

	@Test
	public void invalidBlankLocalCaptures() throws Exception {
		assertLambdaError("""
			int n;
			n = 1; n = 2;
			java.util.function.IntSupplier f = () -> n;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("""
			int n;
			Runnable r = () -> { n = 1; };
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("""
			int n;
			while (System.currentTimeMillis() > 0) n = 1;
			java.util.function.IntSupplier f = () -> n;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("""
			int n;
			if (args.length > 0) n = 1;
			n = 2;
			java.util.function.IntSupplier f = () -> n;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("""
			int n;
			if (args.length > 0) {} else n = 1;
			n = 2;
			java.util.function.IntSupplier f = () -> n;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
	}

	@Test
	public void executeLexicalThisAndCapturedState() throws Exception {
		assertExecuteMain("""
			value : int
			method testMe() {
				value = 3;
				java.util.function.Supplier<Object> self = () -> this;
				java.util.function.IntSupplier read = () -> value;
				System.out.println(self.get() == this);
				value = 4;
				System.out.println(read.getAsInt());
			}
			""", "true\n4\n");
	}

	@Test
	public void continueInsideLambdaDoesNotUpdateAnOuterLoop() throws Exception {
		assertExecuteMain("""
			method testMe() {
				for (int i = 0; i < 2; i += 1) {
					Runnable task = () -> {
						int j = 0;
						while (j++ < 1) continue;
					};
					Runnable each = () -> {
						for (int ignored : new int[] { 1 }) continue;
					};
					task.run();
					each.run();
					System.out.println("iteration");
				}
			}
			""", "iteration\niteration\n");
	}

	@Test
	public void localControlFlow() throws Exception {
		checkCompilation("""
			Runnable task = () -> {
				for (int i = 0; i < 3; i++) {
					if (i == 0) continue;
					break;
				}
				return;
			};
			java.util.function.IntSupplier fail = () -> {
				throw new IllegalStateException();
			};
			Runnable local = () -> {
				int n;
				n = 1;
				System.out.println(n);
			};
			""", null);
	}

	@Test
	public void rawAndWildcardTargets() throws Exception {
		checkCompilation("""
			java.util.function.Consumer raw = x -> System.out.println(x);
			java.util.function.Consumer<? super String> wildcard =
				(Object x) -> System.out.println(x);
			""", null);
	}

	@Test
	public void arrayAndVarargsParameters() throws Exception {
		checkCompilation("""
			java.util.function.ToIntFunction<String[]> length = (String... strings) -> strings.length;
			java.util.function.ToIntFunction<int[]> sum = (int[] values) -> values[0];
			""", null);
		assertHasErrors("""
			java.util.function.BiConsumer<String[], String> invalid =
				(String... strings, String last) -> {};
			""");
	}

	@Test
	public void voidStatementExpressionBodies() throws Exception {
		checkCompilation("""
			import java.util.function.IntConsumer;
			IntConsumer assignment = n -> n = 1;
			IntConsumer postfix = n -> n++;
			IntConsumer prefix = n -> ++n;
			IntConsumer compound = n -> n += 1;
			Runnable constructor = () -> new Object();
			""", null);
	}

	@Test
	public void blankCapturesAssignedByLoops() throws Exception {
		checkCompilation("""
			int n;
			for (n = 1; System.currentTimeMillis() < 0;) {}
			java.util.function.IntSupplier f = () -> n;
			""", null);
		checkCompilation("""
			int n;
			for (int element : new int[n = 1]) {}
			java.util.function.IntSupplier f = () -> n;
			""", null);
		assertLambdaError("""
			int n;
			for (int i = 0; i < 2; i++) n = 1;
			java.util.function.IntSupplier f = () -> n;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("""
			int n;
			for (String arg : args) n = 1;
			java.util.function.IntSupplier f = () -> n;
			""", JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
		assertLambdaError("int n; n++; java.util.function.IntSupplier f = () -> n;",
			JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE);
	}

	@Test
	public void uninitializedCapturesAreRejected() throws Exception {
		assertLambdaError("""
			int n;
			java.util.function.IntSupplier f = () -> n;
			""", JbaseIssueCodes.NOT_INITIALIZED_VARIABLE);
		assertLambdaError("""
			int n;
			Runnable task = () -> { n = 1; };
			System.out.println(n);
			""", JbaseIssueCodes.NOT_INITIALIZED_VARIABLE);
	}

	private void assertLambdaError(final String input, final String code) throws Exception {
		final var model = modelParser.parse(input);
		assertTrue(model.eResource().getErrors().toString(), model.eResource().getErrors().isEmpty());
		final var issues = validationTestHelper.validate(model);
		assertTrue(issues.toString(), issues.stream()
			.anyMatch(issue -> issue.getSeverity() == Severity.ERROR && code.equals(issue.getCode())));
	}

	private void assertHasErrors(final String input) throws Exception {
		final var model = modelParser.parse(input);
		assertTrue(model.eResource().getErrors().toString(), model.eResource().getErrors().isEmpty());
		assertTrue(validationTestHelper.validate(model).stream()
			.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
	}

	@Test
	public void captureReadsDoNotCountAsWrites() throws Exception {
		checkCompilation("""
			int n = 1;
			int other = 0;
			other += n;
			java.util.function.IntSupplier read = () -> n;
			""", null);
	}

	@Test
	public void incompleteLambdaCastStillComputesTypes() throws Exception {
		final var model = modelParser.parse("""
			Object value = (java.util.function.Supplier<String>) () -> "text";
			""");
		final var cast = EcoreUtil2.getAllContentsOfType(model, XCastedExpression.class).getFirst();
		cast.setType(null);
		assertTrue(validationTestHelper.validate(model).stream()
			.anyMatch(issue -> JbaseIssueCodes.INVALID_LAMBDA.equals(issue.getCode())));
	}

	@Test
	public void intersectionCastUsesItsUniqueFunctionalInterfaceBound() throws Exception {
		checkCompilation("""
			Object value = (java.io.Serializable & java.util.function.IntUnaryOperator)
				n -> n + 1;
			""", null);
	}

	@Test
	public void intersectionCastLambdaImplementsMarkerInterface() throws Exception {
		final var source = """
			method testMe(): void {
				Object value = (Runnable & java.io.Serializable) () -> {};
				System.out.println(value instanceof java.io.Serializable);
			}
			""";
		compilationTestHelper.compile(source, result -> assertTrue(result.getSingleGeneratedCode(),
			result.getSingleGeneratedCode().contains("(Runnable & Serializable)")));
		assertExecuteMain(source, "true\n");
	}

	@Test
	public void intersectionCastMethodReferenceImplementsMarkerInterface() throws Exception {
		assertExecuteMain("""
			method testMe(): void {
				Object value = (java.io.Serializable & java.util.function.Supplier<String>)
					String::new;
				System.out.println(value instanceof java.io.Serializable);
			}
			""", "true\n");
	}

	@Test
	public void intersectionCastRequiresExactlyOneFunctionalInterfaceBound() throws Exception {
		final var noFunctionalBound = """
			Object value = (java.io.Serializable & Cloneable) () -> {};
			""";
		assertLambdaError(noFunctionalBound, JbaseIssueCodes.INVALID_LAMBDA);
		assertLambdaError("""
			Object value = (Runnable & java.util.concurrent.Callable<Object>) () -> {};
			""", JbaseIssueCodes.INVALID_LAMBDA);
	}
}
