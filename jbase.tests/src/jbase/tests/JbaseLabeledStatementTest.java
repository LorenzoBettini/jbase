package jbase.tests;

import static org.junit.Assert.assertEquals;

import jbase.jbase.XJBreakStatement;
import jbase.jbase.XJLabeledStatement;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseLabeledStatementTest extends JbaseAbstractTest {

	@Test
	public void parsesLabelAndBreakTarget() throws Exception {
		final var labeled = (XJLabeledStatement) parseHelper.parse("""
			exit: {
				break exit;
			}
			""");
		assertEquals("exit", labeled.getName());
		final var block = (XBlockExpression) labeled.getStatement();
		final var branch = (XJBreakStatement)
			((jbase.jbase.XJSemicolonStatement) block.getExpressions().getFirst()).getExpression();
		assertEquals("exit", branch.getLabel());
		validationTestHelper.assertNoIssues(labeled);
	}

	@Test
	public void validatesLabelledContinueToLoop() throws Exception {
		validationTestHelper.assertNoIssues(parseHelper.parse("""
			outer: while (true) continue outer;
			"""));
	}

	@Test
	public void validatesLabelledContinueToEnhancedForLoop() throws Exception {
		validationTestHelper.assertNoIssues(parseHelper.parse("""
			outer: for (var value : new int[] {1}) continue outer;
			"""));
	}
}
