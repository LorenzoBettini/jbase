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
public class JbaseParenthesizedAssignmentTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Test
	public void assignsParenthesizedVariableAsStatementAndExpression() throws Exception {
		final var source = """
			method testMe() {
				int n = 0;
				(n) = 1;
				int assigned = (n) = 2;
				System.out.println(n);
				System.out.println(assigned);
			}
			""";
		final var parsed = parser.parse(source);
		assertTrue(parsed.eResource().getErrors().toString(),
			parsed.eResource().getErrors().isEmpty());
		checkCompilation(source, null);
		assertExecuteMain(source, "2\n2\n");
	}

	@Test
	public void rejectsIncompatibleValue() throws Exception {
		final var parsed = parser.parse("method testMe() { int n = 0; (n) = \"wrong\"; }");
		assertTrue(parsed.eResource().getErrors().toString(),
			parsed.eResource().getErrors().isEmpty());
		assertTrue(validationTestHelper.validate(parsed).toString(),
			validationTestHelper.validate(parsed).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
	}
}
