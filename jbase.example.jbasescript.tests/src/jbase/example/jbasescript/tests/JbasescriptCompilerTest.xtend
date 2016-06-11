package jbase.example.jbasescript.tests

import com.google.common.base.Joiner
import com.google.inject.Inject
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.TemporaryFolder
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.xbase.compiler.CompilationTestHelper
import org.eclipse.xtext.xbase.compiler.CompilationTestHelper.Result
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbasescriptInjectorProvider)) 
class JbasescriptCompilerTest {

	@Rule @Inject public TemporaryFolder temporaryFolder

	@Inject extension CompilationTestHelper

	@Test
	def void testHelloWorld() {
		'''
System.out.println("Hello World!");
		'''.checkCompilation('''
package jbasescript;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    System.out.println("Hello World!");
  }
}
		''')
	}

	@Test
	def void testLiterals() {
		'''
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

// Into the bargain, there are number literals for 
// java.math.BigInteger and java.math.BigDecimal
java.math.BigInteger bi = 0xbeef_beef_beef_beef_beef#BI;
java.math.BigDecimal bd = 0.123_456_789_123_456_789_123_456_789_123_456_789e4242BD;
		'''.checkCompilation('''
package jbasescript;

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
		''')
	}

	@Test
	def void testOperations() {
		'''
op doPrint(String s) {
	System.out.println(s);
}

op print(String s) {
	doPrint(s);
}

String s = "Hello World";
print(s);
		'''.checkCompilation('''
package jbasescript;

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
		''')
	}

	@Test
	def void testNonFinalParam() {
		'''
op doAppend(String s) : void {
	s += "";
}

String s = "Hello World";
doAppend(s);
		'''.checkCompilation('''
package jbasescript;

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
		''')
	}

	@Test def void testThrow() {
		'''
		op m(String s) : void {
			throw new Exception("test");
		}
		'''.checkCompilation(
'''
package jbasescript;

import org.eclipse.xtext.xbase.lib.Exceptions;

@SuppressWarnings("all")
public class MyFile {
  public static void m(String s) {
    try {
      throw new Exception("test");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public static void main(String[] args) throws Throwable {
  }
}
'''
		)
	}

	def private checkCompilation(CharSequence input, CharSequence expectedGeneratedJava) {
		checkCompilation(input, expectedGeneratedJava, true)
	}

	def private checkCompilation(CharSequence input, CharSequence expectedGeneratedJava, boolean checkValidationErrors) {
		input.compile[
			if (checkValidationErrors) {
				assertNoValidationErrors
			}
			
			if (expectedGeneratedJava != null) {
				assertGeneratedJavaCode(expectedGeneratedJava)
			}
			assertGeneratedJavaCodeCompiles
		]
	}
	
	private def assertNoValidationErrors(Result it) {
		val allErrors = getErrorsAndWarnings.filter[severity == Severity.ERROR]
		if (!allErrors.empty) {
			throw new IllegalStateException("One or more resources contained errors : "+
				Joiner.on(',').join(allErrors)
			);
		}
	}

	def private assertGeneratedJavaCode(CompilationTestHelper.Result r, CharSequence expected) {
		expected.toString.assertEquals(r.singleGeneratedCode)
	}

	def private assertGeneratedJavaCodeCompiles(CompilationTestHelper.Result r) {
		r.compiledClass // check Java compilation succeeds
	}
}