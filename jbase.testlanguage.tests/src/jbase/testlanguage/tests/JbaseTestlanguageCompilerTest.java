package jbase.testlanguage.tests;

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
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseTestlanguageCompilerTest {

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
			package jbasetestlanguage;

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
			package jbasetestlanguage;

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

	@Test
	public void testMethods() throws Exception {
		checkCompilation("""
			method doPrint(String s) {
				System.out.println(s);
			}

			method print(String s) {
				doPrint(s);
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public void doPrint(String s) {
			    System.out.println(s);
			  }

			  public void print(String s) {
			    this.doPrint(s);
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testMethodsAndFields() throws Exception {
		checkCompilation("""
			aString : String

			method print() {
				System.out.println(aString);
			}

			method change() {
				return aString = "a";
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  private String aString;

			  public void print() {
			    System.out.println(this.aString);
			  }

			  public String change() {
			    return this.aString = "a";
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testProperties() throws Exception {
		checkCompilation("""
			import java.util.List;

			aString : String
			aList : List<String>
			""", """
			package jbasetestlanguage;

			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  private String aString;

			  private List<String> aList;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testOperations() throws Exception {
		checkCompilation("""
			op doPrint(String s) {
				System.out.println(s);
			}

			op print(String s) {
				doPrint(s);
			}

			String s = "Hello World";
			print(s);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void doPrint(String s) {
			    System.out.println(s);
			  }

			  public static void print(String s) {
			    MyFile.doPrint(s);
			  }

			  public static void main(String[] args) throws Throwable {
			    String s = "Hello World";
			    MyFile.print(s);
			  }
			}
			""");
	}

	@Test
	public void testNonFinalParam() throws Exception {
		checkCompilation("""
			op doAppend(String s) : void {
				s += "";
			}

			String s = "Hello World";
			doAppend(s);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void doAppend(String s) {
			    String _s = s;
			    s = (_s + "");
			  }

			  public static void main(String[] args) throws Throwable {
			    String s = "Hello World";
			    MyFile.doAppend(s);
			  }
			}
			""");
	}

	@Test
	public void testThrow() throws Exception {
		checkCompilation("""
			method m(String s) : void {
				throw new Exception("test");
			}
			""", """
			package jbasetestlanguage;

			import org.eclipse.xtext.xbase.lib.Exceptions;

			@SuppressWarnings("all")
			public class MyFile {
			  public void m(String s) {
			    try {
			      throw new Exception("test");
			    } catch (Throwable _e) {
			      throw Exceptions.sneakyThrow(_e);
			    }
			  }

			  public static void main(String[] args) throws Throwable {
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
		Assert.assertEquals(expected.toString().replace("\r", ""),
				result.getSingleGeneratedCode().replace("\r", ""));
	}

	private void assertGeneratedJavaCodeCompiles(final CompilationTestHelper.Result result) {
		// check Java compilation succeeds
		result.getCompiledClass();
	}
}
