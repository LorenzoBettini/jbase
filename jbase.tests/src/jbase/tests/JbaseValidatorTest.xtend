package jbase.tests

import com.google.inject.Inject
import jbase.jbase.JbasePackage
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider
import jbase.tests.inputs.JbaseTestlanguageInputs
import jbase.validation.JbaseIssueCodes
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.diagnostics.Diagnostic
import org.eclipse.xtext.diagnostics.Severity
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.validation.Issue
import org.eclipse.xtext.xbase.XbasePackage
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotationsPackage
import org.eclipse.xtext.xbase.validation.IssueCodes
import org.eclipse.xtext.xtype.XtypePackage
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

/**
 * For validation tests we use JbaseTestlanguage since we can also use
 * expectations for operation's return type.
 */
@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseTestlanguageInjectorProvider))
class JbaseValidatorTest extends JbaseAbstractTest {

	/**
	 * Since we use both Jbase and JbaseTestlanguage we specify EObject
	 * as the type returned by ParseHelper
	 */
	@Inject protected extension ParseHelper<EObject>

	@Inject extension JbaseTestlanguageInputs

	val jbasePackage = JbasePackage.eINSTANCE

	@Test def void testArrayIndexNotIntegerLeft() {
		'''
		args[true] = 0;
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBooleanLiteral,
			"int",
			"boolean"
		)
	}

	@Test def void testArrayIndexNotIntegerRight() {
		'''
		String s = args[true];
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBooleanLiteral,
			"int",
			"boolean"
		)
	}

	@Test def void testArrayIndexNotIntegerInArrayConstructorCall() {
		'''
		int[] i = new int[true];
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBooleanLiteral,
			"int",
			"boolean"
		)
	}

	@Test def void testArrayIndexNotIntegerInMemberFeatureCall() {
		'''
		int[][] a;
		int l = a[true].length;
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBooleanLiteral,
			"int",
			"boolean"
		)
	}

	@Test def void testTypeMismatchInArrayDimensionExpression() {
		'''
		{
			int[] i = new int[] { 1, true, 2};
			int[][] j = new int[][] {{ 1, 2}, { 1, "foo", 2}};
			int[][] j = new int[][] { 1 };
		}
		'''.parse => [
			assertTypeMismatch(
				XbasePackage.eINSTANCE.XBooleanLiteral,
				"int",
				"boolean"
			)
			assertTypeMismatch(
				XbasePackage.eINSTANCE.XStringLiteral,
				"int",
				"String"
			)
			assertTypeMismatch(
				XbasePackage.eINSTANCE.XNumberLiteral,
				"int[]",
				"int"
			)
		]
	}

	@Test def void testArrayConstructorSpecifiesNeitherDimensionExpressionNorInitializer() {
		'''
		int[] a = new int[];
		'''.parse.assertError(
			jbasePackage.XJArrayConstructorCall,
			JbaseIssueCodes.ARRAY_CONSTRUCTOR_EITHER_DIMENSION_EXPRESSION_OR_INITIALIZER,
			"Constructor must provide either dimension expressions or an array initializer"
		)
	}

	@Test def void testArrayConstructorSpecifiesBothDimensionExpressionAndInitializer() {
		'''
		int[] j = new int[1] {};
		'''.parse.assertError(
			jbasePackage.XJArrayConstructorCall,
			JbaseIssueCodes.ARRAY_CONSTRUCTOR_BOTH_DIMENSION_EXPRESSION_AND_INITIALIZER,
			"Cannot define dimension expressions when an array initializer is provided"
		)
	}

	@Test def void testArrayConstructorSpecifiesDimensionExpressionAfterEmptyDimension() {
		'''
		int[][] j = new int[][0];
		'''.parse.assertError(
			XbasePackage.eINSTANCE.XNumberLiteral,
			JbaseIssueCodes.ARRAY_CONSTRUCTOR_DIMENSION_EXPRESSION_AFTER_EMPTY_DIMENSION,
			"Cannot specify an array dimension after an empty dimension"
		)
	}

	@Test def void testArrayConstructorSpecifiesDimensionExpressionAfterEmptyDimension2() {
		'''
		int[][][] j = new int[0][][1];
		'''.parse.assertError(
			XbasePackage.eINSTANCE.XNumberLiteral,
			JbaseIssueCodes.ARRAY_CONSTRUCTOR_DIMENSION_EXPRESSION_AFTER_EMPTY_DIMENSION,
			"Cannot specify an array dimension after an empty dimension"
		)
	}

	@Test def void testDiamondConstructorCallInVarDecl() {
		(constructorCallWithDiamondInVarDecl
		+
		// to avoid unused variable warnings
		'''
		System.out.println(list1);
		System.out.println(list2);
		System.out.println(list3);
		'''
		).parse.assertNoIssues
	}

	@Test def void testDiamondConstructorCallInVarDeclNestedWildcard() {
		(constructorCallWithDiamondInVarDeclNestedWildcard
		+
		// to avoid unused variable warnings
		'''
		System.out.println(list3);
		'''
		).parse.assertNoIssues
	}

	@Test def void testDiamondConstructorCallInAssignment() {
		(constructorCallWithDiamondInAssignment
		+
		// to avoid unused variable warnings
		'''
		System.out.println(list1);
		System.out.println(list2);
		System.out.println(list3);
		'''
		).parse.assertNoIssues
	}

	@Test def void testDiamondConstructorCallInAssignmentNestedWildcard() {
		(constructorCallWithDiamondInAssignmentNestedWildcard
		+
		// to avoid unused variable warnings
		'''
		System.out.println(list3);
		'''
		).parse.assertNoIssues
	}

	@Test def void testConstructorCallRawType() {
		'''
		System.out.println(new java.util.ArrayList());
		'''.parse.assertWarningRawType
	}

	@Test def void testConstructorCallRawType2() {
		'''
		java.util.List<String> s = new java.util.ArrayList();
		'''.parse.assertWarningRawType
	}

	@Test def void testConstructorCallNotRawType() {
		'''
		System.out.println(new java.util.ArrayList<>());
		'''.parse.assertNoIssues
	}

	@Test def void testNotArrayTypeLeft() {
		'''
		int i;
		i[0] = 0;
		'''.parse.assertError(
			jbasePackage.XJAssignment,
			JbaseIssueCodes.NOT_ARRAY_TYPE,
			"The type of the expression must be an array type but it resolved to int"
		)
	}

	@Test def void testNotArrayTypeRight() {
		'''
		int i;
		i = i[0];
		'''.parse.assertError(
			jbasePackage.XJArrayAccessExpression,
			JbaseIssueCodes.NOT_ARRAY_TYPE,
			"The type of the expression must be an array type but it resolved to int"
		)
	}

	@Test def void testNotMultiArrayTypeRight() {
		'''
		int[] i;
		i = i[0][1];
		'''.parse.assertError(
			jbasePackage.XJArrayAccessExpression,
			JbaseIssueCodes.NOT_ARRAY_TYPE,
			"The type of the expression must be an array type but it resolved to int"
		)
	}

	@Test def void testNotMultiArrayTypeLeft() {
		'''
		int[] i;
		i[0][1] = 1;
		'''.parse.assertError(
			jbasePackage.XJAssignment,
			JbaseIssueCodes.NOT_ARRAY_TYPE,
			"The type of the expression must be an array type but it resolved to int"
		)
	}

	@Test def void testWrongMultiArrayConstructor() {
		'''
		int[] i = new int[0][1];
		'''.parse.assertTypeMismatch(
			jbasePackage.XJArrayConstructorCall,
			"int[]", "int[][]"
		)
	}

	@Test def void testWrongElementInArrayLiteral() {
		'''
		int[] i = {0, true};
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBooleanLiteral,
			"int",
			"boolean"
		)
	}

	@Test def void testStringLiteralAsStatement() {
		'''
		"test";
		'''.parse.assertNoSideEffectError(XbasePackage.eINSTANCE.XStringLiteral)
	}

	@Test def void testBooleanExpressionAsStatement() {
		'''
		"test" != "a";
		'''.parse => [
			assertNoSideEffectError(XbasePackage.eINSTANCE.XBinaryOperation)
			assertErrorsAsStrings("This expression is not allowed in this context, since it doesn't cause any side effects.")
		]
	}

	@Test def void testDeadCodeAfterReturn() {
		'''
		void m() {
			return;
			System.out.println("");
		}
		'''.parse.assertUnreachableExpression(jbasePackage.XJSemicolonStatement)
	}

	@Test def void testDeadCodeInForLoopTranslatedToJavaWhileEarlyExit() {
		forLoopTranslatedToJavaWhileEarlyExit.
			parse.assertUnreachableExpression(XbasePackage.eINSTANCE.XBinaryOperation)
	}

	@Test def void testInvalidContinue() {
		'''
		if (true)
			continue;
		'''.parse.assertInvalidContinueStatement
	}

	@Test def void testInvalidBreak() {
		'''
		if (true)
			break;
		'''.parse.assertInvalidBreakStatement
	}

	@Test def void testInvalidCharAssignment() {
		'''
		char c = "c";
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XStringLiteral,
			"char", "String"
		)
	}

	@Test def void testInvalidStringAssignment() {
		'''
		String s = 's';
		'''.parse.assertTypeMismatch(
			jbasePackage.XJCharLiteral,
			"String", "char"
		)
	}

	@Test def void testInvalidCharLiteralAssignmentToBoolean() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=457779
		'''
		boolean b = 's';
		'''.parse.assertTypeMismatch(
			jbasePackage.XJCharLiteral,
			"boolean", "char"
		)
	}

	@Test def void testBooleanLiteralAssignableToBoolean() {
		'''
		boolean b = true;
		'''.parseAndAssertNoErrors
	}

	@Test def void testInvalidSwitchExpression() {
		'''
		switch (return 0) {
			default: 
				System.out.println("default");
				break;
		}
		'''.parse.assertError(
			XbasePackage.eINSTANCE.XSwitchExpression,
			Diagnostic.SYNTAX_DIAGNOSTIC,
			"no viable alternative at input 'return'"
		)
	}

	@Test def void testInvalidSwitchCaseType() {
		'''
		String firstArg = args[0];
		char arg = firstArg.toCharArray()[0];
		switch (arg) {
			case "f" : 
				System.out.println("0");
				break;
			default: 
				System.out.println("default");
				break;
		}
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XStringLiteral,
			"char", "String"
		)
	}

	@Test def void testInvalidSwitchReturnType() {
		'''
		op move(int p) : int {
			switch (p) {
				case 0: return "2";
				default: return -1;
			}
		}
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XStringLiteral,
			"int", "String"
		)
	}

	@Test def void testInvalidSwitchReturnType2() {
		'''
		op move(int p) : int {
			switch (p) {
				case 0: System.out.println("0"); break;
				default: return -1;
			}
		}
		'''.parse.assertTypeMismatch(
			jbasePackage.XJBreakStatement,
			"int", "void"
		)
	}

	@Test def void testInvalidSwitchWithoutDefault() {
		'''
		op move(int p) : int {
			switch (p) {
				case 0: return 1;
			}
		}
		'''.parse.assertErrorsAsStrings('''
			Missing default branch in the presence of expected type int
			Missing return'''
		)
	}

	@Test def void testValidSwitchReturnTypeWithFallback() {
		'''
		op move(int p) : int {
			switch (p) {
				case 0: System.out.println("0"); // the default is executed
				default: return -1;
			}
		}
		'''.parse.assertNoErrors
	}

	@Test def void testMissingSemicolonInAssignment() {
		'''
		int i = 0;
		i = 1
		'''.parse.assertMissingSemicolon(XbasePackage.eINSTANCE.XAssignment)
	}

	@Test def void testMissingSemicolonInVariableDeclaration() {
		'''
		int i = 0
		'''.parse.assertMissingSemicolon(XbasePackage.eINSTANCE.XVariableDeclaration)
	}

	@Test def void testMissingSemicolonInContinue() {
		'''
		continue
		'''.parse.assertMissingSemicolon(jbasePackage.XJContinueStatement)
	}

	@Test def void testMissingSemicolonInBreak() {
		'''
		break
		'''.parse.assertMissingSemicolon(jbasePackage.XJBreakStatement)
	}

	@Test def void testMissingSemicolonInReturn() {
		'''
		op m() : void {
			return
		}
		'''.parse.assertMissingSemicolon(XbasePackage.eINSTANCE.XReturnExpression)
	}

	@Test def void testMissingSemicolonInDoWhile() {
		'''
		do {
			
		} while (true)
		'''.parse.assertMissingSemicolon(XbasePackage.eINSTANCE.XDoWhileExpression)
	}

	@Test def void testMissingSemicolonInFeatureCall() {
		'''
		i
		'''.parse.assertMissingSemicolon(XbasePackage.eINSTANCE.XFeatureCall)
	}

	@Test def void testMissingSemicolonInFeatureCall2() {
		'''
		i()
		'''.parse.assertMissingSemicolon(XbasePackage.eINSTANCE.XFeatureCall)
	}

	@Test def void testMissingSemicolonInMemberFeatureCall() {
		'''
		System.out.println()
		'''.parse.assertMissingSemicolon(XbasePackage.eINSTANCE.XMemberFeatureCall)
	}

	@Test def void testMissingSemicolonInImport() {
		'''
		import java.util.List
		'''.parse.assertMissingSemicolon(XtypePackage.eINSTANCE.XImportDeclaration)
	}

	@Test def void testTwoSemicolonsInImportIsOk() {
		'''
		import java.util.List;;
		'''.parseAndAssertNoErrors
	}

	@Test def void testMissingParenthesesForMemberCall() {
		'''
		System.out.println;
		'''.parse.assertMissingParentheses(XbasePackage.eINSTANCE.XMemberFeatureCall)
	}

	@Test def void testMissingParenthesesForMemberCall2() {
		'''
		System.out.println
		'''.parse.assertMissingParentheses(XbasePackage.eINSTANCE.XMemberFeatureCall)
	}

	@Test def void testMissingParenthesesForMethodCall() {
		'''
		op m() {}
		m;
		'''.parse.assertMissingParentheses(XbasePackage.eINSTANCE.XFeatureCall)
	}

	@Test def void testMissingParenthesesForMethodCall2() {
		'''
		op m() {}
		m
		'''.parse.assertMissingParentheses(XbasePackage.eINSTANCE.XFeatureCall)
	}

	@Test def void testMissingParenthesesForArrayLengthOK() {
		'''
		int i = args.length;
		'''.parseAndAssertNoErrors
	}

	@Test def void testMissingParenthesesForConstructor() {
		'''
		new String;
		'''.parse.assertMissingParentheses(
			XbasePackage.eINSTANCE.XConstructorCall,
			"Expression"
		)
	}

	@Test def void testInvalidXbaseOperator() {
		'''
		int i;
		System.out.println(i === 0);
		'''.parse.assertError(
			XbasePackage.eINSTANCE.XBinaryOperation,
			Diagnostic.SYNTAX_DIAGNOSTIC,
			"no viable alternative at input '='"
		)
	}

	@Test def void testParams() {
		'''
		op m(int i, boolean b, String i) : void {}
		'''.parse.assertErrorsAsStrings(
		'''Duplicate local variable i'''
		)
	}

	@Test def void testWrongVariableDeclarationInSeveralDeclarations() {
		'''
		int i = 0, j = "a", k = true;
		'''.parse.assertErrorsAsStrings(
		'''
		Type mismatch: cannot convert from String to int
		Type mismatch: cannot convert from boolean to int'''
		)
	}

	@Test def void testValidAccessToVariable() {
		'''
		int i = 0;
		System.out.println(i);
		'''.parse.assertNoIssues
	}

	@Test def void testVariableDeclarationsNoUnusedWarningsWhenUsed() {
		'''
		{
			int i = 0, j = 0, k = 0;

			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
		}
		'''.parse.assertNoIssues
	}

	@Test def void testUnusedSeveralVariableDeclarations() {
		'''
		{
			int i = 0, j = 0, k = 0;
			System.out.println(j);
		}
		'''.parse.assertIssuesAsStrings(
		'''
		The value of the local variable i is not used
		The value of the local variable k is not used
		'''
		)
	}

	@Test def void testLocalVariabilesAreUnusedWhenOnlyLeftOperandOfAnAssignmentOperator() {
		// local variables are used when they are not used as the left operand of an assignment operator.
		'''
		{
			int i = 0;
			i = 1;
		}
		'''.parse.assertIssuesAsStrings(
		'''
		The value of the local variable i is not used
		'''
		)
	}

	@Test def void testArrayIsUnusedWhenOnlyLeftOperandOfAnAssignmentOperator() {
		'''
		{
			int[] i;
			i = new int[] { 0 };
		}
		'''.parse.assertIssuesAsStrings(
		'''
		The value of the local variable i is not used
		'''
		)
	}

	@Test def void testArrayUsedWhenAssigningToItsElements() {
		'''
		{
			int[] i = { 0 };
			i[0] = 1;
		}
		'''.parseAndAssertNoIssues
	}

	@Test def void testUnusedField() {
		'''
		i : int
		method move() : void {
			i = 0;
		}
		'''.parse.assertIssuesAsStrings(
		'''
		The value of the property i is not used
		'''
		)
	}

	@Test def void testPostfixOnWrongExpression() {
		'''
		"a"++;
		'''.parse.assertIssuesAsStrings(
		'''
		++ cannot be resolved.
		'''
		)
	}

	@Test def void testWrongPostfixOnMethodCall() {
		'''
		op m() { return 0; }
		
		int i = m()++;
		'''.parse.assertErrorsAsStrings(
		'''
		The left-hand side of an assignment must be a variable
		'''
		)
	}

	@Test def void testPostfixOnArrayAccess() {
		'''
		{
			int i = 0;
			int[] a = {1,2,3,4};
			a[i]++;
		}
		'''.parseAndAssertNoIssues
	}

	@Test def void testWrongPostfixOnNonAbstractFeatureCall() {
		'''
		double i = 0.1++;
		'''.parse.assertErrorsAsStrings(
		'''
		The left-hand side of an assignment must be a variable
		'''
		)
	}

	@Test def void testPrefixOnWrongExpression() {
		'''
		++"a";
		'''.parse.assertIssuesAsStrings(
		'''
		++ cannot be resolved.
		'''
		)
	}

	@Test def void testWrongPrefixOnMethodCall() {
		'''
		op m() { return 0; }
		
		int i = ++m();
		'''.parse.assertErrorsAsStrings(
		'''
		The left-hand side of an assignment must be a variable
		'''
		)
	}

	@Test def void testArrayAccessOnMemberFeatureCallNotAnArray() {
		'''
		int a;
		int l = a[0].length;
		'''.parse.assertErrorsAsStrings(
		'''
		The method or field length is undefined for the type int
		The type of the expression must be an array type but it resolved to int
		'''
		)
	}

	@Test def void testArrayAccessOnMemberFeatureCallAndClone() {
		'''
		// clone has a generic type, so the result is inferred
		int[] a = null;
		int[] cl1 = a.clone(); // OK
		'''.parseAndAssertNoErrors
	}

	@Test def void testArrayAccessOnMemberFeatureCallAndClone2() {
		'''
		// clone has a generic type, so the result is inferred
		int[] a;
		int cl1 = a[0].clone(); // inferred as Object
		'''.parse.assertErrorsAsStrings(
		'''
		The method clone() is not visible
		Type mismatch: cannot convert from Object to int
		'''
		)
	}

	@Test def void testArrayAccessOnMemberFeatureCallAndClone3() {
		'''
		// clone has a generic type, so the result is inferred
		String[] a;
		String cl1 = a[0].clone();
		'''.parse.assertErrorsAsStrings(
		'''
		The method clone() is not visible
		Type mismatch: cannot convert from Object to String
		'''
		)
	}

	@Test def void testArrayAccessOnMemberFeatureCallAndClone4() {
		'''
		// clone has a generic type, so the result is inferred
		String[][] a = null;
		String[] cl1 = a[0].clone();
		'''.parseAndAssertNoErrors
	}

	@Test def void testIntegerCannotBeAssignedToByte() {
		"byte b = 1000;".parse.assertNumberLiteralTypeMismatch("byte", "int")
	}

	@Test def void testIntegerCannotBeAssignedToChar() {
		"char c = 100000;".parse.assertNumberLiteralTypeMismatch("char", "int")
	}

	@Test def void testIntegerCannotBeAssignedToChar2() {
		"char c = -10000;".parse.assertTypeMismatch(XbasePackage.eINSTANCE.XUnaryOperation, "char", "int")
	}

	@Test def void testMaxValueChar() {
		"char c = 65535;".parseAndAssertNoErrors
	}

	@Test def void testIntegerCannotBeAssignedToShort() {
		"short s = 100000;".parse.assertNumberLiteralTypeMismatch("short", "int")
	}

	@Test def void testNegativeCanBeAssignedToShort() {
		// https://github.com/LorenzoBettini/javamm/issues/53
		"short s = -10000;".parseAndAssertNoErrors
	}

	@Test def void testDoubleNegativeCanBeAssignedToShort() {
		// https://github.com/LorenzoBettini/javamm/issues/53
		"short s = -(-10000);".parseAndAssertNoErrors
	}

	@Test def void testNegativePositiveCanBeAssignedToShort() {
		// https://github.com/LorenzoBettini/javamm/issues/53
		"short s = -(+10000);".parseAndAssertNoErrors
	}

	@Test def void testPositiveCanBeAssignedToChar() {
		// https://github.com/LorenzoBettini/javamm/issues/53
		"char c = +10;".parseAndAssertNoErrors
	}

	@Test def void testTypeMismatchAfterCast() {
		'''
		char c = 'c';
		String r2 = (char) (int) c;
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XCastedExpression,
			"String", "char"
		)
	}

	@Test def void testInvalidCast() {
		'''
		System.out.println((String) 0);
		'''.parse.assertErrorsAsStrings("Cannot cast from int or Integer to String")
	}

	@Test def void testWarningUnnecessaryCast() {
		'''
		System.out.println((int) 0);
		'''.parse.assertIssuesAsStrings("Unnecessary cast from int to int")
	}

	@Test def void testTypeMismatchInConditionalExpression() {
		'''
		int i = 0;
		int l = i > 0 ? true : 10;
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBooleanLiteral,
			"int",
			"boolean"
		)
	}

	@Test def void testTypeMismatchInConditionalExpression2() {
		'''
		int i = 0;
		int l = i > 0 ? 10 : true;
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBooleanLiteral,
			"int",
			"boolean"
		)
	}

	@Test def void testTypeMismatchInConditionalExpression3() {
		'''
		int i = 0;
		int l = 10 ? 20 : 30;
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XNumberLiteral,
			"boolean",
			"int"
		)
	}

	@Test def void testTypeMismatchWithGenerics() {
		'''
		java.util.Vector<String> v = new java.util.Vector<Object>();
		System.out.println(v);
		'''.parse.assertIssuesAsStrings("Type mismatch: cannot convert from Vector<Object> to Vector<String>")
	}

	@Test def void testTypeErrorWithWildcardExtends() {
		'''
		java.util.Vector<? extends String> v = new java.util.Vector<String>();
		v.add("s");
		'''.parse.assertIssuesAsStrings("Type mismatch: type String is not applicable at this location")
	}

	@Test def void testTypeErrorWithWildcardSuper() {
		'''
		java.util.Vector<? super String> v = new java.util.Vector<String>();
		String s = v.get(0);
		System.out.println(s);
		'''.parse.assertIssuesAsStrings("Type mismatch: cannot convert from Object to String")
	}

	@Test def void testFinalVariableNotInitialized() {
		'''
		final int i;
		'''.parse.assertError(
			jbasePackage.XJVariableDeclaration,
			IssueCodes.MISSING_INITIALIZATION,
			"Value must be initialized"
		)
	}

	@Test def void testFinalVariableNotInitialized2() {
		'''
		final int i = 0, j;
		'''.parse.assertErrorsAsStrings("Value must be initialized")
	}

	@Test def void testAssignmentToFinalVariable() {
		'''
		{
			final int i = 0;
			i = 1;
			// to avoid warning of unused variable
			System.out.println(i);
		}
		'''.parse.assertIssuesAsStrings("Assignment to final variable")
	}

	@Test def void testAssignmentToFinalArrayVariable() {
		'''
		{
			final int[] i = {0};
			i = {0};
			// to avoid warning of unused variable
			System.out.println(i);
		}
		'''.parse.assertIssuesAsStrings("Assignment to final variable")
	}

	@Test def void testAssignmentToArrayParameterImplicitOk() {
		'''
		args = {"a"};
		'''.parseAndAssertNoIssues
	}

	@Test def void testAssignmentToFinalArrayVariableElementOk() {
		'''
		{
			final int[] i = {0};
			i[0] = 0;
		}
		'''.parseAndAssertNoIssues
	}

	@Test def void testAssignmentToFinalVariableAdditional() {
		'''
		{
			final int i = 0, j = 0;
			j = 1;
		}
		'''.parse.assertErrorsAsStrings("Assignment to final variable")
	}

	@Test def void testAssignmentToFinalParameter() {
		'''
		op m(final int i) : void {
			i = 1;
		}
		'''.parse.assertIssuesAsStrings("Assignment to final parameter")
	}

	@Test def void testAssignmentToFinalParameterInForEachLoop() {
		'''
		java.util.List<String> strings;
		for (final String s : strings)
			s = "a";
		'''.parse.assertIssuesAsStrings("Assignment to final parameter")
	}

	@Test def void testAssignmentToParameter() {
		assignToParam.parseAndAssertNoErrors
	}

	@Test def void testInstanceOfWithoutExplicitCast() {
		'''
		import java.util.Date;
		
		Object d = new Date();
		if (d instanceof Date) {
			System.out.println(((Date)d).getTime());
		}
		'''.parseAndAssertNoIssues
	}

	@Test def void testInstanceOfWithoutExplicitCastError() {
		'''
		import java.util.Date;
		
		Object d = new Date();
		if (d instanceof Date) {
			System.out.println(d.getTime());
		}
		'''.parse.assertErrorsAsStrings("The method getTime() is undefined for the type Object")
	}

	@Test def void testInvalidUseOfVarArgs() {
		val input = '''
		op m(int... i, String... a) : void {
			
		}
		'''
		input.parse.assertError(
			jbasePackage.XJJvmFormalParameter,
			JbaseIssueCodes.INVALID_USE_OF_VAR_ARGS,
			input.indexOf("..."), 3,
			"A vararg must be the last parameter."
		)
	}

	@Test def void testMissingReturnStatementDueToImplicitReturn() {
		// https://github.com/LorenzoBettini/javamm/issues/32
		val input = '''
		op m(int i) : int {
			0;
		}
		'''
		input.parse.assertMissingReturn(XbasePackage.eINSTANCE.XNumberLiteral)
	}

	@Test def void testMissingReturnStatementInEmptyBlock() {
		// https://github.com/LorenzoBettini/javamm/issues/32
		val input = '''
		op m(int i) : int {
		}
		'''
		input.parse.assertMissingReturn(XbasePackage.eINSTANCE.XBlockExpression)
	}

	@Test def void testMissingReturnStatement() {
		// https://github.com/LorenzoBettini/javamm/issues/32
		val input = '''
		op m(int i) : int {
			if (i < 0) {
				return 0;
			}
		}
		'''
		input.parse.assertMissingReturn(XbasePackage.eINSTANCE.XIfExpression)
	}

	@Test def void testReturnStatementInBothIfBranches() {
		val input = '''
		op m(int i) : int {
			if (i < 0) {
				return 0;
			} else {
				return 1;
			}
		}
		'''
		input.parse.assertNoErrors
	}

	@Test def void testIncompatibleOperandTypes_Issue_39() {
		// note that the error is reported only in the workbench,
		// after the generated Java code is compiled.
		// so in this test no error is detected.
		// In the workbench test the error is correctly detected:
		// see jbase.ui.tests.JbaseWorkbenchTest.testErrorInGeneratedJavaCode()
		val input = '''
		int a = 2;
		int b = 2;
		int c = 2;
		if (a==b==c==2) {
			System.out.println("TRUE");
		}
		'''
		input.parse.assertNoErrors
	}

	@Test def void testInvalidThrowExpression() {
		val input = '''
		throw new java.util.Date();
		'''
		input.parse.assertErrorsAsStrings("Type mismatch: cannot convert from Date to Throwable")
	}

	@Test def void testTryCatchWithoutCurlyBrackets() {
		val input = '''
		try
		   int i = 0;
		'''
		input.parse.assertErrorsAsStrings(
			'''
			missing '{' at 'int'
			mismatched input '<EOF>' expecting '}'
			'''
		)
	}

	@Test def void testTryCatchWithWrongCatchOrder() {
		val input = '''
		try {
		   int i = 0;
		} catch (Throwable t) {
			System.out.println("");
		} catch (Exception e) {
			System.out.println("");
		}
		'''
		input.parse.assertErrorsAsStrings(
			'''
			Unreachable code: The catch block can never match. It is already handled by a previous condition.
			'''
		)
	}

	@Test def void testClassObjectWithWrongTypeExpression() {
		val input = '''
		String s;
		System.out.println(s.class);
		'''
		input.parse.assertTypeMismatch(XbasePackage.eINSTANCE.XFeatureCall, "Class", "String")
	}

	@Test def void testClassObjectWithWrongTypeExpression2() {
		val input = '''
		String s;
		System.out.println(s.getClass().class);
		'''
		input.parse.assertError(
			jbasePackage.XJClassObject,
			JbaseIssueCodes.INVALID_CLASS_OBJECT_EXPRESSION,
			input.indexOf("s."), "s.getClass()".length,
			"Expected type before .class"
		)
	}

	@Test def void testClassObjectWrongParameterizedTypeExpression() {
		val input = '''
		Class c1 = Class<String>.class;
		'''
		input.parse.assertError(
			XbasePackage.eINSTANCE.XBinaryOperation,
			Diagnostic.SYNTAX_DIAGNOSTIC,
			"no viable alternative at input '.'"
		)
	}

	@Test def void testClassObject() {
		val input = '''
		System.out.println(String.class);
		'''
		input.parse.assertNoErrors
	}

	@Test def void testClassObjectGetName() {
		val input = '''
		System.out.println(String.class.getName());
		'''
		input.parse.assertNoErrors
	}

	@Test def void testClassObjectPrimitiveType() {
		val input = '''
		System.out.println(int.class.getName());
		'''
		input.parse.assertNoErrors
	}

	@Test def void testClassObjectPrimitiveType2() {
		val input = '''
		Class<Integer> c1 = int.class;
		'''
		input.parse.assertNoErrors
	}

	@Test def void testClassObjectAsArgument() {
		val input = '''
		op m(Class c) : void {}
		m(String.class);
		'''
		input.parse.assertNoErrors
	}

	@Test def void testWrongTypeAsTypeLiteralInVariableDeclaration() {
		val input = '''
		Class c1 = String; // in Xbase this is valid, but not in Java
		'''
		input.parse.assertMissingClassInClassObject()
	}

	@Test def void testWrongTypeAsTypeLiteralInFeatureCall() {
		val input = '''
		op m(Class c) {}
		m(String); // in Xbase this is valid, but not in Java
		'''
		input.parse.assertMissingClassInClassObject()
	}

	@Test def void testWrongTypeAsTypeLiteralInMemberCall() {
		val input = '''
		System.out.println(String); // in Xbase this is valid, but not in Java
		'''
		input.parse.assertMissingClassInClassObject()
	}

	@Test def void testWrongTypeAsTypeLiteralInMemberCall2() {
		val input = '''
		method m(Class c) : void {
			this.m(String); // in Xbase this is valid, but not in Java
		}
		'''
		input.parse.assertMissingClassInClassObject()
	}

	@Test def void testInvalidCharacterConstant() {
		val input = '''
		System.out.println('12');
		'''
		input.parse.assertError(
			jbasePackage.XJCharLiteral,
			JbaseIssueCodes.INVALID_CHARACTER_CONSTANT,
			input.indexOf("'12'"), 4,
			"Invalid character constant"
		)
	}

	@Test def void testVariableNotInitializedInMain() {
		val input = '''
		int i;
		System.out.println(i);
		'''
		input.parse.assertVariableNotInitialized("i")
	}

	@Test def void testVariableNotInitializedInMethod() {
		val input = '''
		op m() : void {
			int i;
			System.out.println(i);
		}
		'''
		input.parse.assertVariableNotInitialized("i")
	}

	@Test def void testInvalidExponentiation_Issue_8() {
		// https://github.com/LorenzoBettini/jbase/issues/8
		'''
		double x = 2 ** 3;
		'''.parse.assertError(
			XbasePackage.eINSTANCE.XBinaryOperation,
			Diagnostic.SYNTAX_DIAGNOSTIC,
			"no viable alternative at input '*'"
		)
	}

	@Test def void testValidMultiplication() {
		'''
		double x = 2 * 3;
		'''.parse.assertNoErrors
	}

	def private assertNumberLiteralTypeMismatch(EObject o, String expectedType, String actualType) {
		o.assertTypeMismatch(XbasePackage.eINSTANCE.XNumberLiteral, expectedType, actualType)
	}

	def private assertTypeMismatch(EObject o, EClass c, String expectedType, String actualType) {
		o.assertError(
			c,
			IssueCodes.INCOMPATIBLE_TYPES,
			'''Type mismatch: cannot convert from «actualType» to «expectedType»'''
		)
	}

	def private assertNoSideEffectError(EObject o, EClass c) {
		o.assertError(
			c,
			IssueCodes.INVALID_INNER_EXPRESSION,
			'''This expression is not allowed in this context, since it doesn't cause any side effects.'''
		)
	}

	def private assertUnreachableExpression(EObject o, EClass c) {
		o.assertError(
			c,
			IssueCodes.UNREACHABLE_CODE,
			'''Unreachable expression.'''
		)
	}

	@Test def void testNoDeadCodeWithAdditionalSemicolons() {
		additionalSemicolons.parseAndAssertNoErrors
	}

	@Test def void testDeadCodeAfterReturnWithAdditionalSemicolons() {
		'''
		op m() : void {
			return;;
		}
		'''.parse.assertUnreachableExpression(jbasePackage.XJSemicolonStatement)
	}

	@Test def void testNoDeadCodeWithBreakInAWhileWithConditionAlwaysTrue() {
		'''
		int d = 1;
		while (1 == 1) {
			d++;
			if (d == 3)
				break;
		}
		d = 0;
		'''.parseAndAssertNoErrors
	}

	@Test def void testDeadCodeWithContinueInAWhileWithConditionAlwaysTrue() {
		'''
		int d = 1;
		while (1 == 1) {
			d++;
			if (d == 3)
				continue;
		}
		d = 0;
		'''.parse.assertUnreachableExpression(jbasePackage.XJSemicolonStatement)
	}

	@Test def void testDeadCodeWithoutInAWhileWithConditionAlwaysTrue() {
		'''
		int d = 1;
		while (1 == 1) {
			d++;
		}
		d = 0;
		'''.parse.assertUnreachableExpression(jbasePackage.XJSemicolonStatement)
	}

	@Test def void testNoDeadCodeWithBreakInADoWhileWithConditionAlwaysTrue() {
		'''
		int d = 1;
		do {
			d++;
			if (d == 3)
				break;
		} while (1 == 1);
		d = 0;
		'''.parseAndAssertNoErrors
	}

	@Test def void testNoDeadCodeWithBreakInSingleIfBranchInADoWhileWithConditionAlwaysTrue() {
		'''
		int d = 1;
		do {
			d++;
			if (d == 3)
				break;
			else
				d = 1;
		} while (1 == 1);
		d = 0;
		'''.parseAndAssertNoErrors
	}

	@Test def void testDeadCodeWithContinueInADoWhileWithConditionAlwaysTrue() {
		'''
		int d = 1;
		do {
			d++;
			if (d == 3)
				continue;
		} while (1 == 1);
		d = 0;
		'''.parse.assertUnreachableExpression(jbasePackage.XJSemicolonStatement)
	}

	@Test def void testNoDeadCodeWithBreakInABasicForLoopWithConditionAlwaysTrue() {
		'''
		int d = 1;
		for (;;) {
			d++;
			if (d == 3)
				break;
		}
		d = 0;
		'''.parseAndAssertNoErrors
	}

	@Test def void testDeadCodeWithContinueInABasicForLoopWithConditionAlwaysTrue() {
		'''
		int d = 1;
		for (;;) {
			d++;
			if (d == 3)
				continue;
		}
		d = 0;
		'''.parse.assertUnreachableExpression(jbasePackage.XJSemicolonStatement)
	}

	@Test def void testInvalidFunctionalType() {
		'''
		()=>int f = null;
		'''.parse.assertError(
			jbasePackage.XJSemicolonStatement,
			Diagnostic.SYNTAX_DIAGNOSTIC,
			"no viable alternative at input ')'"
		)
	}

	@Test def void testNoPrintlnExtensionMethod() {
		'''
		println("Hello");
		'''.parse.assertErrorsAsStrings(
		'''
		The method println(String) is undefined
		'''
		)
	}

	@Test def void testNoAddToCollectionExtensionMethod() {
		'''
		java.util.List<String> strings = new java.util.LinkedList<String>();
		strings += "a";
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XBinaryOperation,
			"List<String>",
			"String"
		)
	}

	@Test def void testValidExplicitAddToCollection() {
		'''
		java.util.List<String> strings = new java.util.LinkedList<String>();
		strings.add("a");
		'''.parse.assertNoErrors
	}

	@Test def void testAnnotations() {
		'''
		import com.google.inject.Inject;
		
		@Inject
		o : Object
		
		method @Inject m() {
			return null;
		}
		'''.parseAndAssertNoErrors
	}

	@Test def void testAnnotationsMissingAttribute() {
		'''
		import jbase.tests.util.ExampleAnnotation;
		
		@ExampleAnnotation
		o : Object
		'''.parse.assertError(
			XAnnotationsPackage.eINSTANCE.XAnnotation,
			IssueCodes.ANNOTATIONS_MISSING_ATTRIBUTE_DEFINITION,
			"The annotation must define the attribute 'value'"
		)
	}

	@Test def void testAnnotationsWrongAttributeType() {
		'''
		import jbase.tests.util.ExampleAnnotation;
		
		@ExampleAnnotation(value = "foo")
		o : Object
		'''.parse.assertTypeMismatch(
			XbasePackage.eINSTANCE.XStringLiteral,
			"Class<? extends ScenarioProcessor>",
			"String"
		)
	}

	@Test def void testAnnotationsNotConstantExpression() {
		'''
		import jbase.tests.util.ExampleAnnotation2;
		
		@ExampleAnnotation2(value = Integer.parseInt("0"))
		o : Object
		'''.parse.assertError(
			XbasePackage.eINSTANCE.XMemberFeatureCall,
			IssueCodes.ANNOTATIONS_ILLEGAL_ATTRIBUTE,
			"The value for an annotation attribute must be a constant expression"
		)
	}

	@Test def void testAnnotationsCorrectAttributeTypeButInvalidTypeLiteral() {
		'''
		import jbase.tests.util.ExampleAnnotation;
		import org.eclipse.xtext.xbase.testing.typesystem.TypeSystemSmokeTester;
		
		@ExampleAnnotation(value = TypeSystemSmokeTester)
		o : Object
		'''.parse.assertError(
			XbasePackage.eINSTANCE.XFeatureCall,
			IssueCodes.ANNOTATIONS_ILLEGAL_ATTRIBUTE,
			"The value for an annotation attribute must be a constant expression"
		)
	}

	@Test def void testAnnotationsCorrectAttributeType() {
		'''
		import jbase.tests.util.ExampleAnnotation;
		import org.eclipse.xtext.xbase.testing.typesystem.TypeSystemSmokeTester;
		
		@ExampleAnnotation(value = TypeSystemSmokeTester.class)
		o : Object
		'''.parse.assertNoErrors
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePair() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3(value = String.class)
		o : Object
		'''.parse.assertNoErrors
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePairAndMultiple() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3(value = {String.class, Integer.class})
		o : Object
		'''.parse.assertNoErrors
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesAndSingleValue() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3(String.class)
		o : Object
		'''.parse.assertNoErrors
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesArrayLiteral() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3({String.class, Integer.class})
		o : Object
		'''.parse.assertNoErrors
	}

	@Test def void testBitwiseOperatorsOnChars() {
		'''
		System.out.println('a' & 'b');
		'''.parse.assertNoErrors
	}

	@Test def void testBitwiseOperatorsOnCharsAndInts() {
		'''
		System.out.println('a' & 1);
		'''.parse.assertNoErrors
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesCommaSeparatedWrong() {
		// this is invalid in Java: explicit array literal is required for multiple values
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3(String.class, Integer.class)
		o : Object
		'''.parse.assertError(
			XAnnotationsPackage.eINSTANCE.XAnnotation,
			Diagnostic.SYNTAX_DIAGNOSTIC,
			"mismatched input ',' expecting ')'"
		)
	}

	@Test def void testJbaseExpressionArgumentFactory_Issue53() {
		// https://github.com/LorenzoBettini/jbase/issues/53
		'''
		toString[0] = 0;
		'''.parse.assertErrorsAsStrings(
		'''
		The type of the expression must be an array type but it resolved to String
		Cannot make a static reference to the non-static method toString from the type __synthetic0
		Invalid number of arguments. The method toString() is not applicable for the arguments (int)
		'''
		)
	}

	@Test def void testTryWithResourcesWithEmptyResources() {
		'''
		try () {
			
		}
		'''.parse.assertError(
			jbasePackage.XJTryWithResourcesStatement,
			JbaseIssueCodes.MISSING_RESOURCES,
			4, 1, // error placed on the '('
			'Syntax error on token "(", Resources expected after this token'
		)
	}

	@Test def void testTryWithResourcesWithMissingSemicolon() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("")
			java.io.FileReader fr2 = new java.io.FileReader("")
		) {
			
		}
		'''.parse => [
			1.assertEquals(validationErrors.size) // the last ; is optional
			assertMissingSemicolon(jbasePackage.XJTryWithResourcesVariableDeclaration)
		]
	}

	@Test def void testTryWithResourcesWithSemicolonOk() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
			java.io.FileReader fr2 = new java.io.FileReader("") // last semicolon optional
		) {
			
		}
		'''.parse.assertNoErrors
	}

	@Test def void testTryWithResourcesWithSemicolonOk2() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
			java.io.FileReader fr2 = new java.io.FileReader(""); // last semicolon OK
		) {
			
		}
		'''.parse.assertNoErrors
	}

	@Test def void testTryWithResourcesWithAdditionalSemicolon() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
			java.io.FileReader fr2 = new java.io.FileReader("");;
		) {
			
		}
		'''.parse.assertErrorsAsStrings("extraneous input ';' expecting ')'")
	}

	@Test def void testTryWithResourcesNotAutoCloseableType() {
		// Note that the type of the declared variable is taken into consideration
		// not the actual type of the initialization expression
		val input = '''
		try (
			Object fr1 = new java.io.FileReader("");
		) {
			
		}
		'''
		input.parse.assertError(
			jbasePackage.XJTryWithResourcesVariableDeclaration,
			JbaseIssueCodes.NOT_AUTO_CLOSEABLE,
			input.indexOf("Object"), 6, // error placed on "Object"
			'The resource type Object does not implement java.lang.AutoCloseable'
		)
	}

	@Test def void testTryWithResourcesAutoCloseableType() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
		) {
			
		}
		'''.parse.assertNoErrors
	}

	@Test def void testTryWithResourcesDeclaredResourcesAvailableInResources() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
			java.io.FileReader fr2 = new java.io.FileReader(fr1.toString());
		) {
			
		}
		'''.parse.assertNoErrors
	}

	@Test def void testTryWithResourcesDeclaredResourcesAvailableInExpression() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
		) {
			System.out.println(fr1);
		}
		'''.parse.assertNoErrors
	}

	@Test def void testTryWithResourcesDeclaredResourcesNotAvailableInCatch() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
		) {
		
		} catch (Exception e) {
			System.out.println(fr1);
		}
		'''.parse.assertErrorsAsStrings(
		'''
		The method or field fr1 is undefined
		'''
		)
	}

	@Test def void testTryWithResourcesDeclaredResourcesNotAvailableInFinally() {
		'''
		try (
			java.io.FileReader fr1 = new java.io.FileReader("");
		) {
		
		} finally {
			System.out.println(fr1);
		}
		'''.parse.assertErrorsAsStrings(
		'''
		The method or field fr1 is undefined
		'''
		)
	}

	@Test def void testTryWithResourcesResourceCreatedInMethod() {
		'''
		op createReader() {
			return new java.io.FileReader("");
		}
		
		try (
			java.io.FileReader fr1 = createReader();
		) {
		
		}'''.parse.assertNoErrors
	}

	@Test def void testComparisonWithNull_Issue72() {
		// Xbase would issue this warning:
		// 'The operator '==' should be replaced by '===' when null is one of the arguments.'
		'''
		Object o = new Object();
		System.out.println(o == null);
		'''.parse.assertNoIssues
	}

	def private assertInvalidContinueStatement(EObject o) {
		o.assertError(
			jbasePackage.XJContinueStatement,
			JbaseIssueCodes.INVALID_BRANCHING_STATEMENT,
			"continue cannot be used outside of a loop"
		)
	}

	def private assertInvalidBreakStatement(EObject o) {
		o.assertError(
			jbasePackage.XJBreakStatement,
			JbaseIssueCodes.INVALID_BRANCHING_STATEMENT,
			"break cannot be used outside of a loop or a switch"
		)
	}

	def private assertErrorsAsStrings(EObject o, CharSequence expected) {
		expected.toString.trim.assertEqualsStrings(
			getValidationErrors(o).map[message].join("\n"))
	}
	
	protected def Iterable<Issue> getValidationErrors(EObject o) {
		o.validate.filter[severity == Severity.ERROR]
	}

	def private assertIssuesAsStrings(EObject o, CharSequence expected) {
		expected.toString.trim.assertEqualsStrings(
			o.validate.map[message].join("\n"))
	}

	def private assertMissingSemicolon(EObject o, EClass c) {
		o.assertError(
			c,
			JbaseIssueCodes.MISSING_SEMICOLON,
			'Syntax error, insert ";" to complete Statement'
		)
	}

	def private assertMissingParentheses(EObject o, EClass c) {
		o.assertMissingParentheses(c, "method call")
	}

	def private assertMissingParentheses(EObject o, EClass c, String messageFinalPart) {
		o.assertError(
			c,
			JbaseIssueCodes.MISSING_PARENTHESES,
			'Syntax error, insert "()" to complete ' + messageFinalPart
		)
	}

	def private assertMissingClassInClassObject(EObject o) {
		o.assertError(
			XbasePackage.Literals.XFEATURE_CALL,
			JbaseIssueCodes.INCOMPLETE_CLASS_OBJECT,
			'Syntax error, insert ".class" to complete Expression'
		)
	}

	/**
	 * Redefined since parse returns an EObject here
	 */
	override protected parseAndAssertNoErrors(CharSequence input) {
		input.parse => [
			assertNoErrors
		]
	}

	/**
	 * Redefined since parse returns an EObject here
	 */
	override protected parseAndAssertNoIssues(CharSequence input) {
		input.parse => [
			assertNoIssues
		]
	}

	def private assertMissingReturn(EObject o, EClass c) {
		o.assertError(
			c,
			JbaseIssueCodes.MISSING_RETURN,
			'Missing return'
		)
	}

	def private assertVariableNotInitialized(EObject o, String name) {
		o.assertError(
			XbasePackage.eINSTANCE.XFeatureCall,
			JbaseIssueCodes.NOT_INITIALIZED_VARIABLE,
			"The local variable " + name + " may not have been initialized"
		)
	}

	def private assertWarningRawType(EObject o) {
		o.assertWarning(
			jbasePackage.XJConstructorCall,
			IssueCodes.RAW_TYPE,
			"ArrayList is a raw type. References to generic type ArrayList<E> should be parameterized"
		)
	}
}