package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import java.util.function.Consumer;
import jbase.jbase.XJArrayAccessExpression;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJAssignment;
import jbase.jbase.XJClassObject;
import jbase.jbase.XJConditionalExpression;
import jbase.jbase.XJConstructorCall;
import jbase.jbase.XJSemicolonStatement;
import jbase.jbase.XJTryWithResourcesStatement;
import jbase.jbase.XJVariableDeclaration;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import jbase.tests.inputs.JbaseInputs;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XCastedExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XIfExpression;
import org.eclipse.xtext.xbase.XInstanceOfExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.XThrowExpression;
import org.eclipse.xtext.xbase.XUnaryOperation;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XWhileExpression;

public abstract class JbaseAbstractTest {

	@Inject
	protected ParseHelper<XExpression> parseHelper;

	@Inject
	protected ValidationTestHelper validationTestHelper;

	@Inject
	protected JbaseInputs jbaseInputs;

	/**
	 * For some tests we need to use JbaseTestlanguage's operations and imports
	 */
	protected void useJbaseTestlanguage() {
		new JbaseTestlanguageInjectorProvider().getInjector().injectMembers(this);
	}

	protected void parseAndAssertNoErrors(final CharSequence input) throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	protected void parseAndAssertNoIssues(final CharSequence input) throws Exception {
		validationTestHelper.assertNoIssues(parseHelper.parse(input));
	}

	protected void assertEqualsStrings(final CharSequence expected, final CharSequence actual) {
		assertEquals(expected.toString().replaceAll("\r", ""), actual.toString());
	}

	protected void assertLastExpression(final CharSequence input,
			final Consumer<? super XExpression> tester) throws Exception {
		// if there are several lines we transform it into a block
		var toParse = input.toString().trim();
		if (toParse.contains("\n")) {
			toParse = "{\n" + toParse.indent(1) + "}\n";
		}
		applyTester(parseHelper.parse(toParse), tester);
	}

	protected void applyTester(final XExpression expression,
			final Consumer<? super XExpression> tester) {
		if (expression instanceof XBlockExpression block) {
			final var expressions = block.getExpressions();
			applyTester(expressions.isEmpty() ? null : expressions.getLast(), tester);
		} else if (expression instanceof XJSemicolonStatement statement) {
			applyTester(statement.getExpression(), tester);
		} else {
			tester.accept(expression);
		}
	}

	protected void assertExpression(final CharSequence input,
			final Consumer<? super XExpression> tester) throws Exception {
		tester.accept(parseHelper.parse(input));
	}

	protected XJVariableDeclaration getVariableDeclaration(final XExpression expression) {
		return (XJVariableDeclaration) expression;
	}

	protected XJArrayConstructorCall getVariableDeclarationRightAsArrayConstructorCall(
			final XExpression expression) {
		return (XJArrayConstructorCall) ((XVariableDeclaration) expression).getRight();
	}

	protected XExpression getVariableDeclarationRight(final XExpression expression) {
		return ((XVariableDeclaration) expression).getRight();
	}

	protected XExpression getAssignmentRight(final XExpression expression) {
		return getAssignment(expression).getValue();
	}

	protected XMemberFeatureCall getMemberFeatureCall(final XExpression expression) {
		return (XMemberFeatureCall) expression;
	}

	protected XJArrayAccessExpression getMemberCallTargetArrayAccess(final XExpression expression) {
		return getArrayAccess(getMemberFeatureCall(expression).getMemberCallTarget());
	}

	protected XJArrayAccessExpression getArrayAccess(final XExpression expression) {
		return (XJArrayAccessExpression) expression;
	}

	protected XFeatureCall getFeatureCall(final XExpression expression) {
		return (XFeatureCall) expression;
	}

	protected XCastedExpression getCasted(final XExpression expression) {
		return (XCastedExpression) expression;
	}

	protected XJConditionalExpression getConditional(final XExpression expression) {
		return (XJConditionalExpression) expression;
	}

	protected XInstanceOfExpression getInstanceOf(final XExpression expression) {
		return (XInstanceOfExpression) expression;
	}

	protected XIfExpression getIfStatement(final XExpression expression) {
		return (XIfExpression) expression;
	}

	protected XWhileExpression getWhileExpression(final XExpression expression) {
		return (XWhileExpression) expression;
	}

	protected XBasicForLoopExpression getBasicForLoop(final XExpression expression) {
		return (XBasicForLoopExpression) expression;
	}

	protected XSwitchExpression getSwitch(final XExpression expression) {
		return (XSwitchExpression) expression;
	}

	protected XThrowExpression getThrowExpression(final XExpression expression) {
		return (XThrowExpression) expression;
	}

	protected XJClassObject getClassObject(final XExpression expression) {
		return (XJClassObject) expression;
	}

	protected XAssignment getAssignment(final XExpression expression) {
		return (XAssignment) expression;
	}

	protected XJAssignment getJAssignment(final XExpression expression) {
		return (XJAssignment) expression;
	}

	protected XBinaryOperation getXBinaryOperation(final XExpression expression) {
		return (XBinaryOperation) expression;
	}

	protected XUnaryOperation getXUnaryOperation(final XExpression expression) {
		return (XUnaryOperation) expression;
	}

	protected XJTryWithResourcesStatement getTryWithResources(final XExpression expression) {
		return (XJTryWithResourcesStatement) expression;
	}

	protected XJConstructorCall getConstructorCall(final XExpression expression) {
		return (XJConstructorCall) expression;
	}
}
