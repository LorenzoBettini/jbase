package jbase.tests

import jbase.jbase.XJCharLiteral
import jbase.jbase.XJClassObject
import jbase.jbase.XJPrefixOperation
import jbase.util.JbaseNodeModelUtil
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.nodemodel.util.NodeModelUtils
import org.eclipse.xtext.xbase.XBinaryOperation
import org.eclipse.xtext.xbase.XBooleanLiteral
import org.eclipse.xtext.xbase.XConstructorCall
import org.eclipse.xtext.xbase.XFeatureCall
import org.eclipse.xtext.xbase.XListLiteral
import org.eclipse.xtext.xbase.XMemberFeatureCall
import org.eclipse.xtext.xbase.XNumberLiteral
import org.eclipse.xtext.xbase.XPostfixOperation
import org.eclipse.xtext.xbase.XStringLiteral
import org.eclipse.xtext.xbase.XSynchronizedExpression
import org.eclipse.xtext.xbase.XThrowExpression
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression
import org.eclipse.xtext.xbase.XbasePackage
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseParsingTest extends JbaseAbstractTest {

	@Test def void testAssignmentLeft() {
		'''
		i = 1;
		'''.assertLastExpression [
			assignment.feature as XFeatureCall
		]
	}

	@Test def void testAssignmentRight() {
		'''
		int i;
		i = m();
		'''.assertLastExpression [
			assignment.value as XFeatureCall
		]
	}

	@Test def void testAssignmentRight2() {
		'''
		int j;
		int i;
		i = j;
		'''.assertLastExpression [
			assignment.value as XFeatureCall
		]
	}

	@Test def void testAssignmentIndex() {
		'''
		i[0] = 1;
		'''.assertLastExpression [
			assertTrue(JAssignment.indexes.head instanceof XNumberLiteral)
		]
	}

//	@Test def void testFeatureCallIndex() {
//		'''
//		int[] m() { return null; }
//		m()[0] = 1;
//		'''.parse => [
//			assertTrue((main.expressions.head as JavammXAssignment).index instanceof XNumberLiteral)
//		]
//	}

	@Test def void testFeatureCallIndex2() {
		'''
		i = m()[0];
		'''.assertLastExpression [
			assertTrue(getArrayAccess(assignment.value).indexes.head instanceof XNumberLiteral)
		]
	}

	@Test def void testArrayAccessInParenthesizedExpression() {
		arrayAccessInParenthesizedExpression.assertLastExpression [
			assertTrue(getArrayAccess(assignment.value).indexes.head instanceof XNumberLiteral)
		]
	}

	@Test def void testMultiArrayAccessInRightHandsideExpression() {
		multiArrayAccessInRightHandsideExpression.assertLastExpression [
			val indexes = getArrayAccess(assignment.value).indexes
			assertTrue(indexes.head instanceof XNumberLiteral)
			assertTrue(indexes.last instanceof XBinaryOperation)
		]
	}

	@Test def void testMultiArrayAccessInLeftHandsideExpression() {
		'''
		int[][] a;
		a[0][1+2] = 1;
		'''.assertLastExpression [
			val indexes = JAssignment.indexes
			assertTrue(indexes.head instanceof XNumberLiteral)
			assertTrue(indexes.last instanceof XBinaryOperation)
		]
	}

	@Test def void testSystemOut() {
		'''
		System.out;
		'''.assertLastExpression [
			memberFeatureCall.memberCallTarget
		]
	}

	@Test def void testSystemOutPrintln() {
		'''
		System.out.println("a");
		'''.assertLastExpression [
			// the whole call
			memberFeatureCall => [
				// System.out
				(memberCallTarget as XMemberFeatureCall)
				// the argument
				(memberCallArguments.head as XStringLiteral)
			]
		]
	}

	@Test def void testConstructorCall() {
		'''
		new String();
		'''.assertLastExpression [
			(it as XConstructorCall).arguments
		]
	}

	@Test def void testArrayConstructorCall() {
		'''
		int[] a = new int[0];
		'''.assertLastExpression [
			assertTrue(getVariableDeclarationRightAsArrayConstructorCall.indexes.head instanceof XNumberLiteral)
		]
	}

	@Test def void testMultiArrayConstructorCall() {
		'''
		// of course true is not a valid array index
		// but we only test parsing here
		int[][] a = new int[0][true];
		'''.assertLastExpression [
			getVariableDeclarationRightAsArrayConstructorCall.indexes => [
				assertTrue(head instanceof XNumberLiteral)
				assertTrue(last instanceof XBooleanLiteral)
			]
		]
	}

	@Test def void testIncompleteArrayConstructorCall() {
		'''
		int[] a = new int[
		'''.assertLastExpression [
			assertTrue(getVariableDeclarationRightAsArrayConstructorCall.indexes.empty)
		]
	}

	@Test def void testIncompleteArrayConstructorCall2() {
		'''
		int[] a = new int[][
		'''.assertLastExpression [
			getVariableDeclarationRightAsArrayConstructorCall => [
				indexes.empty.assertTrue
				// the second dimension is parsed
				assertEquals(2, dimensions.size)
			]
		]
	}

	@Test def void testArrayConstructorCallWithDimensionsAndNoIndexes() {
		'''
		int[] a = new int[][][];
		'''.assertLastExpression [
			getVariableDeclarationRightAsArrayConstructorCall => [
				indexes.empty.assertTrue
				assertEquals(3, dimensions.size)
			]
		]
	}

	@Test def void testArrayConstructorCallWithDimensionsAndIndexes() {
		'''
		int[] a = new int[0][0][0];
		'''.assertLastExpression [
			getVariableDeclarationRightAsArrayConstructorCall => [
				assertEquals(3, indexes.size)
				assertEquals(3, dimensions.size)
			]
		]
	}

	@Test def void testArrayConstructorCallWithDimensionsAndSomeIndexes() {
		'''
		int[] a = new int[0][][0];
		'''.assertLastExpression [
			getVariableDeclarationRightAsArrayConstructorCall => [
				assertEquals(2, indexes.size)
				assertEquals(3, dimensions.size)
			]
		]
	}

	@Test def void testArrayConstructorCallWithDimensionsAndArrayLiteral() {
		'''
		int[] a = new int[][][] {};
		'''.assertLastExpression [
			getVariableDeclarationRightAsArrayConstructorCall => [
				assertEquals(3, dimensions.size)
				arrayLiteral.assertNotNull
			]
		]
	}

	@Test def void testArrayConstructorCallWithDimensionsAndIncompleteArrayLiteral() {
		'''
		int[] a = new int[][][] {
		'''.assertLastExpression [
			getVariableDeclarationRightAsArrayConstructorCall
		]
	}
	
	@Test def void testArrayLiteral() {
		arrayLiteral.assertLastExpression [
			assertEquals(
				3,
				(variableDeclarationRight
					as XListLiteral
				).elements.size
			)
		]
	}

	@Test def void testStringLiteral() {
		'''
		"a";
		'''.assertLastExpression[
			assertEquals(1, (it as XStringLiteral).value.length)
		]
	}

	@Test def void testCharLiteral() {
		'''
		'a';
		'''.assertLastExpression[
			assertEquals(1, (it as XJCharLiteral).value.length)
		]
	}

	@Test def void testIncompleteVariableDeclaration() {
		'''
		int i = a
		'''.assertLastExpression[
			variableDeclarationRight.assertNotNull
		]
	}

	@Test def void testIncompleteVariableDeclaration2() {
		'''
		int i =
		'''.assertLastExpression[
			variableDeclarationRight.assertNull
		]
	}

	@Test def void testIncompleteVariableDeclaration3() {
		'''
		int i = ;
		'''.assertLastExpression[
			variableDeclarationRight.assertNull
		]
	}

	@Test def void testIncompleteFeatureCall() {
		'''
		my
		'''.assertLastExpression[
			featureCall.actualReceiver.assertNull
		]
	}

	@Test def void testIncompleteMemberCall() {
		'''
		String s = "";
		s.m
		'''.assertLastExpression[
			memberFeatureCall.actualReceiver.assertNotNull
		]
	}

	@Test def void testIncompleteMemberCall2() {
		'''
		s.m
		'''.assertLastExpression[
			memberFeatureCall.actualReceiver.assertNotNull
		]
	}

	@Test def void testIncompleteMemberCall4() {
		'''
		s.
		'''.assertLastExpression[
			memberFeatureCall.actualReceiver.assertNotNull
		]
	}

	@Test def void testWhileWithBlocks() {
		'''
		while (i < 10) {
			i = i + 1;
		}
		'''.assertLastExpression[
			whileExpression.predicate.assertNotNull
		]
	}

	@Test def void testSeveralVariableDeclarations() {
		'''
		int i, j = 0, k;
		'''.assertLastExpression[
			variableDeclaration => [
				type.assertNotNull
				additionalVariables => [
					2.assertEquals(size)
					get(0).right.assertNotNull
					get(1).right.assertNull
				]	
			]
		]
	}

	@Test def void testSeveralVariableDeclarationsInForLoop() {
		'''
		for (int i, j = 0, k; i < 0; i++) {}
		'''.assertLastExpression[
			basicForLoop.initExpressions.head.variableDeclaration => [
				type.assertNotNull
				additionalVariables => [
					2.assertEquals(size)
					get(0).right.assertNotNull
					get(1).right.assertNull
				]	
			]
		]
	}

	@Test def void testSeveralAssignmentsInForLoop() {
		'''
		int i;
		int j;
		int k;
		for (i = 0, j = 0, k = 0; i < 0; i++) {}
		'''.assertLastExpression[
			3.assertEquals(basicForLoop.initExpressions.size)
		]
	}

	@Test def void testPostfixOperation() {
		'''
		int i = 0;
		i++;
		'''.assertLastExpression[
			assertTrue((it as XPostfixOperation).operand instanceof XFeatureCall)
		]
	}

	@Test def void testPrefixOperation() {
		'''
		int i = 0;
		++i;
		'''.assertLastExpression[
			assertTrue((it as XJPrefixOperation).operand instanceof XFeatureCall)
		]
	}

	@Test def void testMemberFeatureCallIndex() {
		'''
		int[][] arr;
		arr[0].length;
		'''.assertLastExpression [
			assertFalse(memberCallTargetArrayAccess.indexes.empty)
		]
	}

	@Test def void testIncompleteMemberFeatureCallIndex() {
		'''
		arr[0].;
		'''.assertLastExpression [
			assertFalse(memberCallTargetArrayAccess.indexes.empty)
		]
	}

	@Test def void testMemberCallTargetOfJavammMemberFeatureCallIsArrayAccessExpression() {
		'''
		int[][] arr;
		arr[0].length;
		'''.assertLastExpression [
			memberCallTargetArrayAccess
		]
	}

	@Test def void testCastedExpression() {
		'''
		(int) 10;
		'''.assertLastExpression[
			casted => [
				assertEquals("int", type.simpleName)
				assertTrue(target instanceof XNumberLiteral)
			]
		]
	}

	@Test def void testIncompleteCastedExpressionParsedAsParenthesizedExpression() {
		'''
		(int) ;
		'''.assertLastExpression[
			featureCall
		]
	}

	@Test def void testCastedExpressionWithParenthesis() {
		'''
		(char) ((int) 10);
		'''.assertLastExpression[
			casted => [
				assertEquals("char", type.simpleName)
				getCasted(target) => [
					assertEquals("int", type.simpleName)
					assertTrue(target instanceof XNumberLiteral)
				]
			]
		]
	}

	@Test def void testCastedExpressionRightAssociativity() {
		'''
		(char) (int) 10; // equivalent to (char) ((int) 10)
		'''.assertLastExpression[
			casted => [
				assertEquals("char", type.simpleName)
				getCasted(target) => [
					assertEquals("int", type.simpleName)
					assertTrue(target instanceof XNumberLiteral)
				]
			]
		]
	}

	@Test def void testConditionalExpression() {
		'''
		int j = 0;
		int i = j > 0 ? 1 : '2';
		'''.assertLastExpression[
			variableDeclarationRight.conditional => [
				assertTrue(then instanceof XNumberLiteral)
				assertTrue(^else instanceof XStringLiteral)
			]
		]
	}

	@Test def void testIncompleteConditionalExpression() {
		'''
		int i = j > 0 ? 
		'''.assertLastExpression[
			variableDeclarationRight.conditional => [
				assertNull(then)
			]
		]
	}

	@Test def void testIncompleteConditionalExpression2() {
		'''
		int i = j > 0 ? 1
		'''.assertLastExpression[
			variableDeclarationRight.conditional => [
				assertTrue(then instanceof XNumberLiteral)
				assertNull(^else)
			]
		]
	}

	@Test def void testIncompleteConditionalExpression3() {
		'''
		int i = j > 0 ? 1 :
		'''.assertLastExpression[
			variableDeclarationRight.conditional => [
				assertTrue(then instanceof XNumberLiteral)
				assertNull(^else)
			]
		]
	}

	@Test def void testInstanceOf() {
		'''
		String s;
		boolean b = s instanceof String;
		'''.assertLastExpression[
			variableDeclarationRight.instanceOf => [
				assertTrue(expression instanceof XFeatureCall)
			]
		]
	}

	@Test def void testIncompleteInstanceOf() {
		'''
		boolean b = s instanceof 
		'''.assertLastExpression[
			variableDeclarationRight.instanceOf => [
				assertTrue(expression instanceof XFeatureCall)
			]
		]
	}

	@Test def void testNoQuestionMarkParsedAsBinaryExpression() {
		'''
		int j = 0;
		boolean b = j > 0;
		'''.assertLastExpression[
			assertTrue(variableDeclarationRight instanceof XBinaryOperation)
		]
	}

	@Test
	def void testSwitchWithoutCase() {
		'''
		switch (argsNum) {
			case 
			default: System.out.println("default");
		}
		'''.assertExpression[
			getSwitch.cases.head.^case.assertNull
		]
	}

	@Test
	def void testSwitchWithoutCaseThen() {
		'''
		switch (argsNum) {
			case 0
			default: System.out.println("default");
		}
		'''.assertExpression[
			// then part is always not null in Jbase
			getSwitch.cases.head.then.assertNotNull
		]
	}

	@Test
	def void testThrowWithoutExpression() {
		'''
		throw
		'''.assertLastExpression[
			assertTrue("class: " + it.class, it instanceof XThrowExpression)
		]
	}

	@Test
	def void testThrowWithExpressionWithoutSemicolon() {
		'''
		throw new Exception()
		'''.assertLastExpression[
			throwExpression.expression => [
				assertTrue("class: " + it.class, it instanceof XConstructorCall)
			]
		]
	}

	@Test
	def void testTryCatchIncomplete() {
		'''
		try
		'''.assertLastExpression[
			assertTrue("class: " + it.class, it instanceof XTryCatchFinallyExpression)
		]
	}

	@Test
	def void testSynchronizedIncomplete() {
		'''
		synchronized (
		'''.assertLastExpression[
			assertTrue("class: " + it.class, it instanceof XSynchronizedExpression)
		]
	}

	@Test
	def void testClassObject() {
		'''
		String.class
		'''.assertLastExpression[
			assertTrue("class: " + it.class, it instanceof XJClassObject)
		]
	}

	@Test
	def void testClassObjectWithArrayDimensions() {
		'''
		String[].class
		'''.assertLastExpression[
			classObject => [
				"String".assertEquals(typeExpression.toString)
				1.assertEquals(arrayDimensions.size)
			]
		]
	}

	@Test
	def void testClassObjectWithWrongTypeExpression() {
		// we parse it as a ClassObject though it is not well-typed
		'''
		a.class
		'''.assertLastExpression[
			classObject => [
				"a".assertEquals(typeExpression.toString)
			]
		]
	}

	@Test
	def void testBooleanAnd() {
		'''
		true && false;
		'''.assertBinaryOperation(
			"org.eclipse.xtext.xbase.lib.BooleanExtensions.operator_and(boolean,boolean)")
	}

	@Test
	def void testBitwiseAnd() {
		'''
		0 & 1;
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseAnd(int,int)")
	}

	@Test
	def void testBooleanOr() {
		'''
		true || false;
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.BooleanExtensions.operator_or(boolean,boolean)")
	}

	@Test
	def void testBitwiseOr() {
		'''
		0 | 1;
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseOr(int,int)")
	}

	@Test
	def void testBitwiseXor() {
		'''
		0 ^ 1;
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseXor(int,int)")
	}

	@Test
	def void testBitwiseNot() {
		'''
		~1;
		'''.assertUnaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseNot(int)")
	}

	@Test
	def void testLeftShift() {
		'''
		1 << 2;
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_doubleLessThan(int,int)")
	}

	@Test
	def void testRightShift() {
		'''
		1 >> 2;
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_doubleGreaterThan(int,int)")
	}

	@Test
	def void testUnsignedRightShift() {
		'''
		1 >>> 2;
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_tripleGreaterThan(int,int)")
	}

	@Test
	def void testBitwiseOnChar() {
		'''
		'a' >>> 'b';
		'''.assertBinaryOperation("org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_tripleGreaterThan(int,int)")
	}

	@Test
	def void testAndOrPrecedence() {
		'''
		1 && 2 || 3 && 4
		'''.assertPrecedence(
		'''
		((1 && 2) || (3 && 4))
		'''
		)
	}

	@Test
	def void testBitwisePrecedenceAnd() {
		'''
		1 && 2 & 3
		'''.assertPrecedence(
		'''
		(1 && (2 & 3))
		'''
		)
	}

	@Test
	def void testBitwisePrecedenceAndOr() {
		'''
		1 | 2 && 3
		'''.assertPrecedence(
		'''
		((1 | 2) && 3)
		'''
		)
	}

	@Test
	def void testBitwisePrecedence() {
		'''
		1 && 2 & 3 ^ 4
		'''.assertPrecedence(
		'''
		(1 && ((2 & 3) ^ 4))
		'''
		)
	}

	@Test
	def void testBooleanBitwiseXor() {
		'''
		true ^ false;
		'''.assertBinaryOperation("jbase.lib.JbaseBooleanExtensions.bitwiseXor(boolean,boolean)")
	}

	def private assertBinaryOperation(CharSequence input, String expectedResolvedFeature) {
		input.assertLastExpression[
			XBinaryOperation => [
				expectedResolvedFeature.
				assertEquals(feature.identifier)
			]
		]
	}

	def private assertUnaryOperation(CharSequence input, String expectedResolvedFeature) {
		input.assertLastExpression[
			XUnaryOperation => [
				expectedResolvedFeature.
				assertEquals(feature.identifier)
			]
		]
	}

	def private assertPrecedence(CharSequence input, CharSequence expected) {
		input.assertLastExpression[
			expected.toString.trim.assertEquals(stringRepr)
		]
	}

	def private String stringRepr(EObject model) {
		val util = new JbaseNodeModelUtil
		switch (model) {
			XBinaryOperation:
				"(" + 
				stringRepr(model.leftOperand) + " " +
				NodeModelUtils.getTokenText(
					NodeModelUtils.
						findNodesForFeature(model, XbasePackage.eINSTANCE.XAbstractFeatureCall_Feature).
						head
				) + " " +
				stringRepr(model.rightOperand)
				+ ")"
			default: util.getProgramText(model)
		}
	}
}