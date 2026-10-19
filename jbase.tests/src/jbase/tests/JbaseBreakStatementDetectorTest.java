package jbase.tests;

import static org.junit.Assert.*;
import jbase.controlflow.JbaseBreakStatementDetector;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseBreakStatementDetectorTest extends JbaseAbstractTest {

	public static class JbaseBreakStatementDetectorCustom extends JbaseBreakStatementDetector {

		// make it public to test it
		@Override
		public boolean possibleBreak(final XExpression e) {
			return super.possibleBreak(e);
		}

		@Override
		public boolean containsPossibleBreak(final XExpression e) {
			return super.containsPossibleBreak(e);
		}
	}

	private final JbaseBreakStatementDetectorCustom detector = new JbaseBreakStatementDetectorCustom();

	@Test(expected = IllegalArgumentException.class)
	public void testNullInDispatchMethod() throws Exception {
		detector.containsPossibleBreak(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullInDispatchMethod2() throws Exception {
		detector.possibleBreak(null);
	}

	@Test
	public void testNull() throws Exception {
		assertFalse(detector.containsPossibleBreakStatement(null));
	}

	@Test
	public void testNull2() throws Exception {
		assertFalse(detector.isPossibleBreakStatement(null));
	}

	@Test
	public void testAnyStatement() throws Exception {
		assertIsPossibleBreakStatement("int i = 0;", false);
	}

	@Test
	public void testAnyStatement2() throws Exception {
		assertContainsBreakStatement("int i = 0;", false);
	}

	@Test
	public void testContinue() throws Exception {
		assertIsPossibleBreakStatement("continue;", false);
	}

	@Test
	public void testBreak() throws Exception {
		assertIsPossibleBreakStatement("break;", true);
	}

	@Test
	public void testBreakInSingleThenBranch() throws Exception {
		assertIsPossibleBreakStatement("""
			if (true)
				break;
			""", true);
	}

	@Test
	public void testBreakInSingleElseBranch() throws Exception {
		assertIsPossibleBreakStatement("""
			if (true) ;
			else
				break;
			""", true);
	}

	@Test
	public void testBreakInThenBranch() throws Exception {
		assertIsPossibleBreakStatement("""
			if (true)
				break;
			else {
			\t
			}
			""", true);
	}

	@Test
	public void testBreakInElseBranch() throws Exception {
		assertIsPossibleBreakStatement("""
			if (true) {
			\t
			} else {
				break;
			}
			""", true);
	}

	@Test
	public void testBreakInBothBranches() throws Exception {
		assertIsPossibleBreakStatement("""
			if (true)
				break;
			else
				break;
			""", true);
	}

	@Test
	public void testBreakInBothBranchesWithBlocks() throws Exception {
		assertIsPossibleBreakStatement("""
			if (true) {
				break;
			} else {
				break;
			}
			""", true);
	}

	@Test
	public void testBreakAndContinueInBranchesWithBlocks() throws Exception {
		assertIsPossibleBreakStatement("""
			if (true) {
				break;
			} else {
				continue;
			}
			""", true);
	}

	@Test
	public void testBreakInWhile() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				break;
			}
			""", true);
	}

	@Test
	public void testBreakInNestedWhileIgnore() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				while (true)
					break;
			}
			""", false);
	}

	@Test
	public void testBreakInWhileInThenBranch() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				if (true)
					break;
			}
			""", true);
	}

	@Test
	public void testBreakInWhileInElseBranch() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				if (true);
				else
					break;
			}
			""", true);
	}

	@Test
	public void testBreakInWhileNoBreakInIf() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				if (true);
			}
			""", false);
	}

	@Test
	public void testBreakInWhileBothBranchesOfIf() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				if (true)
					break;
				else
					break;
			}
			""", true);
	}

	@Test
	public void testBreakInWhileNotBothBranchesOfIf() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				if (true)
					break;
				else
					System.out.println("");
			}
			""", true);
	}

	@Test
	public void testBreakInWhileNotBothBranchesOfIf2() throws Exception {
		assertContainsBreakStatement("""
			while (true) {
				if (true)
					break;
				else
					continue;
			}
			""", true);
	}

	@Test
	public void testBreakInDoWhile() throws Exception {
		assertContainsBreakStatement("""
			do {
				break;
			} while (true);
			""", true);
	}

	@Test
	public void testBreakInBasicForLoop() throws Exception {
		assertContainsBreakStatement("""
			for (;;) {
				break;
			}
			""", true);
	}

	private void assertIsPossibleBreakStatement(final CharSequence input, final boolean expected)
			throws Exception {
		final var expression = parseHelper.parse(input);
		assertEquals(expression.toString(), expected,
				detector.isPossibleBreakStatement(expression));
	}

	private void assertContainsBreakStatement(final CharSequence input, final boolean expected)
			throws Exception {
		final var expression = parseHelper.parse(input);
		assertEquals(expression.toString(), expected,
				detector.containsPossibleBreakStatement(expression));
	}
}
