package jbase.tests;

import static org.junit.Assert.assertTrue;

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
public class JbaseArrayCompoundAssignmentTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Test
	public void generatedJavaUsesNativeAssignment() throws Exception {
		checkCompilation("""
			int[] values = {1};
			values[0] += 2;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] values = { 1 };
			    values[0] += 2;
			  }
			}
			""");
	}

	@Test
	public void arithmeticAndBitwiseOperators() throws Exception {
		final var source = """
			method testMe() {
				int[] values = {10};
				values[0] += 2;
				values[0] -= 3;
				values[0] *= 4;
				values[0] /= 2;
				values[0] %= 7;
				values[0] |= 8;
				values[0] &= 14;
				values[0] ^= 3;
				System.out.println(values[0]);
				boolean[] flags = {true};
				flags[0] &= false;
				flags[0] |= true;
				flags[0] ^= true;
				System.out.println(flags[0]);
				String[] texts = {"a"};
				texts[0] += 2;
				System.out.println(texts[0]);
				Object[] objects = {"a"};
				objects[0] += "b";
				System.out.println(objects[0]);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "15\nfalse\na2\nab\n");
	}

	@Test
	public void narrowingAndAssignmentValues() throws Exception {
		final var source = """
			method kind(byte value): String { return "byte"; }
			method kind(int value): String { return "int"; }
			method testMe() {
				byte[] bytes = {127};
				short[] shorts = {32767};
				char[] chars = {'a'};
				float[] floats = {1.0f};
				bytes[0] += 1;
				shorts[0] *= 2;
				chars[0] += 2;
				floats[0] += 0.5d;
				System.out.println(bytes[0]);
				System.out.println(shorts[0]);
				System.out.println(chars[0]);
				System.out.println(floats[0]);
				int[][] matrix = {{1}, {2}};
				int result = 10 + (matrix[0][0] += matrix[1][0] *= 3);
				System.out.println(result);
				System.out.println(matrix[0][0]);
				System.out.println(matrix[1][0]);
				byte assigned = bytes[0] += 1;
				System.out.println(assigned);
				System.out.println(kind(bytes[0] += 1));
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "-128\n-2\nc\n1.5\n17\n7\n6\n-127\nbyte\n");
	}

	@Test
	public void evaluationOrderAndSavedElementValue() throws Exception {
		final var source = """
			method receiver(int[] values): int[] { System.out.print("array "); return values; }
			method index(): int { System.out.print("index "); return 0; }
			method right(int[] values): int {
				System.out.print("right "); values[0] = 100; return 2;
			}
			method testMe() {
				int[] values = {1};
				receiver(values)[index()] += right(values);
				System.out.println(values[0]);
				System.out.println((receiver(values)[index()] += right(values)) + right(values));
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "array index right 3\narray index right right 7\n");
	}

	@Test
	public void nullAndBoundsChecksPrecedeRightOperand() throws Exception {
		final var source = """
			method index(): int { System.out.print("index "); return 1; }
			method right(): int { System.out.print("right "); return 2; }
			method testMe() {
				int[] missing = null;
				try { missing[index()] += right(); }
				catch (NullPointerException e) { System.out.println("null"); }
				int[] empty = {};
				try { empty[index()] *= right(); }
				catch (ArrayIndexOutOfBoundsException e) { System.out.println("bounds"); }
				System.out.println("done");
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "index null\nindex bounds\ndone\n");
	}

	@Test
	public void loopsLambdasAndConditionalOperands() throws Exception {
		final var source = """
			import java.util.function.IntSupplier;
			import java.util.function.IntConsumer;
			method testMe() {
				int[] values = {0};
				for (; values[0] < 2; values[0] += 1) {}
				IntSupplier update = () -> values[0] += 2;
				System.out.println(update.getAsInt());
				IntConsumer discard = n -> values[0] *= n;
				discard.accept(3);
				values[true ? 0 : 1] += values[0] > 10 ? 1 : 2;
				System.out.println(values[0]);
				boolean enabled = false;
				if (enabled && (values[0] += 100) > 0) {}
				System.out.println(values[0]);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "4\n13\n13\n");
	}

	@Test
	public void invalidOperandsAreRejected() throws Exception {
		for (final var source : new String[] {
				"int[] a = {1}; a[0] += \"text\";",
				"boolean[] a = {true}; a[0] += 1;",
				"int[] a = {1}; a[0] &= 1.5;",
				"Object[] a = {null}; a[0] *= 2;",
				"Integer[] a = {1}; a[0] += 1.5;",
				"Byte[] a = {(byte) 1}; a[0] += 1;",
				"Integer[] a = {1}; a[0] += \"text\";",
				"int[] a = {1}; a[0] += unknown;"}) {
			final var issues = validationTestHelper.validate(parser.parse(source));
			assertTrue(source, issues.stream().anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
	}

	@Test
	public void boxedElementsAndUnboxingOrder() throws Exception {
		final var source = """
			method right(): int { System.out.print("right "); return 2; }
			method testMe() {
				Integer[] values = {1};
				Integer result = values[0] += right();
				System.out.println(result);
				Boolean[] flags = {true};
				flags[0] &= false;
				System.out.println(flags[0]);
				Integer[] missing = {null};
				try { missing[0] += right(); }
				catch (NullPointerException e) { System.out.println("unbox"); }
				long[] longs = {2L};
				double[] doubles = {2.0};
				longs[0] += 1.5;
				doubles[0] *= 2;
				System.out.println(longs[0]);
				System.out.println(doubles[0]);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "right 3\nfalse\nunbox\n3\n4.0\n");
	}

	@Test
	public void conditionalSiblingsAndIndexSideEffects() throws Exception {
		final var source = """
			method right(int[] values): int {
				System.out.print("right "); values[0] = 100; return 2;
			}
			method testMe() {
				final int[] values = {1, 2};
				int index = 0;
				values[index++] += 2;
				System.out.println(index);
				System.out.println(values[0]);
				System.out.println((values[0] += 2) + (true ? right(values) : 0));
				System.out.println(false ? (values[0] += 1) : (values[1] *= 2));
				System.out.println(values[0]);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "1\n3\nright 7\n4\n100\n");
	}

	@Test
	public void forInitializersConditionsAndSwitchOperands() throws Exception {
		final var source = """
			method testMe() {
				int[] values = {0};
				for (values[0] += 1; (values[0] += 1) < 4; values[0] += 1) {}
				System.out.println(values[0]);
				values[0] += switch (values[0]) {
					case 4 -> true ? 2 : 3;
					default -> 0;
				};
				System.out.println(values[0]);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "4\n6\n");
	}
}
