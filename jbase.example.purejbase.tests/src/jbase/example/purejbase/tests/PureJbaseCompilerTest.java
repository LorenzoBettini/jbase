package jbase.example.purejbase.tests;

import com.google.inject.Inject;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.TemporaryFolder;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.testing.CompilationTestHelper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(PureJbaseInjectorProvider.class)
public class PureJbaseCompilerTest {

	@Rule
	@Inject
	public TemporaryFolder temporaryFolder;

	@Inject
	private CompilationTestHelper compilationTestHelper;

	@Test
	public void testHelloWorld() throws Exception {
		checkCompilation("""
			System.out.println("Hello World!");
			""", """
			package purejbase;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    System.out.println("Hello World!");
			  }
			}
			""");
	}

	@Test
	public void testLiterals() throws Exception {
		checkCompilation("""
			Object o = null;

			// String Literal
			String s = "Hello World";

			// Number Literals are mostly like in Java
			// (consult the documentation for more details)
			int i = 42;
			double d = 0.42e2;

			// Boolean Literal
			boolean b1 = true;
			boolean b2 = false;

			// Into the bargain, there are number literals for\s
			// java.math.BigInteger and java.math.BigDecimal
			java.math.BigInteger bi = 0xbeef_beef_beef_beef_beef#BI;
			java.math.BigDecimal bd = 0.123_456_789_123_456_789_123_456_789_123_456_789e4242BD;
			""", """
			package purejbase;

			import java.math.BigDecimal;
			import java.math.BigInteger;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Object o = null;
			    String s = "Hello World";
			    int i = 42;
			    double d = 0.42e2;
			    boolean b1 = true;
			    boolean b2 = false;
			    BigInteger bi = new BigInteger("beefbeefbeefbeefbeef", 16);
			    BigDecimal bd = new BigDecimal("0.123456789123456789123456789123456789e4242");
			  }
			}
			""");
	}

	private void checkCompilation(final CharSequence input,
			final CharSequence expectedGeneratedJava) throws Exception {
		checkCompilation(input, expectedGeneratedJava, true);
	}

	private void checkCompilation(final CharSequence input,
			final CharSequence expectedGeneratedJava, final boolean checkValidationErrors)
			throws Exception {
		compilationTestHelper.compile(input, result -> {
			if (checkValidationErrors) {
				assertNoValidationErrors(result);
			}
			if (expectedGeneratedJava != null) {
				assertGeneratedJavaCode(result, expectedGeneratedJava);
			}
			assertGeneratedJavaCodeCompiles(result);
		});
	}

	private void assertNoValidationErrors(final CompilationTestHelper.Result result) {
		final var allErrors = result.getErrorsAndWarnings().stream()
				.filter(issue -> issue.getSeverity() == Severity.ERROR).toList();
		if (!allErrors.isEmpty()) {
			throw new IllegalStateException("One or more resources contained errors : "
					+ allErrors.stream().map(Object::toString)
							.collect(java.util.stream.Collectors.joining(",")));
		}
	}

	private void assertGeneratedJavaCode(final CompilationTestHelper.Result result,
			final CharSequence expected) {
		Assert.assertEquals(expected.toString(), result.getSingleGeneratedCode());
	}

	private void assertGeneratedJavaCodeCompiles(final CompilationTestHelper.Result result) {
		// check Java compilation succeeds
		result.getCompiledClass();
	}
}
