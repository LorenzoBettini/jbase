package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import java.util.function.Consumer;
import jbase.jbase.XJSemicolonStatement;
import jbase.util.JbaseNodeModelUtil;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseNodeModelUtilTest extends JbaseAbstractTest {

	@Inject
	private JbaseNodeModelUtil jbaseNodeModelUtil;

	@Test
	public void testTerminatingSemicolon() throws Exception {
		assertLastExpressionText("""
			i = 1;
			""", "i = 1;");
	}

	@Test
	public void testTerminatingSemicolon2() throws Exception {
		assertLastExpressionText("""
			i = 1;
			j = 0 ;
			""", "j = 0 ;");
	}

	@Test
	public void testAdditionalSemicolon() throws Exception {
		// additional semicolons are removed in blocks
		assertLastExpressionText("""
			{ i = 1 ;; }
			""", ";");
	}

	@Test
	public void testMissingSemicolon() throws Exception {
		assertLastExpressionText("""
			i = 1
			""", "i = 1");
	}

	@Test
	public void testMissingSemicolonDetected() throws Exception {
		assertLastExpressionTextSemicolon("""
			i = 1
			""", false);
	}

	@Test
	public void testSemicolonDetected() throws Exception {
		assertLastExpressionTextSemicolon("""
			i = 1;
			""", true);
	}

	@Test
	public void testSemicolonDetectedOnNextLine() throws Exception {
		assertLastExpressionTextSemicolon("""
			i = 1
			;
			""", true);
	}

	@Test
	public void testOffset() throws Exception {
		final var expression = parseHelper.parse("""
			i = 1
			;
			""");
		final var assignment = (XAssignment) ((XJSemicolonStatement) expression).getExpression();
		assertEquals(4, jbaseNodeModelUtil.getElementOffsetInProgram(assignment.getValue()));
	}

	private void assertLastExpressionText(final CharSequence input, final CharSequence expected)
			throws Exception {
		assertLastExpression(input, expression -> assertEquals(expected,
				jbaseNodeModelUtil.getProgramText(expression)));
	}

	private void assertLastExpressionTextSemicolon(final CharSequence input,
			final boolean expectedSemicolon) throws Exception {
		assertLastExpression(input, expression -> assertEquals(expectedSemicolon,
				jbaseNodeModelUtil.hasSemicolon(expression)));
	}

	/**
	 * Here we really want to apply the tester on a possible semicolon statement,
	 * not on the contained expression in the semicolon statement.
	 */
	@Override
	protected void applyTester(final XExpression expression,
			final Consumer<? super XExpression> tester) {
		if (expression instanceof XBlockExpression block) {
			final var expressions = block.getExpressions();
			applyTester(expressions.isEmpty() ? null : expressions.getLast(), tester);
		} else {
			tester.accept(expression);
		}
	}
}
