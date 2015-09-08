package jbase.tests

import com.google.inject.Inject
import org.eclipse.xtext.junit4.util.ParseHelper
import org.eclipse.xtext.junit4.validation.ValidationTestHelper
import org.eclipse.xtext.xbase.XBlockExpression
import org.eclipse.xtext.xbase.XCastedExpression
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.XFeatureCall
import org.eclipse.xtext.xbase.XIfExpression
import org.eclipse.xtext.xbase.XInstanceOfExpression
import org.eclipse.xtext.xbase.XSwitchExpression
import org.eclipse.xtext.xbase.XVariableDeclaration
import jbase.testlanguage.JbaseTestlanguageInjectorProvider
import jbase.tests.inputs.JbaseInputs
import jbase.jbase.XJArrayConstructorCall
import jbase.jbase.XJConditionalExpression
import jbase.jbase.XJMemberFeatureCall
import jbase.jbase.XJVariableDeclaration

import static org.junit.Assert.*
import org.eclipse.xtext.xbase.XThrowExpression
import jbase.jbase.XJClassObject

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
		if (e instanceof XBlockExpression) {
			tester.apply(e.expressions.last)
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

	protected def getMemberFeatureCall(XExpression it) {
		it as XJMemberFeatureCall
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

	protected def getSwitch(XExpression it) {
		it as XSwitchExpression
	}

	protected def getThrowExpression(XExpression it) {
		it as XThrowExpression
	}

	protected def getClassObject(XExpression it) {
		it as XJClassObject
	}
}