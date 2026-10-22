package jbase.tests;

import static org.junit.Assert.assertTrue;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;

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

	@Inject
	private ParseHelper<EObject> parser;

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
}
