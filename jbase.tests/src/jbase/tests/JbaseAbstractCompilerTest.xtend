package jbase.tests

import com.google.common.base.Joiner
import com.google.inject.Inject
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider
import jbase.tests.inputs.JbaseTestlanguageInputs
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import org.eclipse.xtext.xbase.testing.CompilationTestHelper.Result
import org.eclipse.xtext.xbase.testing.TemporaryFolder
import org.junit.Rule
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

/**
 * For compilation tests we use JbaseTestlanguage since we test also
 * parameters.
 */
@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseTestlanguageInjectorProvider))
abstract class JbaseAbstractCompilerTest extends JbaseAbstractTest {

	@Rule @Inject public TemporaryFolder temporaryFolder
	@Inject protected extension CompilationTestHelper
	@Inject protected extension JbaseTestlanguageInputs

	def protected checkCompilation(CharSequence input, CharSequence expectedGeneratedJava) {
		checkCompilation(input, expectedGeneratedJava, true)
	}

	def protected checkCompilation(CharSequence input, CharSequence expectedGeneratedJava,
		boolean checkValidationErrors) {
		input.compile [
			if (checkValidationErrors) {
				assertNoValidationErrors
			}
			if (expectedGeneratedJava !== null) {
				assertGeneratedJavaCode(expectedGeneratedJava)
			}
			if (checkValidationErrors) {
				assertGeneratedJavaCodeCompiles
			}
		]
	}

	def protected assertNoValidationErrors(Result it) {
		val allErrors = getErrorsAndWarnings.filter[severity == Severity.ERROR]
		if (!allErrors.empty) {
			throw new IllegalStateException(
				"One or more resources contained errors : " + Joiner.on(',').join(allErrors)
			);
		}
	}

	def protected assertGeneratedJavaCode(CompilationTestHelper.Result r, CharSequence expected) {
		expected.toString.assertEquals(r.singleGeneratedCode)
	}

	def protected assertGeneratedJavaCodeCompiles(CompilationTestHelper.Result r) {
		r.compiledClass // check Java compilation succeeds
	}

	/**
	 * Assumes that the generated Java code has a method testMe to call
	 * and asserts what the run program prints on standard output.
	 */
	def protected assertExecuteMain(CharSequence file, CharSequence expectedOutput) {
		val classes = <Class<?>>newArrayList()
		file.compile [
			classes += compiledClass
		]
		val clazz = classes.head
		val out = new ByteArrayOutputStream()
		val backup = System.out
		System.setOut(new PrintStream(out))
		try {
			val instance = clazz.getConstructor().newInstance()
			clazz.declaredMethods.findFirst[name == 'testMe'] => [
				accessible = true
				invoke(instance, null) // just to pass an argument	
			]
		} finally {
			System.setOut(backup)
		}
		assertEquals(expectedOutput.toString, out.toString)
	}
}
