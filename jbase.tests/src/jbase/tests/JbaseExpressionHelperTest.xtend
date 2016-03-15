package jbase.tests

import com.google.inject.Inject
import jbase.JbaseInjectorProvider
import jbase.util.JbaseExpressionHelper
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.xbase.XUnaryOperation
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseExpressionHelperTest extends JbaseAbstractTest {

	@Inject JbaseExpressionHelper helper

	@Test def void testSpecialHandlingTrue() {
		'''
			-1;
		'''.assertUnaryOperationSpecialHandling(true)
	}

	@Test def void testSpecialHandlingTrue2() {
		'''
			-(-1);
		'''.assertUnaryOperationSpecialHandling(true)
	}

	@Test def void testSpecialHandlingFalse() {
		'''
			-(-'a');
		'''.assertUnaryOperationSpecialHandling(false)
	}

	@Test def void testSpecialHandlingFalse2() {
		'''
			!1;
		'''.assertUnaryOperationSpecialHandling(false)
	}

	@Test def void testSpecialHandlingFalseExplicit() {
		'''
			-1;
		'''.assertUnaryOperationSpecialHandlingWithBaseCaseFailure
	}

	@Test def void testBaseCase() {
		'''
			-1;
		'''.assertUnaryOperationSpecialHandling("-1")
	}

	@Test def void testStepCase() {
		'''
			-(-1);
		'''.assertUnaryOperationSpecialHandling("-(-1)")
	}

	@Test def void testStepCase2() {
		'''
			-(+1);
		'''.assertUnaryOperationSpecialHandling("-(+1)")
	}

	def private assertUnaryOperationSpecialHandling(CharSequence input, boolean expected) {
		input.assertLastExpression [
			expected.assertEquals(helper.specialHandling(it as XUnaryOperation))
		]
	}

	def private assertUnaryOperationSpecialHandling(CharSequence input, CharSequence expected) {
		input.assertLastExpression [
			val buffer = new StringBuilder
			helper.specialHandling(
				it as XUnaryOperation,
				[ unaryOperation, numLiteral |
					buffer.append(unaryOperation.getConcreteSyntaxFeatureName + numLiteral.value)
					return true
				],
				[ unaryOperation |
					buffer.insert(0, unaryOperation.getConcreteSyntaxFeatureName + "(").
						append(")")
				]
			)
			expected.toString.assertEquals(buffer.toString)
		]
	}

	def private assertUnaryOperationSpecialHandlingWithBaseCaseFailure(CharSequence input) {
		input.assertLastExpression [
			helper.specialHandling(
				it as XUnaryOperation,
				[ unaryOperation, numLiteral |
					return false
				],
				[]
			).assertFalse()
		]
	}

}