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
public class JbaseShiftAssignmentTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Test
	public void leftShiftAssignment() throws Exception {
		assertShiftAssignment("<<=", "16");
	}

	@Test
	public void rightShiftAssignment() throws Exception {
		assertShiftAssignment(">>=", "4");
	}

	@Test
	public void unsignedRightShiftAssignment() throws Exception {
		assertShiftAssignment(">>>=", "4");
	}

	private void assertShiftAssignment(final String operator, final String expected) throws Exception {
		final var source = """
			method testMe() {
				int n = 8;
				n %s 1;
				System.out.println(n);
			}
			""".formatted(operator);
		checkCompilation(source, null);
		assertExecuteMain(source, expected + "\n");
	}

	@Test
	public void signedAndUnsignedShiftsAndLongDistances() throws Exception {
		final var source = """
			method testMe() {
				int signed = -8;
				int unsigned = -8;
				signed>>=1;
				unsigned>>>=1;
				long wide = -8L;
				wide>>>=1L;
				System.out.println(signed);
				System.out.println(unsigned);
				System.out.println(wide);
				int masked = 1;
				masked<<=33L;
				System.out.println(masked);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "-4\n2147483644\n9223372036854775804\n2\n");
	}

	@Test
	public void narrowingAndAssignmentValues() throws Exception {
		final var source = """
			method testMe() {
				byte small = 64;
				short medium = -8;
				char letter = 'a';
				System.out.println(small <<= 1);
				System.out.println(medium >>= 1);
				System.out.println((int) (letter >>>= 1));
				byte[] bytes = {64};
				short[] shorts = {-8};
				char[] chars = {'a'};
				System.out.println(bytes[0] <<= 1);
				System.out.println(shorts[0] >>= 1);
				System.out.println((int) (chars[0] >>>= 1));
				int n = 8;
				int distance = 1;
				System.out.println(n <<= distance >>= 1);
				System.out.println(distance);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "-128\n-4\n48\n-128\n-4\n48\n8\n0\n");
	}

	@Test
	public void arrayReceiverAndIndexAreEvaluatedOnce() throws Exception {
		for (final var operator : new String[] {"<<=", ">>=", ">>>="}) {
			final var source = """
				method receiver(int[] values): int[] { System.out.print("array "); return values; }
				method index(): int { System.out.print("index "); return 0; }
				method right(int[] values): int {
					System.out.print("right "); values[0] = 100; return 1;
				}
				method testMe() {
					int[] values = {8};
					receiver(values)[index()] %s right(values);
					System.out.println(values[0]);
				}
				""".formatted(operator);
			checkCompilation(source, null);
			assertExecuteMain(source, "array index right " + (operator.equals("<<=") ? 16 : 4) + "\n");
		}
	}

	@Test
	public void fieldReceiverIsEvaluatedOnce() throws Exception {
		final var source = """
			value : int
			method receiver() { System.out.print("receiver "); return this; }
			method testMe() {
				value = 8;
				receiver().value <<= 1;
				receiver().value >>= 1;
				receiver().value >>>= 1;
				System.out.println(value);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "receiver receiver receiver 4\n");
	}

	@Test
	public void boxedLocalsAndFieldsKeepTheirLvalues() throws Exception {
		final var source = """
			value : Integer
			small : byte
			method receiver() { System.out.print("receiver "); return this; }
			method kind(byte n): String { return "byte"; }
			method kind(int n): String { return "int"; }
			method testMe() {
				Integer n = 8;
				Long wide = -8L;
				n <<= 1L;
				wide >>>= 1;
				System.out.println(n);
				System.out.println(wide);
				value = 8;
				receiver().value >>= 1;
				System.out.println(value);
				small = 64;
				System.out.println(kind(receiver().small <<= 1));
				System.out.println(small);
				Integer[] values = {-8};
				System.out.println(values[0] >>>= 1L);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "16\n9223372036854775804\nreceiver 4\nreceiver byte\n-128\n"
			+ "2147483644\n");
	}

	@Test
	public void invalidOperandsAreRejected() throws Exception {
		for (final var operator : new String[] {"<<=", ">>=", ">>>="}) {
			for (final var source : new String[] {
					"boolean n = true; n %s 1;",
					"int n = 8; n %s 1.5;",
					"int n = 8; n %s true;",
					"double n = 8; n %s 1;",
					"int[] n = {8}; n[0] %s 1.5;",
					"final int n = 8; n %s 1;"}) {
				final var model = parser.parse(source.formatted(operator));
				assertTrue(model.eResource().getErrors().toString(),
					model.eResource().getErrors().isEmpty());
				assertTrue(source, validationTestHelper.validate(model).stream()
					.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
			}
		}
	}
}
