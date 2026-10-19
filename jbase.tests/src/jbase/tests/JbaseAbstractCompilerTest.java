package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import jbase.tests.inputs.JbaseTestlanguageInputs;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.TemporaryFolder;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.testing.CompilationTestHelper;
import org.junit.Rule;
import org.junit.runner.RunWith;

/**
 * For compilation tests we use JbaseTestlanguage since we test also
 * parameters.
 */
@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public abstract class JbaseAbstractCompilerTest extends JbaseAbstractTest {

	@Rule
	@Inject
	public TemporaryFolder temporaryFolder;

	@Inject
	protected CompilationTestHelper compilationTestHelper;

	@Inject
	protected JbaseTestlanguageInputs jbaseTestlanguageInputs;

	protected void checkCompilation(final CharSequence input,
			final CharSequence expectedGeneratedJava) throws Exception {
		checkCompilation(input, expectedGeneratedJava, true);
	}

	protected void checkCompilation(final CharSequence input,
			final CharSequence expectedGeneratedJava, final boolean checkValidationErrors)
			throws Exception {
		compilationTestHelper.compile(input, result -> {
			if (checkValidationErrors) {
				assertNoValidationErrors(result);
			}
			if (expectedGeneratedJava != null) {
				assertGeneratedJavaCode(result, expectedGeneratedJava);
			}
			if (checkValidationErrors) {
				assertGeneratedJavaCodeCompiles(result);
			}
		});
	}

	protected void assertNoValidationErrors(final CompilationTestHelper.Result result) {
		final var allErrors = result.getErrorsAndWarnings().stream()
				.filter(issue -> issue.getSeverity() == Severity.ERROR).toList();
		if (!allErrors.isEmpty()) {
			throw new IllegalStateException("One or more resources contained errors : "
					+ allErrors.stream().map(Object::toString).collect(Collectors.joining(",")));
		}
	}

	protected void assertGeneratedJavaCode(final CompilationTestHelper.Result r,
			final CharSequence expected) {
		assertEquals(expected.toString(), r.getSingleGeneratedCode());
	}

	protected void assertGeneratedJavaCodeCompiles(final CompilationTestHelper.Result result) {
		// check Java compilation succeeds
		result.getCompiledClass();
	}

	/**
	 * Assumes that the generated Java code has a method testMe to call
	 * and asserts what the run program prints on standard output.
	 */
	protected void assertExecuteMain(final CharSequence file, final CharSequence expectedOutput)
			throws Exception {
		final var classes = new ArrayList<Class<?>>();
		compilationTestHelper.compile(file, result -> classes.add(result.getCompiledClass()));
		final var clazz = classes.getFirst();
		final var out = new ByteArrayOutputStream();
		final var backup = System.out;
		System.setOut(new PrintStream(out));
		try {
			final var instance = clazz.getConstructor().newInstance();
			final var testMethod = Arrays.stream(clazz.getDeclaredMethods())
					.filter(candidate -> candidate.getName().equals("testMe")).findFirst()
					.orElseThrow();
			testMethod.setAccessible(true);
			// just to pass an argument
			testMethod.invoke(instance, (Object[]) null);
		} finally {
			System.setOut(backup);
		}
		assertEquals(expectedOutput.toString(), out.toString());
	}
}
