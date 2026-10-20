package jbase.tests;

import static org.junit.Assert.*;
import jbase.controlflow.JbaseBranchingStatementDetector;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseBranchingStatementDetectorTest extends JbaseAbstractTest {

	public static class JavammBranchingStatementDetectorCustom
			extends JbaseBranchingStatementDetector {

		// make it public to test it
		@Override
		public boolean sureBranch(final XExpression e) {
			return super.sureBranch(e);
		}
	}

	private final JavammBranchingStatementDetectorCustom detector = new JavammBranchingStatementDetectorCustom();

	@Test(expected = IllegalArgumentException.class)
	public void testNullInDispatchMethod() {
		detector.sureBranch(null);
	}

	@Test
	public void testNull() {
		assertFalse(detector.isSureBranchStatement(null));
	}

	@Test
	public void testAnyStatement() throws Exception {
		assertIsSureBranchStatement("int i = 0;", false);
	}

	@Test
	public void testContinue() throws Exception {
		assertIsSureBranchStatement("continue;", true);
	}

	@Test
	public void testBreak() throws Exception {
		assertIsSureBranchStatement("break;", true);
	}

	@Test
	public void testContinueInSingleThenBranch() throws Exception {
		assertIsSureBranchStatement("""
			if (true)
				continue;
			""", true);
	}

	@Test
	public void testContinueInSingleElseBranch() throws Exception {
		assertIsSureBranchStatement("""
			if (true) ;
			else
				continue;
			""", false);
	}

	@Test
	public void testContinueInThenBranch() throws Exception {
		assertIsSureBranchStatement("""
			if (true)
				continue;
			else {
			\t
			}
			""", false);
	}

	@Test
	public void testContinueInElseBranch() throws Exception {
		assertIsSureBranchStatement("""
			if (true) {
			\t
			} else {
				continue;
			}
			""", false);
	}

	@Test
	public void testContinueInBothBranches() throws Exception {
		assertIsSureBranchStatement("""
			if (true)
				continue;
			else
				continue;
			""", true);
	}

	@Test
	public void testContinueInBothBranchesWithBlocks() throws Exception {
		assertIsSureBranchStatement("""
			if (true) {
				continue;
			} else {
				continue;
			}
			""", true);
	}

	private void assertIsSureBranchStatement(final CharSequence input, final boolean expected)
			throws Exception {
		final var expression = parseHelper.parse(input);
		assertEquals(expression.toString(), expected, detector.isSureBranchStatement(expression));
	}
}
