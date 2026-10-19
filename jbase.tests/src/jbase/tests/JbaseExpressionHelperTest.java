package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import jbase.util.JbaseExpressionHelper;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseExpressionHelperTest extends JbaseAbstractTest {

	@Inject
	private JbaseExpressionHelper helper;

	@Test
	public void testSpecialHandlingTrue() throws Exception {
		assertUnaryOperationSpecialHandling("""
			-1;
			""", true);
	}

	@Test
	public void testSpecialHandlingTrue2() throws Exception {
		assertUnaryOperationSpecialHandling("""
			-(-1);
			""", true);
	}

	@Test
	public void testSpecialHandlingFalse() throws Exception {
		assertUnaryOperationSpecialHandling("""
			-(-'a');
			""", false);
	}

	@Test
	public void testSpecialHandlingFalse2() throws Exception {
		assertUnaryOperationSpecialHandling("""
			!1;
			""", false);
	}

	@Test
	public void testSpecialHandlingFalseExplicit() throws Exception {
		assertUnaryOperationSpecialHandlingWithBaseCaseFailure("""
			-1;
			""");
	}

	@Test
	public void testBaseCase() throws Exception {
		assertUnaryOperationSpecialHandling("""
			-1;
			""", "-1");
	}

	@Test
	public void testStepCase() throws Exception {
		assertUnaryOperationSpecialHandling("""
			-(-1);
			""", "-(-1)");
	}

	@Test
	public void testStepCase2() throws Exception {
		assertUnaryOperationSpecialHandling("""
			-(+1);
			""", "-(+1)");
	}

	private void assertUnaryOperationSpecialHandling(final CharSequence input,
			final boolean expected) throws Exception {
		assertLastExpression(input, expression -> assertEquals(expected,
				helper.specialHandling((XUnaryOperation) expression)));
	}

	private void assertUnaryOperationSpecialHandling(final CharSequence input,
			final CharSequence expected) throws Exception {
		assertLastExpression(input, expression -> {
			final var buffer = new StringBuilder();
			helper.specialHandling((XUnaryOperation) expression,
					new JbaseExpressionHelper.BaseCase() {

						@Override
						public Boolean apply(final XUnaryOperation operation,
								final XNumberLiteral literal) {
							buffer.append(operation.getConcreteSyntaxFeatureName())
									.append(literal.getValue());
							return true;
						}
					}, new JbaseExpressionHelper.StepCase() {

						@Override
						public void accept(final XUnaryOperation operation) {
							buffer.insert(0, operation.getConcreteSyntaxFeatureName() + "(")
									.append(")");
						}
					});
			assertEquals(expected.toString(), buffer.toString());
		});
	}

	private void assertUnaryOperationSpecialHandlingWithBaseCaseFailure(final CharSequence input)
			throws Exception {
		assertLastExpression(input,
				expression -> assertFalse(helper.specialHandling((XUnaryOperation) expression,
						new JbaseExpressionHelper.BaseCase() {

							@Override
							public Boolean apply(final XUnaryOperation operation,
									final XNumberLiteral literal) {
								return false;
							}
						}, new JbaseExpressionHelper.StepCase() {

							@Override
							public void accept(final XUnaryOperation operation) {
							}
						})));
	}
}
