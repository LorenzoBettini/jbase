package jbase.tests;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import jbase.testlanguage.tests.SealedTypes;
import jbase.typesystem.JbaseSealedTypeResolver;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseRecordPatternTest extends JbaseAbstractCompilerTest {
	private static final String IMPORTS = """
		import jbase.testlanguage.tests.RecordPatternTypes.Point;
		import jbase.testlanguage.tests.RecordPatternTypes.Segment;
		import jbase.testlanguage.tests.RecordPatternTypes.Box;
		import jbase.testlanguage.tests.RecordPatternTypes.NumberBox;
		import jbase.testlanguage.tests.RecordPatternTypes.Value;
		import jbase.testlanguage.tests.RecordPatternTypes.Implementing;
		import jbase.testlanguage.tests.RecordPatternTypes.Pair;
		import jbase.testlanguage.tests.RecordPatternTypes.Data;
		import jbase.testlanguage.tests.RecordPatternTypes.Empty;
		import jbase.testlanguage.tests.RecordPatternTypes.Counted;
		import jbase.testlanguage.tests.RecordPatternTypes.Failing;
		""";

	private static final String SEALED_IMPORTS = """
		import jbase.testlanguage.tests.SealedTypes.Shape;
		import jbase.testlanguage.tests.SealedTypes.Circle;
		import jbase.testlanguage.tests.SealedTypes.Square;
		import jbase.testlanguage.tests.SealedTypes.AbstractShape;
		import jbase.testlanguage.tests.SealedTypes.AbstractCircle;
		import jbase.testlanguage.tests.SealedTypes.AbstractSquare;
		import jbase.testlanguage.tests.SealedTypes.ConcreteShape;
		import jbase.testlanguage.tests.SealedTypes.ConcreteCircle;
		import jbase.testlanguage.tests.SealedTypes.ConcreteHierarchy;
		import jbase.testlanguage.tests.SealedTypes.ConcreteEnd;
		import jbase.testlanguage.tests.SealedTypes.Nested;
		import jbase.testlanguage.tests.SealedTypes.NestedLeft;
		import jbase.testlanguage.tests.SealedTypes.NestedRight;
		import jbase.testlanguage.tests.SealedTypes.NestedEnd;
		import jbase.testlanguage.tests.SealedTypes.Records;
		import jbase.testlanguage.tests.SealedTypes.IntRecord;
		import jbase.testlanguage.tests.SealedTypes.RecordEnd;
		import jbase.testlanguage.tests.SealedTypes.OpenHierarchy;
		import jbase.testlanguage.tests.SealedTypes.OpenBranch;
		import jbase.testlanguage.tests.SealedTypes.ClosedBranch;
		import jbase.testlanguage.tests.SealedTypes.Generic;
		import jbase.testlanguage.tests.SealedTypes.GenericString;
		import jbase.testlanguage.tests.SealedTypes.GenericInteger;
		import jbase.testlanguage.tests.SealedTypes.EnumHierarchy;
		import jbase.testlanguage.tests.SealedTypes.Color;
		""";

	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private IBatchTypeResolver batchTypeResolver;

	@Test
	public void nativeJavaGeneration() throws Exception {
		checkCompilation("""
			import jbase.testlanguage.tests.RecordPatternTypes.Point;
			Object value = new Point(1, 2);
			if (value instanceof Point(final int x, var y)) System.out.println(x + y);
			int sum = switch (value) {
				case Point(var x, int y) when x > 0 -> x + y;
				default -> 0;
			};
			""", """
			package jbasetestlanguage;

			import jbase.testlanguage.tests.RecordPatternTypes;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Object value = new RecordPatternTypes.Point(1, 2);
			    if ((value instanceof RecordPatternTypes.Point(final int x, var y))) {
			      System.out.println((x + y));
			    }
			    int sum = switch (value) {
			      case RecordPatternTypes.Point(var x, int y) when (x > 0) -> {
			        yield (x + y);
			      }
			      default -> {
			        yield 0;
			      }
			    };
			  }
			}
			""");
	}

	@Test
	public void nestedPatternsAndRuntimeMatching() throws Exception {
		final var source = IMPORTS + """
			method testMe() {
				Object value = new Segment(new Point(1, 2), new Point(3, 4));
				if (value instanceof Segment(Point(int x, var y), Point(var a, int b))) {
					y = y + x;
					y++;
					System.out.println(y + a + b);
				}
				int sum = switch (value) {
					case Segment(Point(var x, var y), Point(var a, var b)) when x > 0 -> x + y + a + b;
					default -> 0;
				};
				System.out.println(sum);
				System.out.println(null instanceof Empty());
				System.out.println(new Empty() instanceof Empty());
				Box<String> nullable = new Box<String>(null);
				if (nullable instanceof Box<String>(String text)) System.out.println(text);
				Box<Point> missing = new Box<Point>(null);
				System.out.println(missing instanceof Box(Point(var x, var y)));
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "11\n10\nfalse\ntrue\nnull\nfalse\n");
	}

	@Test
	public void genericInferenceAndExhaustiveness() throws Exception {
		checkCompilation(IMPORTS + """
			method text(Box<String> box) : String {
				return switch (box) { case Box(var value) -> value.trim(); };
			}
			method nested(Box<Box<String>> box) : String {
				return switch (box) { case Box(Box(var value)) -> value.trim(); };
			}
			method bounded(Box<? extends CharSequence> box) : int {
				return switch (box) { case Box(var value) -> value.length(); };
			}
			method raw(Box box) {
				if (box instanceof Box(var value)) System.out.println(value);
			}
			method throughInterface(Value<String> source) : String {
				if (source instanceof Implementing(var value)) return value.trim();
				return "other";
			}
			method boundedRecord(Object source) {
				if (source instanceof NumberBox(var value)) System.out.println(value.intValue());
			}
			method point(Point point) : int {
				return switch (point) { case Point(int x, int y) -> x + y; };
			}
			method data(Data data) {
				if (data instanceof Data(final var text, int[] numbers, java.util.List<String> words)) {
					java.util.function.Supplier<String> supplier = () -> text;
					System.out.println(supplier.get() + numbers.length + words.get(0));
				}
			}
			Object unknown = new Box<String>("hello");
			if (unknown instanceof Box(var value)) System.out.println(value);
			if (unknown instanceof Box(String value)) System.out.println(value.length());
			""", null);
	}

	@Test
	public void sealedHierarchyExhaustiveness() throws Exception {
		final var source = SEALED_IMPORTS + """
			method shapeSize(Shape shape) : int {
				return switch (shape) {
					case null -> 0;
					case Circle circle -> circle.radius();
					case Square square -> square.side();
				};
			}
			method abstractShapeSize(AbstractShape shape) : int {
				return switch (shape) {
					case AbstractCircle circle -> circle.radius();
					case AbstractSquare square -> square.side();
				};
			}
			method concreteShapeSize(ConcreteShape shape) : int {
				return switch (shape) {
					case ConcreteCircle circle -> circle.size();
					case ConcreteShape base -> base.size();
				};
			}
			method nestedSize(Nested nested) : int {
				return switch (nested) {
					case NestedLeft left -> 1;
					case NestedRight right -> 2;
					case NestedEnd end -> 3;
				};
			}
			method recordSize(Records value) : int {
				return switch (value) {
					case IntRecord(int size) -> size;
					case RecordEnd end -> 0;
				};
			}
			method openSize(OpenHierarchy value) : int {
				return switch (value) {
					case OpenBranch open -> 1;
					case ClosedBranch closed -> 2;
				};
			}
			method genericSize(Generic<String> value) : int {
				return switch (value) {
					case GenericString string -> string.value().length();
				};
			}
			method rawGenericSize(Generic value) : int {
				return switch (value) {
					case GenericString string -> string.value().length();
					case GenericInteger integer -> integer.value().intValue();
				};
			}
			method enumSize(EnumHierarchy value) : int {
				return switch (value) {
					case Color.RED -> 0;
					case Color.BLUE -> 1;
				};
			}
			""";
		checkCompilation(source, null);
	}

	@Test
	public void incompleteSealedHierarchySwitchesAreRejected() throws Exception {
		assertError(SEALED_IMPORTS + """
			Shape shape = null;
			int size = switch (shape) {
				case Circle circle -> circle.radius();
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			AbstractShape shape = null;
			int size = switch (shape) {
				case AbstractCircle circle -> circle.radius();
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			ConcreteShape shape = null;
			int size = switch (shape) {
				case ConcreteCircle circle -> circle.size();
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			ConcreteHierarchy shape = null;
			int size = switch (shape) {
				case ConcreteCircle circle -> circle.size();
				case ConcreteEnd end -> 0;
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			Shape shape = null;
			int size = switch (shape) {
				case Circle circle when circle.radius() > 0 -> circle.radius();
				case Square square -> square.side();
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			OpenHierarchy value = null;
			int size = switch (value) {
				case ClosedBranch closed -> 0;
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			Nested value = null;
			int size = switch (value) {
				case NestedLeft left -> 0;
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			boolean value = true;
			int size = switch (value) {
				case true -> 1;
			};
			""", "must be exhaustive");
		assertError(SEALED_IMPORTS + """
			Shape value = null;
			int size = switch (value) {
				case true -> 1;
			};
			""", "must be exhaustive");
		assertError("""
			import java.time.DayOfWeek;
			int size = switch (DayOfWeek.MONDAY) {
				case MONDAY -> 1;
			};
			""", "must be exhaustive");
	}

	@Test
	public void sealedTypeResolutionHandlesMissingTypesAndResources() throws Exception {
		final var expression = shapeSwitch();
		final var selector = batchTypeResolver.resolveTypes(expression)
			.getActualType(expression.getSwitch());
		final var missingClassLoader = new ClassLoader(SealedTypes.class.getClassLoader()) {
			@Override
			protected Class<?> loadClass(final String name, final boolean resolve)
					throws ClassNotFoundException {
				if (name.equals(SealedTypes.Shape.class.getName())) {
					throw new ClassNotFoundException(name);
				}
				return super.loadClass(name, resolve);
			}
		};
		final var factory = new MissingTypeProviderFactory();
		final var unavailable = new TestSealedTypeResolver(missingClassLoader, factory);
		final List<LightweightTypeReference> unavailableTypes =
			unavailable.permittedTypes(expression, selector);
		assertTrue(unavailableTypes.isEmpty());

		final var missingPermittedType = new TestSealedTypeResolver(
			SealedTypes.Shape.class.getClassLoader(), factory);
		assertTrue(missingPermittedType.permittedTypes(expression, selector).isEmpty());
		assertTrue(missingPermittedType.permittedTypes(null, selector).isEmpty());
		assertTrue(missingPermittedType.permittedTypes(
			org.eclipse.emf.ecore.EcoreFactory.eINSTANCE.createEObject(), selector).isEmpty());
	}

	private XSwitchExpression shapeSwitch() throws Exception {
		return EcoreUtil2.getAllContentsOfType(parser.parse(SEALED_IMPORTS + """
			Shape shape = null;
			int size = switch (shape) {
				case Circle circle -> circle.radius();
				case Square square -> square.side();
			};
			"""), XSwitchExpression.class).getFirst();
	}

	private static final class TestSealedTypeResolver extends JbaseSealedTypeResolver {
		TestSealedTypeResolver(final ClassLoader classLoader,
				final IJvmTypeProvider.Factory typeProviderFactory) {
			super(classLoader, typeProviderFactory);
		}
	}

	private static final class MissingTypeProviderFactory implements IJvmTypeProvider.Factory {
		private final IJvmTypeProvider typeProvider = new IJvmTypeProvider() {
			@Override
			public JvmType findTypeByName(final String name) {
				return null;
			}

			@Override
			public JvmType findTypeByName(final String name, final boolean binaryNestedTypeDelimiter) {
				return null;
			}

			@Override
			public ResourceSet getResourceSet() {
				return null;
			}
		};

		@Override
		public IJvmTypeProvider createTypeProvider(final ResourceSet resourceSet) {
			return typeProvider;
		}

		@Override
		public IJvmTypeProvider findTypeProvider(final ResourceSet resourceSet) {
			return typeProvider;
		}

		@Override
		public IJvmTypeProvider findOrCreateTypeProvider(final ResourceSet resourceSet) {
			return typeProvider;
		}

		@Override
		public IJvmTypeProvider createTypeProvider() {
			return typeProvider;
		}
	}

	@Test
	public void flowScopesAndColonSwitches() throws Exception {
		checkCompilation(IMPORTS + """
			method guarded(Object value) : int {
				if (!(value instanceof Point(var x, var y))) return 0;
				return x + y;
			}
			Object value = new Point(1, 2);
			if (value instanceof Point(var x, var y) && x > y) System.out.println(x);
			if (!(value instanceof Point(var x, var y)) || x > y) {}
			else System.out.println(y);
			int selected = value instanceof Point(var x, var y) ? x + y : 0;
			while (value instanceof Point(var x, var y)) { System.out.println(x); value = null; }
			for (; value instanceof Point(var x, var y); System.out.println(y)) value = null;
			switch (value) {
				case Point(var x, var y) when x > y: System.out.println(x); break;
				case Empty(): break;
				default: break;
			}
			""", null);
	}

	@Test
	public void partialPatternsDoNotDominateWholeRecords() throws Exception {
		checkCompilation(IMPORTS + """
			Object value = new Pair("a", "b");
			switch (value) {
				case Box(Point(var x, var y)) -> System.out.println(x);
				case Box(Empty()) -> {}
				case Box(Point point) -> System.out.println(point);
				case Box(var content) -> System.out.println(content);
				default -> {}
			}
			switch (value) {
				case Pair(String left, String right) -> System.out.println(left + right);
				case Pair(var left, Integer right) -> System.out.println(right);
				case Pair pair -> System.out.println(pair);
				default -> {}
			}
			""", null);
	}

	@Test
	public void nativeAccessorEvaluationAndFailures() throws Exception {
		final var source = IMPORTS + """
			method testMe() {
				Counted.calls = 0;
				Object value = new Counted(7);
				if (value instanceof Counted(var n)) System.out.println(n);
				System.out.println(Counted.calls);
				try {
					Object broken = new Failing(1);
					boolean matched = broken instanceof Failing(var n);
				} catch (MatchException e) {
					System.out.println(e.getCause().getMessage());
				}
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "7\n1\naccessor\n");
	}

	@Test
	public void boxedComponentsUseSwitchTargetConversions() throws Exception {
		final var source = IMPORTS + """
			method testMe() {
				Box<Integer> integral = new Box<Integer>(7);
				int primitive = switch (integral) { case Box(var n) -> n; };
				long widened = switch (integral) { case Box(var n) -> { yield n; } };
				Integer boxed = switch (integral) { case Box(var n) -> n.intValue(); };
				System.out.println(primitive + widened + boxed);
				Boolean flag = true;
				boolean bool = switch (flag) { case Boolean b -> b; };
				Float single = 2.0f;
				float fraction = switch (single) { case Float n -> n; };
				Double decimal = 3.0;
				double number = switch (decimal) { case Double n -> n; };
				System.out.println(bool);
				System.out.println(fraction + number);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "21\ntrue\n5.0\n");
	}

	@Test
	public void invalidPatternsAndScopes() throws Exception {
		for (final var statement : java.util.List.of(
			"boolean b = value instanceof String();",
			"boolean b = value instanceof java.lang.Record();",
			"boolean b = value instanceof Point(var x);",
			"boolean b = value instanceof Point(var x, var y, var z);",
			"boolean b = value instanceof Point(var x, var y, Point(var a, var b));",
			"boolean b = value instanceof Point(Point(var a, var b), var y);",
			"boolean b = value instanceof Point(long x, int y);",
			"boolean b = value instanceof Point(Integer x, int y);",
			"boolean b = value instanceof Data(int text, var numbers, var words);",
			"boolean b = value instanceof Data(Integer text, var numbers, var words);",
			"boolean b = value instanceof Point(var x, var x);",
			"boolean b = value instanceof Point(var x, var y); System.out.println(x);",
			"boolean b = value instanceof Point(var x, var y) || x > 0;",
			"if (value instanceof Point(final int x, var y)) x = 1;",
			"if (value instanceof Point(final var x, var y)) x++;",
			"boolean b = value instanceof final Point(var x, var y);",
			"boolean b = value instanceof Segment(final Point(var x, var y), var end);",
			"boolean b = value instanceof Box<String>(var text);",
			"boolean b = value instanceof Box(java.util.List<String> words);",
			"switch (value) { case final Point(var x, var y) -> {} default -> {} }",
			"switch (value) { case Point(var x, var y) -> {} case Point(var a, var b) -> {} default -> {} }",
			"switch (value) { case Pair(var x, var y) -> {} case Pair(Point(var a, var b), var z) -> {} default -> {} }",
			"switch (value) { case Box<String>(var text) -> {} default -> {} }",
			"switch (value) { case Point(var x) -> {} case Point(var x, var y) -> {} default -> {} }",
			"switch (value) { case Box(Point(var x, var y)) -> {} "
				+ "case Box(Point(var a, var b)) -> {} default -> {} }",
			"Point point = null; int n = switch (point) { case Point(var x) -> x; };",
			"Box<Pair> box = null; int n = switch (box) { case Box(Pair(String a, var b)) -> 0; };",
			"Pair pair = null; int n = switch (pair) { case Pair(String x, String y) -> 1; };",
			"Box<Object> box = null; int n = switch (box) { case Box(Point(var x, var y)) -> x; };")) {
			final var source = IMPORTS + "Object value = null; " + statement;
			final var issues = validationTestHelper.validate(parser.parse(source));
			assertTrue(source + "\n" + issues, issues.stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
	}

	private void assertError(final String source, final String message) throws Exception {
		final var issues = validationTestHelper.validate(parser.parse(source));
		assertTrue(issues.toString(),
			issues.stream().anyMatch(issue -> issue.getMessage().contains(message)));
	}
}
