package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.resource.XtextResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import jbase.refactoring.JbaseFunctionalConversion;
import jbase.refactoring.JbaseFunctionalConversionValidator;

/** Exercises conversion against linked editor resources. */
@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseFunctionalConversionTest {
	@Inject private ParseHelper<EObject> parser;
	@Inject private JbaseFunctionalConversion conversion;
	@Inject private JbaseFunctionalConversionValidator validator;
	@Inject private IBatchTypeResolver typeResolver;

	@Test
	public void staticReference() throws Exception {
		assertConversion("java.util.function.Function<String, Integer> f = Integer::parseInt;",
			"(arg0) -> Integer.parseInt(arg0)");
	}

	@Test
	public void unboundReference() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = String::length;",
			"(arg0) -> arg0.length()");
	}

	@Test
	public void staticLambda() throws Exception {
		assertConversion("java.util.function.Function<String, Integer> f = "
			+ "s -> Integer.parseInt(s);", "Integer::parseInt");
	}

	@Test
	public void unboundLambda() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> s.length();", "java.lang.String::length");
	}

	@Test
	public void boundReceiverIsNotConverted() throws Exception {
		assertConversion("String text = \"a\"; java.util.function.Supplier<String> f = "
			+ "text::trim;", null);
	}

	@Test
	public void constructorReference() throws Exception {
		assertConversion("java.util.function.Supplier<String> f = String::new;",
			"() -> new String()");
	}

	@Test
	public void constructorLambda() throws Exception {
		assertConversion("java.util.function.Supplier<String> f = () -> new String();",
			"String::new");
	}

	@Test
	public void arrayReference() throws Exception {
		assertConversion("java.util.function.IntFunction<int[]> f = int[]::new;",
			"(arg0) -> new int[arg0]");
	}

	@Test
	public void arrayLambda() throws Exception {
		assertConversion("java.util.function.IntFunction<int[]> f = n -> new int[n];",
			"int[]::new");
	}

	@Test
	public void returnBlock() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> { return s.length(); };", "java.lang.String::length");
	}

	@Test
	public void nonForwardingLambdaIsNotConverted() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = s -> s.length() + 1;",
			null);
	}

	@Test
	public void boundReceiverLambdaIsNotConverted() throws Exception {
		assertConversion("String text = \"a\"; java.util.function.Supplier<String> f = "
			+ "() -> text.trim();", null);
	}

	@Test
	public void typedParameterIsNotConverted() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "(String s) -> s.length();", null);
	}

	@Test
	public void multipleStatementsAreNotConverted() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> { System.out.println(s); return s.length(); };", null);
	}

	@Test
	public void skipsCommentsInsideExpression() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> /* explanation */ s.length();", null);
	}

	@Test
	public void voidBlock() throws Exception {
		assertConversion("java.util.function.Consumer<String> f = "
			+ "s -> { s.trim(); };", "java.lang.String::trim");
	}

	@Test
	public void twoArgumentStaticReference() throws Exception {
		assertConversion("java.util.function.BiFunction<Integer, Integer, Integer> f = "
			+ "Integer::sum;", "(arg0, arg1) -> Integer.sum(arg0, arg1)");
	}

	@Test
	public void staticLambdaWithChangedArgumentIsNotConverted() throws Exception {
		assertConversion("java.util.function.Function<String, Integer> f = "
			+ "s -> Integer.parseInt(s.trim());", null);
	}

	@Test
	public void unboundLambdaWithChangedArgumentIsNotConverted() throws Exception {
		assertConversion("java.util.function.BiFunction<String, String, Integer> f = "
			+ "(s, t) -> s.indexOf(s);", null);
	}

	@Test
	public void explicitMethodTypeArgumentsArePreserved() throws Exception {
		assertConversion("java.util.function.Supplier<java.util.List<String>> f = "
			+ "java.util.Collections::<String>emptyList;",
			"() -> java.util.Collections.<String>emptyList()");
	}

	@Test
	public void explicitLambdaTypeArgumentsAreNotDiscarded() throws Exception {
		assertConversion("java.util.function.Supplier<java.util.List<String>> f = "
			+ "() -> java.util.Collections.<String>emptyList();", null);
	}

	@Test
	public void expressionReceiverIsNotConverted() throws Exception {
		assertConversion("java.util.function.Supplier<String> f = \"hello\"::trim;", null);
	}

	@Test
	public void thisReceiverCanBeConverted() throws Exception {
		assertConversion("method value(): int { return 1; } method test() { "
			+ "java.util.function.IntSupplier f = this::value; }",
			"() -> this.value()");
	}

	@Test
	public void unboundTwoArgumentLambda() throws Exception {
		assertConversion("java.util.function.BiFunction<String, String, Integer> f = "
			+ "(s, t) -> s.indexOf(t);", "java.lang.String::indexOf");
	}

	@Test
	public void constructorWithIgnoredArgumentIsNotConverted() throws Exception {
		assertConversion("java.util.function.Function<String, String> f = "
			+ "s -> new String();", null);
	}

	@Test
	public void arrayWithChangedDimensionIsNotConverted() throws Exception {
		assertConversion("java.util.function.IntFunction<int[]> f = "
			+ "n -> new int[n + 1];", null);
	}

	@Test
	public void emptyBlockIsNotConverted() throws Exception {
		assertConversion("Runnable f = () -> {};", null);
	}

	@Test
	public void nonFunctionalTargetIsNotConverted() throws Exception {
		assertConversion("Object f = () -> 1;", null);
	}

	@Test
	public void lineCommentIsNotDiscarded() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> // explanation\n s.length();", null);
	}

	@Test
	public void fieldAccessIsNotConverted() throws Exception {
		assertConversion("java.util.function.ToIntFunction<java.awt.Point> f = "
			+ "p -> p.x;", null);
	}

	@Test
	public void nestedCallIsNotConverted() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> s.trim().length();", null);
	}

	@Test
	public void multidimensionalArrayIsNotConverted() throws Exception {
		assertConversion("java.util.function.IntFunction<int[][]> f = "
			+ "n -> new int[n][];", null);
	}

	@Test
	public void duplicatedDimensionIsNotConverted() throws Exception {
		assertConversion("java.util.function.IntFunction<int[][]> f = "
			+ "n -> new int[n][n];", null);
	}

	@Test
	public void methodReferenceWithoutTargetIsNotConverted() throws Exception {
		assertConversion("var f = String::length;", null);
	}

	@Test
	public void lambdaWithoutTargetIsNotConverted() throws Exception {
		assertConversion("var f = s -> s;", null);
	}

	@Test
	public void arrayReferenceWithWrongArityIsNotConverted() throws Exception {
		assertConversion("java.util.function.BiFunction<Integer, Integer, int[]> f = "
			+ "int[]::new;", null);
	}

	@Test
	public void syntheticArrayMethodIsNotConverted() throws Exception {
		assertConversion("java.util.function.Function<int[], int[]> f = int[]::clone;",
			null);
	}

	@Test
	public void explicitConstructorTypeArgumentsAreNotDiscarded() throws Exception {
		assertConversion("java.util.function.Supplier<String> f = "
			+ "String::<String>new;", null);
	}

	@Test
	public void superReceiverCanBeConverted() throws Exception {
		assertConversion("method test() { java.util.function.Supplier<String> f = "
			+ "super::toString; }", "() -> super.toString()");
	}

	@Test
	public void nestedBlockIsNotConverted() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> {{ return s.length(); }};", null);
	}

	@Test
	public void incompleteLambdaIsNotConverted() throws Exception {
		final var source = "Runnable f = () -> {};";
		final var model = parser.parse(source);
		for (final var iterator = model.eAllContents(); iterator.hasNext();) {
			if (iterator.next() instanceof XClosure closure) {
				closure.setExpression(null);
				assertNull(conversion.convert(closure, source));
				return;
			}
		}
		throw new AssertionError("No lambda in test input");
	}

	@Test
	public void staticMethodCalledThroughParameterIsNotConverted() throws Exception {
		assertConversion("java.util.function.Function<String, String> f = "
			+ "s -> s.valueOf(s);", null);
	}

	@Test
	public void instanceMethodCalledThroughTypeIsNotConverted() throws Exception {
		assertConversion("java.util.function.ToIntFunction<String> f = "
			+ "s -> String.length();", null);
	}

	@Test
	public void validatesReferenceConversion() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = String::length;",
			"(arg0) -> arg0.length()", true);
	}

	@Test
	public void rejectsChangedExecutable() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = String::length;",
			"(arg0) -> arg0.indexOf(\"a\")", false);
	}

	@Test
	public void rejectsInvalidReplacement() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = String::length;",
			"(arg0) -> arg0.noSuchMethod()", false);
	}

	@Test
	public void validatesArrayReferenceConversion() throws Exception {
		assertValidation("java.util.function.IntFunction<int[]> f = int[]::new;",
			"(arg0) -> new int[arg0]", true);
	}

	@Test
	public void validatesLambdaConversion() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = s -> s.length();",
			"java.lang.String::length", true);
	}

	@Test
	public void validatesConstructorConversion() throws Exception {
		assertValidation("java.util.function.Supplier<String> f = String::new;",
			"() -> new String()", true);
	}

	@Test
	public void rejectsDifferentFunctionalTarget() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = String::length;",
			"(java.util.function.Function<String, Integer>) (s -> s.length())", false);
	}

	@Test
	public void rejectsReplacementWithoutClosure() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = String::length;",
			"null", false);
	}

	@Test
	public void rejectsNestedReplacementClosure() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = String::length;",
			"(java.util.function.ToIntFunction<String>) (s -> s.length())", false);
	}

	@Test
	public void rejectsMissingExecutable() throws Exception {
		assertValidation("Runnable f = () -> {};", "() -> {}", false);
	}

	@Test
	public void rejectsUnavailableCopy() throws Exception {
		final var source = "java.util.function.ToIntFunction<String> f = String::length;";
		final var original = parser.parse(source);
		for (final var iterator = original.eAllContents(); iterator.hasNext();) {
			if (iterator.next() instanceof XClosure closure) {
				assertEquals(false, validator.preservesBinding(closure, null, source,
					"(s) -> s.length()"));
				return;
			}
		}
		throw new AssertionError("No method reference in test input");
	}

	@Test
	public void rejectsMissingNewExecutable() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = String::length;",
			"s -> 1", false);
	}

	@Test
	public void rejectsDifferentArrayConstruction() throws Exception {
		assertValidation("java.util.function.IntFunction<int[]> f = int[]::new;",
			"n -> java.util.Arrays.copyOf(new int[] {}, n)", false);
	}

	@Test
	public void validatesReturnBlock() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = "
			+ "s -> { return s.length(); };", "java.lang.String::length", true);
	}

	@Test
	public void validatesTypedConstructorReference() throws Exception {
		assertValidation("java.util.function.Supplier<java.util.ArrayList<String>> f = "
			+ "java.util.ArrayList<String>::new;",
			"() -> new java.util.ArrayList<String>()", true);
	}

	@Test
	public void rejectsChangedFunctionalTarget() throws Exception {
		assertValidation("""
			method accept(java.util.function.ToIntFunction<String> f): int { return 1; }
			method accept(java.util.function.Function<String, String> f): int { return 2; }
			int selected = accept(String::length);
			""", "s -> s.trim()", false);
	}

	@Test
	public void validatesVoidBlock() throws Exception {
		assertValidation("java.util.function.Consumer<String> f = s -> { s.trim(); };",
			"String::trim", true);
	}

	@Test
	public void staleSourceWithDifferentTargetIsRejected() throws Exception {
		final var originalSource = "java.util.function.Function<String, Integer> f = String::length;";
		final var staleSource = "java.util.function.Function<String, String > f = String::length;";
		final var original = parser.parse(originalSource);
		final var copy = (XtextResource) parser.parse(staleSource).eResource();
		for (final var iterator = original.eAllContents(); iterator.hasNext();) {
			if (iterator.next() instanceof XClosure closure) {
				final var oldType = typeResolver.resolveTypes(closure).getActualType(closure);
				assertEquals(false, oldType.getJavaIdentifier().contains("String, String"));
				assertEquals(false, validator.preservesBinding(closure, copy, staleSource,
					"s -> s.trim()"));
				return;
			}
		}
		throw new AssertionError("No method reference in test input");
	}

	@Test
	public void nestedBlockHasNoExecutable() throws Exception {
		assertValidation("java.util.function.ToIntFunction<String> f = "
			+ "s -> {{ return s.length(); }};", "s -> s.length()", false);
	}

	private void assertValidation(final String source, final String replacement,
			final boolean expected) throws Exception {
		final var original = parser.parse(source);
		final var copy = (XtextResource) parser.parse(source).eResource();
		for (final var iterator = original.eAllContents(); iterator.hasNext();) {
			if (iterator.next() instanceof XClosure closure) {
				assertEquals(expected, validator.preservesBinding(closure, copy, source,
					replacement));
				return;
			}
		}
		throw new AssertionError("No functional expression in test input");
	}

	private void assertConversion(final String source, final String expected) throws Exception {
		final var model = parser.parse(source);
		String actual = null;
		for (final var iterator = model.eAllContents(); iterator.hasNext();) {
			if (iterator.next() instanceof XClosure closure) {
				actual = conversion.convert(closure, source);
				break;
			}
		}
		if (expected == null) {
			assertNull(actual);
		} else {
			assertEquals(expected, actual);
		}
	}
}
