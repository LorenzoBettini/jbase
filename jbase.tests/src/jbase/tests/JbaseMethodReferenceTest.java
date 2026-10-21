package jbase.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseMethodReferenceTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Test
	public void referenceFormsAndTargetContexts() throws Exception {
		checkCompilation("""
			import java.util.*;
			import java.util.function.*;
			import java.util.stream.*;
			method length(): ToIntFunction<String> { return String::length; }
			ToIntFunction<String> length = String::length;
			Function<String, Integer> parse = Integer::parseInt;
			Consumer<String> print = System.out::println;
			Supplier<String> bound = "hello"::trim;
			Supplier<ArrayList<String>> create = ArrayList<String>::new;
			IntFunction<ArrayList<String>> capacity = ArrayList<String>::new;
			IntFunction<int[]> array = int[]::new;
			IntFunction<String[][]> matrix = String[][]::new;
			Function<List<String>, String> first = List<String>::getFirst;
			ToIntFunction<List<String>> size = List::size;
			Function<String, List<String>> singleton = Collections::<String>singletonList;
			BiFunction<String, String, List<String>> varargs = Arrays::asList;
			Stream.of("a", "bb").map(String::length).forEach(System.out::println);
			ToIntFunction<String> assigned;
			assigned = String::length;
			var cast = (ToIntFunction<String>) String::length;
			ToIntFunction<String> conditional = true ? String::length : String::length;
			Consumer<String> discard = String::trim;
			""", null);
	}

	@Test
	public void generatedJavaPreservesReference() throws Exception {
		checkCompilation("""
			java.util.function.ToIntFunction<String> length = String::length;
			""", """
			package jbasetestlanguage;

			import java.util.function.ToIntFunction;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    ToIntFunction<String> length = String::length;
			  }
			}
			""");
	}

	@Test
	public void executeReferences() throws Exception {
		assertExecuteMain("""
			import java.util.*;
			import java.util.function.*;
			method testMe() {
				ToIntFunction<String> length = String::length;
				Function<String, Integer> parse = Integer::parseInt;
				Supplier<ArrayList<String>> create = ArrayList<String>::new;
				IntFunction<int[]> array = int[]::new;
				System.out.println(length.applyAsInt("hello"));
				System.out.println(parse.apply("42"));
				System.out.println(create.get().size());
				System.out.println(array.apply(3).length);
			}
			""", "5\n42\n0\n3\n");
	}

	@Test
	public void genericReferencesAndExceptions() throws Exception {
		checkCompilation("""
			import java.util.*;
			import java.util.concurrent.Callable;
			import java.util.function.*;
			Supplier<List<String>> empty = Collections::emptyList;
			Function<String, String> identity = Objects::requireNonNull;
			Function<String, List<String>> singleton = Collections::singletonList;
			Function<List<String>, String> first = List::getFirst;
			Supplier<List<String>> create = ArrayList::new;
			Function<String[], List<String>> fixedVarargs = Arrays::asList;
			Supplier<List<String>> emptyVarargs = Arrays::asList;
			Callable<Object> read = new java.io.ObjectInputStream(
				new java.io.ByteArrayInputStream(new byte[] {}))::readObject;
			Supplier<Integer> unchecked = new java.util.Random()::nextInt;
			ToIntFunction<String> explicitIgnored = String::<Integer>length;
			IntFunction<List<?>[]> reifiable = List<?>[]::new;
			Function<String[], String[]> clone = String[]::clone;
			Supplier<String[]> boundClone = new String[] {"hello"}::clone;
			Consumer raw = System.out::println;
			Consumer<? super String> wildcard = System.out::println;
			LongConsumer boxedLong = new java.util.concurrent.atomic.AtomicReference<Long>()::set;
			Function<String, StringBuilder> builder = StringBuilder::new;
			Function<Object, String> objectMethod = Object::toString;
			Function<String, Class<? extends String>> stringClass = String::getClass;
			Supplier<Class<? extends String>> boundClass = "hello"::getClass;
			Function<String[], Class<? extends String[]>> arrayType = String[]::getClass;
			BiFunction<String, Integer, Map<String, Integer>> map =
				Collections::<String, Integer>singletonMap;
			Function<jbase.tests.util.MethodReferenceFixtures.Box<String>.Value, String> inner =
				jbase.tests.util.MethodReferenceFixtures.Box<String>.Value::get;
			var comparator = Comparator.comparing(String::length);
			comparator.compare("a", "bb");
			Function<String[], Class<?>> arrayClass = String[]::getClass;
			Supplier<String> arrayText = new String[] {}::toString;
			""", null);
	}

	@Test
	public void boundReceiverEvaluation() throws Exception {
		final var source = """
			import java.util.function.*;
			method receiver(int[] calls): String { calls[0]++; return " hello "; }
			method local(): java.util.function.IntSupplier { return this::hashCode; }
			method testMe() {
				int[] calls = {0};
				Supplier<String> once = receiver(calls)::trim;
				System.out.println(calls[0]);
				System.out.println(once.get());
				System.out.println(once.get());
				System.out.println(calls[0]);
				String text = "first";
				Supplier<String> captured = text::trim;
				text = "second";
				System.out.println(captured.get());
				String absent = null;
				try {
					Supplier<String> broken = absent::trim;
					System.out.println("late null check");
				} catch (NullPointerException e) {
					System.out.println("null at creation");
				}
				Supplier<String> current = this::toString;
				Supplier<String> parent = super::toString;
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "1\nhello\nhello\n1\nfirst\nnull at creation\n");
	}

	@Test
	public void boundReferenceKeepsArgumentEvaluationOrder() throws Exception {
		final var source = """
			import java.util.function.Supplier;
			method mutate(String[] values): int { values[0] = "second"; return 1; }
			method consume(Supplier<String> supplier, int ignored): String {
				return supplier.get();
			}
			method testMe() {
				String[] values = {"first"};
				System.out.println(consume(values[0]::trim, mutate(values)));
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "first\n");
	}

	@Test
	public void overloadInvocationPhasesAndVariableArity() throws Exception {
		final var source = """
			import jbase.tests.util.MethodReferenceFixtures;
			import java.util.function.*;
			method testMe() {
				Function<Integer, String> boxed = MethodReferenceFixtures::choose;
				IntFunction<String> primitive = MethodReferenceFixtures::choose;
				Supplier<String> empty = MethodReferenceFixtures::spread;
				Function<String, String> single = MethodReferenceFixtures::spread;
				Runnable unchecked = MethodReferenceFixtures::unchecked;
				Function<String, Object> pick = MethodReferenceFixtures::pick;
				IntFunction<Class<?>> classOf = MethodReferenceFixtures::getClass;
				Function<Object, String> prefixed = MethodReferenceFixtures::prefixed;
				var applied = MethodReferenceFixtures.apply(String::length);
				jbase.tests.util.LambdaFixtures.GenericOperation generic =
					MethodReferenceFixtures::identity;
				System.out.println(boxed.apply(1));
				System.out.println(primitive.apply(1));
				System.out.println(empty.get());
				System.out.println(single.apply("a"));
				System.out.println(prefixed.apply("a"));
				System.out.println(pick.apply("x"));
				System.out.println(applied);
				System.out.println(generic.apply("generic"));
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "Object\nint\nString\nString\nString\nString\n2\ngeneric\n");
	}

	@Test
	public void resolvedExecutableUsesJavaOverloadSelection() throws Exception {
		final var model = parser.parse("""
			import jbase.tests.util.MethodReferenceFixtures;
			import java.util.function.*;
			Function<Integer, String> boxed = MethodReferenceFixtures::choose;
			IntFunction<String> primitive = MethodReferenceFixtures::choose;
			Supplier<String> empty = MethodReferenceFixtures::spread;
			""");
		validationTestHelper.assertNoErrors(model);
		final var references = org.eclipse.xtext.EcoreUtil2.getAllContentsOfType(model,
			jbase.jbase.XJMethodReference.class);
		assertEquals(java.util.List.of("java.lang.Object", "int", "java.lang.String[]"),
			references.stream().map(reference -> reference.getExecutable().getParameters()
				.getFirst().getParameterType().getIdentifier()).toList());
	}

	@Test
	public void incompleteReferencesDoNotCrashValidation() throws Exception {
		for (final var source : new String[] {
			"Runnable f = ()::run;", "Runnable f = ()::new;", "Runnable f = missing::run;",
			"Runnable f = String::;", "Runnable f = String::<String>",
			"Runnable f = List<String>::", "Runnable f = int[]::"
		}) {
			assertTrue(source, validationTestHelper.validate(parser.parse(source)).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
	}

	@Test
	public void invalidReferences() throws Exception {
		for (final var source : new String[] {
			"var f = String::length;",
			"Object f = String::length;",
			"Runnable f = String::length;",
			"java.util.function.ToIntFunction<Integer> f = String::length;",
			"java.util.function.Supplier<Integer> f = \"x\"::trim;",
			"Runnable f = System::out;",
			"Runnable f = String::missing;",
			"java.util.function.Function<String, String> f = jbase.tests.util.MethodReferenceFixtures::ambiguousVarargs;",
			"java.util.function.Function<String, Class<? extends String>> f = Object::getClass;",
			"java.util.function.Function<String, Long> f = String::length;",
			"java.util.function.IntConsumer f = new java.util.concurrent.atomic.AtomicReference<Long>()::set;",
			"int n = 1; java.util.function.IntSupplier f = n::intValue;",
			"java.util.function.IntUnaryOperator f = Integer::intValue;",
			"java.util.function.Supplier<Deprecated> f = Deprecated::new;",
			"String s = \"\"; java.util.function.Supplier<String> f = s::new;",
			"java.util.function.Supplier<String[]> f = String[]::clone;",
			"java.util.function.Function<Object, String[]> f = String[]::clone;",
			"java.util.function.Function<Integer, String[]> f = new String[] {}::clone;",
			"java.util.function.Supplier<String> f = int::toString;",
			"java.util.function.Supplier<String> f = String::format;",
			"java.io.OutputStream f = System.out::write;",
			"java.util.function.Function<String, String> f = jbase.tests.util.MethodReferenceFixtures::bounded;",
			"java.util.function.Function<String, String> f = jbase.tests.util.MethodReferenceFixtures::ambiguous;",
			"java.util.function.IntFunction<java.util.List<? super String>[]> f = java.util.List<? super String>[]::new;",
			"java.util.function.IntFunction<java.util.List<? extends Number>[]> f = java.util.List<? extends Number>[]::new;",
			"int[] a = {}; java.util.function.IntFunction<int[]> f = a::new;",
			"java.util.function.IntFunction<int[]> f = int[]::<Integer>new;",
			"java.util.function.Supplier<java.util.List<String>> f = java.util.List<String>::of;",
			"java.util.function.Supplier<java.util.ArrayList<?>> f = java.util.ArrayList<?> ::new;",
			"java.util.function.Function<Integer, String> f = Integer::toString;",
			"java.util.function.Supplier<Number> f = Number::new;",
			"java.util.function.Supplier<String> f = \"x\"::valueOf;",
			"java.util.function.Supplier<Runnable> f = Runnable::new;",
			"java.util.function.Supplier<int[]> f = int[]::new;",
			"java.util.function.Supplier<String> f = String::intern;",
			"java.util.function.IntFunction<java.util.List<String>[]> f = java.util.List<String>[]::new;",
			"java.util.function.Consumer<java.util.List<Object>> f = java.util.Collections::<Object>sort;",
			"java.util.function.Function<String, String> f = java.util.Collections::<String, Integer>singletonList;",
			"java.util.function.ToIntFunction<String> f = String::coder;",
			"java.util.function.Function<Long, int[]> f = int[]::new;",
			"java.util.function.Function<String, Class<?>> f = Class::forName;"
		}) {
			final var model = parser.parse(source);
			assertTrue(source, validationTestHelper.validate(model).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR
					&& jbase.validation.JbaseIssueCodes.INVALID_METHOD_REFERENCE.equals(issue.getCode())));
		}
	}
}
