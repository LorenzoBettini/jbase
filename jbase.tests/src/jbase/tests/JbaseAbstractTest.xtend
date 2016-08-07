package jbase.tests

import com.google.inject.Inject
import jbase.jbase.XJArrayAccessExpression
import jbase.jbase.XJArrayConstructorCall
import jbase.jbase.XJAssignment
import jbase.jbase.XJClassObject
import jbase.jbase.XJConditionalExpression
import jbase.jbase.XJConstructorCall
import jbase.jbase.XJSemicolonStatement
import jbase.jbase.XJTryWithResourcesStatement
import jbase.jbase.XJVariableDeclaration
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider
import jbase.tests.inputs.JbaseInputs
import org.eclipse.xtext.junit4.util.ParseHelper
import org.eclipse.xtext.junit4.validation.ValidationTestHelper
import org.eclipse.xtext.xbase.XAssignment
import org.eclipse.xtext.xbase.XBasicForLoopExpression
import org.eclipse.xtext.xbase.XBinaryOperation
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XCastedExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XFeatureCall
import org.eclipse.xtext.xbase.XIfExpression
import org.eclipse.xtext.xbase.XInstanceOfExpression
import org.eclipse.xtext.xbase.XMemberFeatureCall
import org.eclipse.xtext.xbase.XSwitchExpression
import org.eclipse.xtext.xbase.XThrowExpression
import org.eclipse.xtext.xbase.XUnaryOperation
import org.eclipse.xtext.xbase.XVariableDeclaration
import org.eclipse.xtext.xbase.XWhileExpression

import static org.junit.Assert.*

abstract class JbaseAbstractTest {
	@Inject protected extension ParseHelper<XExpression>
	@Inject protected extension ValidationTestHelper
	@Inject protected extension JbaseInputs

	/**
	 * For some tests we need to use JbaseTestlanguage's operations and imports
	 */
	def protected useJbaseTestlanguage() {
		new JbaseTestlanguageInjectorProvider().injector.injectMembers(this)
	}
	
	def protected void parseAndAssertNoErrors(CharSequence input) {
		input.parse => [
			assertNoErrors
		]
	}

	def protected void parseAndAssertNoIssues(CharSequence input) {
		input.parse => [
			assertNoIssues
		]
	}

	def protected assertEqualsStrings(CharSequence expected, CharSequence actual) {
		assertEquals(expected.toString().replaceAll("\r", ""), actual.toString());
	}

	def protected assertLastExpression(CharSequence input, (XExpression)=>void tester) {
		// if there are several lines we transform it into a block
		var toParse = input.toString.trim
		if (toParse.contains("\n")) {
			toParse = '''
			{
				«toParse»
			}
			'''
		}
		val e = toParse.parse
		applyTester(e, tester)
	}

	def protected void applyTester(XExpression e, (XExpression)=>void tester) {
		if (e instanceof XBlockExpression) {
			e.expressions.last.applyTester(tester)
		} else if (e instanceof XJSemicolonStatement) {
			e.expression.applyTester(tester)
		} else {
			tester.apply(e)
		}
	}

	def protected assertExpression(CharSequence input, (XExpression)=>void tester) {
		tester.apply(input.parse)
	}

	protected def getVariableDeclaration(XExpression it) {
		it as XJVariableDeclaration
	}

	protected def getVariableDeclarationRightAsArrayConstructorCall(XExpression it) {
		(it as XVariableDeclaration).right as XJArrayConstructorCall
	}

	protected def getVariableDeclarationRight(XExpression it) {
		(it as XVariableDeclaration).right
	}

	protected def getAssignmentRight(XExpression it) {
		assignment.value
	}

	protected def getMemberFeatureCall(XExpression it) {
		it as XMemberFeatureCall
	}

	protected def getMemberCallTargetArrayAccess(XExpression it) {
		memberFeatureCall.memberCallTarget.arrayAccess
	}

	protected def getArrayAccess(XExpression it) {
		it as XJArrayAccessExpression
	}

	protected def getFeatureCall(XExpression it) {
		it as XFeatureCall
	}

	protected def getCasted(XExpression it) {
		it as XCastedExpression
	}

	protected def getConditional(XExpression it) {
		it as XJConditionalExpression
	}

	protected def getInstanceOf(XExpression it) {
		it as XInstanceOfExpression
	}

	protected def getIfStatement(XExpression it) {
		it as XIfExpression
	}

	protected def getWhileExpression(XExpression it) {
		it as XWhileExpression
	}

	protected def getBasicForLoop(XExpression it) {
		it as XBasicForLoopExpression
	}

	protected def getSwitch(XExpression it) {
		it as XSwitchExpression
	}

	protected def getThrowExpression(XExpression it) {
		it as XThrowExpression
	}

	protected def getClassObject(XExpression it) {
		it as XJClassObject
	}

	protected def getAssignment(XExpression it) {
		it as XAssignment
	}

	protected def getJAssignment(XExpression it) {
		it as XJAssignment
	}

	protected def getXBinaryOperation(XExpression it) {
		it as XBinaryOperation
	}

	protected def getXUnaryOperation(XExpression it) {
		it as XUnaryOperation
	}

	protected def getTryWithResources(XExpression it) {
		it as XJTryWithResourcesStatement
	}

	protected def getConstructorCall(XExpression it) {
		it as XJConstructorCall
	}


}