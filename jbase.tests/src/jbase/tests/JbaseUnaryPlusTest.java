package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.inject.Inject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.junit.Test;

public class JbaseUnaryPlusTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private IBatchTypeResolver typeResolver;

	@Test
	public void unaryPlusOnIntVariable() throws Exception {
		checkCompilation("""
			int n = 1;
			int positive = +n;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int n = 1;
			    int _plus = (+n);
			    int positive = _plus;
			  }
			}
			""");
	}

	@Test
	public void numericPromotionAndUnboxing() throws Exception {
		final String[][] cases = {
			{"byte", "1", "int"}, {"short", "1", "int"}, {"char", "'a'", "int"},
			{"int", "1", "int"}, {"long", "1L", "long"},
			{"float", "1.5f", "float"}, {"double", "1.5", "double"},
			{"Byte", "Byte.valueOf((byte) 1)", "int"},
			{"Short", "Short.valueOf((short) 1)", "int"},
			{"Character", "Character.valueOf('a')", "int"},
			{"Integer", "Integer.valueOf(1)", "int"},
			{"Long", "Long.valueOf(1L)", "long"},
			{"Float", "Float.valueOf(1.5f)", "float"},
			{"Double", "Double.valueOf(1.5)", "double"}
		};
		for (final var entry : cases) {
			final var input = entry[0] + " n = " + entry[1] + "; var positive = +n;";
			final var model = parser.parse(input);
			validationTestHelper.assertNoErrors(model);
			final var operation = EcoreUtil2.getAllContentsOfType(model, XUnaryOperation.class)
				.getFirst();
			assertEquals(entry[0], entry[2], typeResolver.resolveTypes(model)
				.getActualType(operation).getIdentifier());
			checkCompilation(input, null);
		}
	}

	@Test
	public void promotedVariablesCannotBeNarrowed() throws Exception {
		for (final var type : new String[] {"byte", "short", "char"}) {
			validationTestHelper.assertError(parser.parse(type + " n = 1; "
				+ type + " positive = +n;"), XbasePackage.Literals.XUNARY_OPERATION,
				IssueCodes.INCOMPATIBLE_TYPES, "int", type);
		}
	}

	@Test
	public void nonNumericOperandsAreRejected() throws Exception {
		for (final var operand : new String[] {"true", "\"text\"", "new Object()"}) {
			final var errors = validationTestHelper.validate(parser.parse("var n = " + operand
				+ "; var positive = +n;")).stream()
				.filter(issue -> issue.getSeverity() == Severity.ERROR).toList();
			assertFalse(operand, errors.isEmpty());
		}
	}

	@Test
	public void literalsRetainConstantNarrowing() throws Exception {
		checkCompilation("""
			byte b = +1;
			short s = +1;
			char c = +1;
			short nested = +(-(+1));
			""", null);
	}

	@Test
	public void nestedExpressionsAndSideEffects() throws Exception {
		final var input = """
			method next(): int { System.out.println("next"); return 3; }
			method kind(int n): String { return "int"; }
			method kind(long n): String { return "long"; }
			method testMe() {
				int n = 2;
				System.out.println(+n + (-n) * +(+n));
				System.out.println(+(n + 1));
				System.out.println(+next());
				int[] values = {7, 8};
				int index = 0;
				System.out.println(+values[index++]);
				System.out.println(index);
				byte small = 1;
				System.out.println(kind(+small));
				Long boxed = 4L;
				System.out.println(kind(+boxed));
				double negativeZero = -0.0;
				System.out.println(1.0 / +negativeZero);
			}
			""";
		checkCompilation(input, null);
		assertExecuteMain(input, "-2\n3\nnext\n3\n7\n1\nint\nlong\n-Infinity\n");
	}
}
