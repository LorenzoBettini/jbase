package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import jbase.jbase.XJSwitchExpression;
import jbase.jbase.XJGenericArrayTypeReference;
import jbase.jbase.XJInnerTypeReference;
import jbase.jbase.XJParameterizedTypeReference;
import jbase.jbase.XJWildcardTypeReference;
import jbase.compiler.JbaseTypeReferenceSerializer;
import org.apache.log4j.Level;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.serializer.impl.Serializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.logging.LoggingTester;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.compiler.StringBuilderBasedAppendable;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.resource.BatchLinkableResource;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseSerializerTest extends JbaseAbstractTest {

	/**
	 * Since we use both Jbase and JbaseTestlanguage we specify EObject
	 * as the type returned by ParseHelper
	 */
	@Inject
	protected ParseHelper<EObject> parseHelper;

	@Inject
	private Serializer serializer;

	@Inject
	private JbaseTypeReferenceSerializer typeReferenceSerializer;

	@Test
	public void annotationAwareTypeReferencesWithoutAnnotationsUseDefaultSerialization()
			throws Exception {
		final var parsed = parseHelper.parse("""
			import java.util.List;
			import java.util.Map;
			import java.util.Map.Entry;
			import jbase.tests.util.SourceAnnotation;

			@SourceAnnotation List<String> parameterized = null;
			List<@SourceAnnotation String> annotatedTypeArgument = null;
			String @SourceAnnotation [] array = null;
			Map.@SourceAnnotation Entry<String, String> inner = null;
			Map.@SourceAnnotation Entry rawInner = null;
			List<@SourceAnnotation ? extends String> wildcard = null;
			""");
		EcoreUtil.resolveAll(parsed);
		final var typeReferences = new java.util.ArrayList<JvmTypeReference>();
		typeReferences.addAll(EcoreUtil2.getAllContentsOfType(parsed,
			XJParameterizedTypeReference.class));
		typeReferences.addAll(EcoreUtil2.getAllContentsOfType(parsed,
			XJGenericArrayTypeReference.class));
		typeReferences.addAll(EcoreUtil2.getAllContentsOfType(parsed, XJInnerTypeReference.class));
		typeReferences.addAll(EcoreUtil2.getAllContentsOfType(parsed,
			XJWildcardTypeReference.class));
		assertTrue(typeReferences.stream().anyMatch(XJParameterizedTypeReference.class::isInstance));
		assertTrue(typeReferences.stream().anyMatch(XJGenericArrayTypeReference.class::isInstance));
		assertTrue(typeReferences.stream().anyMatch(XJInnerTypeReference.class::isInstance));
		assertTrue(typeReferences.stream().anyMatch(XJWildcardTypeReference.class::isInstance));
		for (final var typeReference : typeReferences) {
			switch (typeReference) {
				case XJParameterizedTypeReference reference -> reference.getSourceAnnotations().clear();
				case XJGenericArrayTypeReference reference -> reference.getSourceAnnotations().clear();
				case XJInnerTypeReference reference -> reference.getSourceAnnotations().clear();
				case XJWildcardTypeReference reference -> reference.getSourceAnnotations().clear();
				default -> throw new IllegalStateException("Unexpected type reference");
			}
			typeReferenceSerializer.serialize(typeReference, typeReference.eContainer(),
				new StringBuilderBasedAppendable(), false, false, false, false);
		}
	}

	@Test
	public void annotationSerializationRequiresTreeAppendable() throws Exception {
		final var parsed = parseHelper.parse("""
			import java.util.List;
			import jbase.tests.util.SourceAnnotation;
			List<@SourceAnnotation String> values = null;
			""");
		final var typeReference = EcoreUtil2.getAllContentsOfType(parsed,
			XJParameterizedTypeReference.class).getFirst();
		final var context = typeReference.eContainer();
		final var appendable = new StringBuilderBasedAppendable();
		try {
			typeReferenceSerializer.serialize(typeReference, context, appendable,
				false, false, false, false);
		} catch (final IllegalStateException expected) {
			assertEquals("Source annotations require a tree appendable", expected.getMessage());
			return;
		}
		fail("Expected an exception for a non-tree appendable");
	}

	@Test
	public void testAnonymousClasses() throws Exception {
		assertSerialize("""
			var value = new Object() { n: int method get(): int { return n; } };
			Runnable task = new Runnable() { method run(): void {} };
			Object empty = new Object() {};
			""");
	}

	@Test
	public void testAnonymousClassesWithoutNodeModel() throws Exception {
		final var source = "var value = new Object() { n: int "
			+ "method get(): int { return n; } }; Object marker = new Object() {};";
		final var parsed = parseHelper.parse(source);
		validationTestHelper.assertNoErrors(parsed);
		EcoreUtil.resolveAll(parsed);
		final var copy = EcoreUtil.copy(parsed);
		final var resource = parsed.eResource();
		resource.getContents().clear();
		resource.getContents().add(copy);
		assertEquals(source.replaceAll("\\s+", ""), serializer.serialize(copy).replaceAll("\\s+", ""));
	}

	@Test
	public void testRecordPatterns() throws Exception {
		assertSerialize("""
			Object value = null;
			if (value instanceof Box<Point>(Point(final int x, final var y))) {}
			int result = switch (value) {
				case Empty() -> 0;
				case Box(Point(var x, var y)) when x > 0 -> x + y;
				default -> -1;
			};
			switch (value) { case Point(var x, int y): break; default: break; }
			""");
	}

	@Test
	public void testRecordPatternsWithoutNodeModel() throws Exception {
		final var recordName = "jbase.testlanguage.tests.RecordPatternTypes.";
		final var source = "Object value = null; if (value instanceof " + recordName + "Box("
			+ recordName + "Point(var x, int y))) {} switch (value) { case " + recordName
			+ "Empty() -> {} case " + recordName + "Point(final var x, var y) -> {} default -> {} }";
		final var parsed = parseHelper.parse(source);
		validationTestHelper.assertNoErrors(parsed);
		EcoreUtil.resolveAll(parsed);
		final var copy = EcoreUtil.copy(parsed);
		final var resource = parsed.eResource();
		resource.getContents().clear();
		resource.getContents().add(copy);
		assertEquals(source.replaceAll("\\s+", ""), serializer.serialize(copy).replaceAll("\\s+", ""));
	}

	@Test
	public void testEnhancedSwitches() throws Exception {
		assertSerialize("""
			Object value = "hello";
			int n = switch (value) {
				case final String s when s.length() > 1 -> { yield s.length(); }
				case null, default -> 0;
			};
			switch (n) {
				case 0, 1 -> System.out.println(n);
				default -> { System.out.println("other"); }
			}
			""");
	}

	@Test
	public void testSwitchDefaultOrderAndColonExpressions() throws Exception {
		assertSerialize("""
			switch (1) { default: break; case 0: break; }
			switch (1) { case 0 -> {} default -> {} case 1 -> {} }
			switch (1) { case 0: System.out.println(); break; }
			switch (1) {}
			Object value = null;
			int n = switch (value) { case null, default: yield 1; };
			""");
	}

	@Test
	public void testDefaultLabelsAtEveryPosition() throws Exception {
		for (final var label : java.util.List.of("default", "case null, default")) {
			for (final var body : java.util.List.of(" -> {} ", ": break; ")) {
				for (var position = 0; position <= 2; position++) {
					final var parts = new java.util.ArrayList<>(java.util.List.of(
						"case 0" + body, "case 1" + body));
					parts.add(position, label + body);
					assertSerialize("switch (1) { " + String.join("", parts) + "}\n");
				}
			}
		}
	}

	@Test
	public void testSwitchWithoutNodeModel() throws Exception {
		final var source = "switch (1) { case 0 -> {} default -> {} }";
		final var parsedModel = parseHelper.parse(source);
		final var resource = parsedModel.eResource();
		final var model = EcoreUtil.copy(parsedModel);
		resource.getContents().clear();
		resource.getContents().add(model);
		assertEquals(source.replaceAll("\\s+", ""),
			serializer.serialize(model).replaceAll("\\s+", ""));
	}

	@Test
	public void testSwitchWithCaseWithoutNodeModel() throws Exception {
		final var source = "switch (1) { case 0 -> {} default -> {} }";
		final var model = parseHelper.parse(source);
		final var expression = EcoreUtil2.getAllContentsOfType(model, XJSwitchExpression.class)
			.getFirst();
		expression.getCases().set(0, EcoreUtil.copy(expression.getCases().getFirst()));
		assertEquals(source.replaceAll("\\s+", ""),
			serializer.serialize(model).replaceAll("\\s+", ""));
	}

	@Test
	public void testInstanceOfPatterns() throws Exception {
		assertSerialize("""
			Object value = "hello";
			if (value instanceof final String text && !text.isEmpty()) {
				System.out.println(text);
			} else if (!(value instanceof String text)) {
				System.out.println(value);
			} else System.out.println(text);
			""");
	}

	@Test
	public void testAssertStatements() throws Exception {
			assertSerialize("""
				assert true;
				assert true : "message";
				""");
	}

	@Test
	public void testAssertStatementsWithoutNodeModel() throws Exception {
			final var source = "assert true; assert true : \"message\";";
			final var parsed = parseHelper.parse(source);
			validationTestHelper.assertNoErrors(parsed);
			final var resource = parsed.eResource();
			final var model = EcoreUtil.copy(parsed);
			resource.getContents().clear();
			resource.getContents().add(model);
			assertEquals(source.replaceAll("\\s+", ""),
				serializer.serialize(model).replaceAll("\\s+", ""));
	}

	@Test
	public void testMethodReferences() throws Exception {
		assertSerialize("""
			java.util.function.ToIntFunction<String> length = String::length;
			java.util.function.Consumer<String> print = System.out::println;
			java.util.function.Supplier<java.util.ArrayList<String>> list =
				java.util.ArrayList<String>::new;
			java.util.function.IntFunction<int[][]> arrays = int[][]::new;
			""");
	}

	@Test
	public void testLambdas() throws Exception {
		assertSerialize("""
			java.util.function.Function<String, Integer> f = s -> s.length();
			java.util.function.BiFunction<Integer, Integer, Integer> sum = (x, y) -> x + y;
			java.util.function.IntUnaryOperator g = (final int x) -> { return x + 1; };
			java.util.function.IntUnaryOperator identity = (var n) -> n;
			java.util.function.IntBinaryOperator add = (final var x, var y) -> x + y;
			Runnable r = () -> {};
			""");
	}

	@Test
	public void testSourceAnnotationsWithoutNodeModel() throws Exception {
		final var source = """
			method annotated(@jbase.tests.util.SourceAnnotation String parameter): void {}
			@jbase.tests.util.SourceAnnotation int local = 1;
			final @jbase.tests.util.SourceAnnotation String text = "text";
			java.util.List<@jbase.tests.util.SourceAnnotation String> values = null;
			java.util.List<@jbase.tests.util.SourceAnnotation ? extends String> wildcard = null;
			String @jbase.tests.util.SourceAnnotation [] names = null;
			Object created = new @jbase.tests.util.SourceAnnotation String("text");
			java.util.function.Function<String, Integer> length =
				(@jbase.tests.util.SourceAnnotation var value) -> 1;
			for (@jbase.tests.util.SourceAnnotation String value : values) {}
			""";
		final var parsed = parseHelper.parse(source);
		validationTestHelper.assertNoErrors(parsed);
		EcoreUtil.resolveAll(parsed);
		final var copy = EcoreUtil.copy(parsed);
		final var resource = parsed.eResource();
		resource.getContents().clear();
		resource.getContents().add(copy);
		assertEquals(source.replaceAll("\\s+", ""), serializer.serialize(copy).replaceAll("\\s+", ""));
	}

	@Test
	public void testSourceAnnotationOnInnerType() throws Exception {
		assertSerialize("""
			import java.util.Map;
			import java.util.Map.Entry;

			Map.@jbase.tests.util.SourceAnnotation Entry<String, String> entry = null;
			""");
	}

	@Test
	public void testShiftAssignments() throws Exception {
		assertSerialize("""
			int n = 8;
			n <<= 1;
			n >>= 1;
			n >>>= 1;
			""");
	}

	@Test
	public void testAssignmentLeft() throws Exception {
		assertSerialize("""
			i = 1;
			""");
	}

	@Test
	public void testVariableDeclarations() throws Exception {
		assertSerialize("""
			int  i , j , k ;
			""");
	}

	@Test
	public void testMemberFeatureCall() throws Exception {
		assertSerialize("""
			System.out.println("a");
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccesses() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length;
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccesses2() throws Exception {
		assertSerialize("""
			args  [ 0 ] [ 1 ]   . length;
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccessesWithParenthesis() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length();
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccessesWithArguments() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length(0);
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccessesWithArguments2() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length(0,1);
			""");
	}

	@Test
	public void testBinaryOperator() throws Exception {
		assertSerialize("""
			for  (int  i  =  0  ; i  <  argsNum ;  i  +=  1 )  {
			\t
			}
			""");
	}

	@Test
	public void testBinaryOperator2() throws Exception {
		assertSerialize("""
			int j = 1 + 0;
			""");
	}

	@Test
	public void testBreak() throws Exception {
		assertSerialize("""
			while (true) {
				break ;
			}
			""");
	}

	@Test
	public void testBreak2() throws Exception {
		assertSerialize("""
			while (true)
				break ;
			""");
	}

	@Test
	public void testCast() throws Exception {
		assertSerialize("""
			(Object) o;
			""");
	}

	@Test
	public void testConditional() throws Exception {
		assertSerialize("""
			1 > 0 ? true : false;
			""");
	}

	@Test
	public void testArrayDimensions() throws Exception {
		assertSerialize("""
			new List[1][] {0, 1, 1 + 2};
			new List[1][2] {0, 1, 1 + 2};
			""");
	}

	@Test
	public void testArrayDimensions2() throws Exception {
		assertSerialize("""
			new List[][2] {0, 1, 1 + 2};
			new List[][] {0, 1, 1 + 2};
			new List[][];
			""");
	}

	@Test
	public void testArrayDimensions3() throws Exception {
		assertSerialize("""
			new List[][2][][1] {0, 1, 1 + 2};
			""");
	}

	@Test
	public void testBitwiseInclusiveOr() throws Exception {
		assertSerialize("""
			0 | 1;
			""");
	}

	@Test
	public void testBitwiseOperators() throws Exception {
		assertSerialize("""
			0 & 1;
			0 | 1;
			0 ^ 1;
			~1;
			""");
	}

	private void assertSerialize(final CharSequence input) throws Exception {
		final var o = parseHelper.parse(input);
		LoggingTester.captureLogging(Level.ERROR, BatchLinkableResource.class,
			() -> assertEquals(input.toString(), serializer.serialize(o))).assertNoLogEntries();
	}
}
