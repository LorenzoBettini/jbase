package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import jbase.jbase.JbasePackage;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import jbase.tests.inputs.JbaseTestlanguageInputs;
import jbase.validation.JbaseIssueCodes;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.diagnostics.Diagnostic;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotationsPackage;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.eclipse.xtext.xtype.XtypePackage;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * For validation tests we use JbaseTestlanguage since we can also use
 * expectations for operation's return type.
 */
@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseValidatorTest extends JbaseAbstractTest {

	/**
	 * Since we use both Jbase and JbaseTestlanguage we specify EObject
	 * as the type returned by ParseHelper
	 */
	@Inject
	protected ParseHelper<EObject> parseHelper;

	@Inject
	private JbaseTestlanguageInputs jbaseTestlanguageInputs;

	private final JbasePackage jbasePackage = JbasePackage.eINSTANCE;

	@Test
	public void testArrayIndexNotIntegerLeft() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			args[true] = 0;
			"""), XbasePackage.eINSTANCE.getXBooleanLiteral(), "int", "boolean");
	}

	@Test
	public void testArrayIndexNotIntegerRight() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			String s = args[true];
			"""), XbasePackage.eINSTANCE.getXBooleanLiteral(), "int", "boolean");
	}

	@Test
	public void testArrayIndexNotIntegerInArrayConstructorCall() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			int[] i = new int[true];
			"""), XbasePackage.eINSTANCE.getXBooleanLiteral(), "int", "boolean");
	}

	@Test
	public void testArrayIndexNotIntegerInMemberFeatureCall() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			int[][] a;
			int l = a[true].length;
			"""), XbasePackage.eINSTANCE.getXBooleanLiteral(), "int", "boolean");
	}

	@Test
	public void testQualifiedInnerCreationRejectsInvalidTypes() throws Exception {
		final var parsed = parseHelper.parse("""
			import jbase.tests.util.QualifiedInnerFixtures.Outer;
			Outer outer = new Outer();
			int number = 1;
			outer.new MissingInner();
			number.new Inner();
			outer.new StaticNested();
			outer.new Marker();
			""");
		assertTrue(validationTestHelper.validate(parsed).stream()
			.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
	}

	@Test
	public void testTypeMismatchInArrayDimensionExpression() throws Exception {
		final var expression = parseHelper.parse("""
			{
				int[] i = new int[] { 1, true, 2};
				int[][] j = new int[][] {{ 1, 2}, { 1, "foo", 2}};
				int[][] j = new int[][] { 1 };
			}
			""");
		assertTypeMismatch(expression, XbasePackage.eINSTANCE.getXBooleanLiteral(), "int",
				"boolean");
		assertTypeMismatch(expression, XbasePackage.eINSTANCE.getXStringLiteral(), "int", "String");
		assertTypeMismatch(expression, XbasePackage.eINSTANCE.getXNumberLiteral(), "int[]", "int");
	}

	@Test
	public void testArrayConstructorSpecifiesNeitherDimensionExpressionNorInitializer()
			throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int[] a = new int[];
			"""), jbasePackage.getXJArrayConstructorCall(),
				JbaseIssueCodes.ARRAY_CONSTRUCTOR_EITHER_DIMENSION_EXPRESSION_OR_INITIALIZER,
				"Constructor must provide either dimension expressions or an array initializer");
	}

	@Test
	public void testArrayConstructorSpecifiesBothDimensionExpressionAndInitializer()
			throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int[] j = new int[1] {};
			"""), jbasePackage.getXJArrayConstructorCall(),
				JbaseIssueCodes.ARRAY_CONSTRUCTOR_BOTH_DIMENSION_EXPRESSION_AND_INITIALIZER,
				"Cannot define dimension expressions when an array initializer is provided");
	}

	@Test
	public void testArrayConstructorSpecifiesDimensionExpressionAfterEmptyDimension()
			throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int[][] j = new int[][0];
			"""), XbasePackage.eINSTANCE.getXNumberLiteral(),
				JbaseIssueCodes.ARRAY_CONSTRUCTOR_DIMENSION_EXPRESSION_AFTER_EMPTY_DIMENSION,
				"Cannot specify an array dimension after an empty dimension");
	}

	@Test
	public void testArrayConstructorSpecifiesDimensionExpressionAfterEmptyDimension2()
			throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int[][][] j = new int[0][][1];
			"""), XbasePackage.eINSTANCE.getXNumberLiteral(),
				JbaseIssueCodes.ARRAY_CONSTRUCTOR_DIMENSION_EXPRESSION_AFTER_EMPTY_DIMENSION,
				"Cannot specify an array dimension after an empty dimension");
	}

	@Test
	public void testDiamondConstructorCallInVarDecl() throws Exception {
		// to avoid unused variable warnings
		validationTestHelper.assertNoIssues(
				parseHelper.parse(jbaseInputs.constructorCallWithDiamondInVarDecl() + """
			System.out.println(list1);
			System.out.println(list2);
			System.out.println(list3);
			"""));
	}

	@Test
	public void testDiamondConstructorCallInVarDeclNestedWildcard() throws Exception {
		// to avoid unused variable warnings
		validationTestHelper.assertNoIssues(
				parseHelper.parse(jbaseInputs.constructorCallWithDiamondInVarDeclNestedWildcard() + """
			System.out.println(list3);
			"""));
	}

	@Test
	public void testDiamondConstructorCallInAssignment() throws Exception {
		// to avoid unused variable warnings
		validationTestHelper.assertNoIssues(
				parseHelper.parse(jbaseInputs.constructorCallWithDiamondInAssignment() + """
			System.out.println(list1);
			System.out.println(list2);
			System.out.println(list3);
			"""));
	}

	@Test
	public void testDiamondConstructorCallInAssignmentNestedWildcard() throws Exception {
		// to avoid unused variable warnings
		validationTestHelper.assertNoIssues(parseHelper
				.parse(jbaseInputs.constructorCallWithDiamondInAssignmentNestedWildcard() + """
			System.out.println(list3);
			"""));
	}

	@Test
	public void testConstructorCallRawType() throws Exception {
		assertWarningRawType(parseHelper.parse("""
			System.out.println(new java.util.ArrayList());
			"""));
	}

	@Test
	public void testConstructorCallRawType2() throws Exception {
		assertWarningRawType(parseHelper.parse("""
			java.util.List<String> s = new java.util.ArrayList();
			"""));
	}

	@Test
	public void testConstructorCallNotRawType() throws Exception {
		validationTestHelper.assertNoIssues(parseHelper.parse("""
			System.out.println(new java.util.ArrayList<>());
			"""));
	}

	@Test
	public void testNotArrayTypeLeft() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int i;
			i[0] = 0;
			"""), jbasePackage.getXJAssignment(), JbaseIssueCodes.NOT_ARRAY_TYPE,
				"The type of the expression must be an array type but it resolved to int");
	}

	@Test
	public void testNotArrayTypeRight() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int i;
			i = i[0];
			"""), jbasePackage.getXJArrayAccessExpression(), JbaseIssueCodes.NOT_ARRAY_TYPE,
				"The type of the expression must be an array type but it resolved to int");
	}

	@Test
	public void testNotMultiArrayTypeRight() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int[] i;
			i = i[0][1];
			"""), jbasePackage.getXJArrayAccessExpression(), JbaseIssueCodes.NOT_ARRAY_TYPE,
				"The type of the expression must be an array type but it resolved to int");
	}

	@Test
	public void testNotMultiArrayTypeLeft() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int[] i;
			i[0][1] = 1;
			"""), jbasePackage.getXJAssignment(), JbaseIssueCodes.NOT_ARRAY_TYPE,
				"The type of the expression must be an array type but it resolved to int");
	}

	@Test
	public void testWrongMultiArrayConstructor() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			int[] i = new int[0][1];
			"""), jbasePackage.getXJArrayConstructorCall(), "int[]", "int[][]");
	}

	@Test
	public void testWrongElementInArrayLiteral() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			int[] i = {0, true};
			"""), XbasePackage.eINSTANCE.getXBooleanLiteral(), "int", "boolean");
	}

	@Test
	public void testStringLiteralAsStatement() throws Exception {
		assertNoSideEffectError(parseHelper.parse("""
			"test";
			"""), XbasePackage.eINSTANCE.getXStringLiteral());
	}

	@Test
	public void testBooleanExpressionAsStatement() throws Exception {
		final var expression = parseHelper.parse("""
			"test" != "a";
			""");
		assertNoSideEffectError(expression, XbasePackage.eINSTANCE.getXBinaryOperation());
		assertErrorsAsStrings(expression,
				"This expression is not allowed in this context, since it doesn't cause any side effects.");
	}

	@Test
	public void testDeadCodeAfterReturn() throws Exception {
		assertUnreachableExpression(parseHelper.parse("""
			void m() {
				return;
				System.out.println("");
			}
			"""), jbasePackage.getXJSemicolonStatement());
	}

	@Test
	public void testDeadCodeInForLoopTranslatedToJavaWhileEarlyExit() throws Exception {
		assertUnreachableExpression(
				parseHelper.parse(jbaseInputs.forLoopTranslatedToJavaWhileEarlyExit()),
				XbasePackage.eINSTANCE.getXBinaryOperation());
	}

	@Test
	public void testInvalidContinue() throws Exception {
		assertInvalidContinueStatement(parseHelper.parse("""
			if (true)
				continue;
			"""));
	}

	@Test
	public void testInvalidBreak() throws Exception {
		assertInvalidBreakStatement(parseHelper.parse("""
			if (true)
				break;
			"""));
	}

	@Test
	public void testBreakToNonEnclosingLabel() throws Exception {
		assertIssueCode(parseHelper.parse("break missing;"),
			JbaseIssueCodes.INVALID_BRANCHING_STATEMENT);
	}

	@Test
	public void testContinueToNonLoopLabel() throws Exception {
		assertIssueCode(parseHelper.parse("""
			scope: {
				continue scope;
			}
			"""), JbaseIssueCodes.INVALID_BRANCHING_STATEMENT);
	}

	@Test
	public void testBranchToLabelOutsideLambda() throws Exception {
		assertIssueCode(parseHelper.parse("""
			target: {
				java.lang.Runnable action = () -> {
					break target;
				};
			}
			"""), JbaseIssueCodes.INVALID_BRANCHING_STATEMENT);
	}

	@Test
	public void testBreakOutOfSwitchExpression() throws Exception {
		assertIssueCode(parseHelper.parse("""
			target: {
				int value = switch (1) {
					default -> {
						break target;
					}
				};
			}
			"""), JbaseIssueCodes.INVALID_SWITCH);
	}

	@Test
	public void testBreakToEnclosingLabeledSwitchStatement() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			target: {
				switch (1) {
					default -> {
						break target;
					}
				}
			}
			"""));
	}

	@Test
	public void testContinueToEnclosingLabeledWhileLoop() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			boolean keepGoing = false;
			outer: while (keepGoing) continue outer;
			"""));
	}

	@Test
	public void testContinueToEnclosingLabeledBasicForLoop() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			outer: for (int i = 0; i < 1; i++) continue outer;
			"""));
	}

	@Test
	public void testContinueToEnclosingLabeledEnhancedForLoop() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			{
				outer: for (var value : new int[] {1}) continue outer;
			}
			"""));
	}

	@Test
	public void testBranchToOuterLabelAcrossInnerLabel() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			outer: {
				inner: {
					break outer;
				}
			}
			"""));
	}

	@Test
	public void testBranchToLabelOutsideAnonymousClass() throws Exception {
		assertIssueCode(parseHelper.parse("""
			target: {
				var action = new Object() {
					method run() {
						break target;
					}
				};
			}
			"""), JbaseIssueCodes.INVALID_BRANCHING_STATEMENT);
	}

	@Test
	public void testInvalidCharAssignment() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			char c = "c";
			"""), XbasePackage.eINSTANCE.getXStringLiteral(), "char", "String");
	}

	@Test
	public void testInvalidStringAssignment() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			String s = 's';
			"""), jbasePackage.getXJCharLiteral(), "String", "char");
	}

	@Test
	public void testInvalidCharLiteralAssignmentToBoolean() throws Exception {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=457779
		assertTypeMismatch(parseHelper.parse("""
			boolean b = 's';
			"""), jbasePackage.getXJCharLiteral(), "boolean", "char");
	}

	@Test
	public void testBooleanLiteralAssignableToBoolean() throws Exception {
		parseAndAssertNoErrors("""
			boolean b = true;
			""");
	}

	@Test
	public void testInvalidSwitchExpression() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			switch (return 0) {
				default:\s
					System.out.println("default");
					break;
			}
			"""), XbasePackage.eINSTANCE.getXSwitchExpression(), Diagnostic.SYNTAX_DIAGNOSTIC,
				"no viable alternative at input 'return'");
	}

	@Test
	public void testInvalidSwitchCaseType() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			String firstArg = args[0];
			char arg = firstArg.toCharArray()[0];
			switch (arg) {
				case "f" :\s
					System.out.println("0");
					break;
				default:\s
					System.out.println("default");
					break;
			}
			"""), XbasePackage.eINSTANCE.getXStringLiteral(), "char", "String");
	}

	@Test
	public void testInvalidSwitchReturnType() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			op move(int p) : int {
				switch (p) {
					case 0: return "2";
					default: return -1;
				}
			}
			"""), XbasePackage.eINSTANCE.getXStringLiteral(), "int", "String");
	}

	@Test
	public void testInvalidSwitchReturnType2() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			op move(int p) : int {
				switch (p) {
					case 0: System.out.println("0"); break;
					default: return -1;
				}
			}
			"""), jbasePackage.getXJBreakStatement(), "int", "void");
	}

	@Test
	public void testInvalidSwitchWithoutDefault() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			op move(int p) : int {
				switch (p) {
					case 0: return 1;
				}
			}
			"""), """
			Missing default branch in the presence of expected type int
			Missing return\
			""");
	}

	@Test
	public void testValidSwitchReturnTypeWithFallback() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			op move(int p) : int {
				switch (p) {
					case 0: System.out.println("0"); // the default is executed
					default: return -1;
				}
			}
			"""));
	}

	@Test
	public void testMissingSemicolonInAssignment() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			int i = 0;
			i = 1
			"""), XbasePackage.eINSTANCE.getXAssignment());
	}

	@Test
	public void testMissingSemicolonInVariableDeclaration() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			int i = 0
			"""), XbasePackage.eINSTANCE.getXVariableDeclaration());
	}

	@Test
	public void testMissingSemicolonInContinue() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			continue
			"""), jbasePackage.getXJContinueStatement());
	}

	@Test
	public void testMissingSemicolonInBreak() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			break
			"""), jbasePackage.getXJBreakStatement());
	}

	@Test
	public void testMissingSemicolonInReturn() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			op m() : void {
				return
			}
			"""), XbasePackage.eINSTANCE.getXReturnExpression());
	}

	@Test
	public void testMissingSemicolonInDoWhile() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			do {
			\t
			} while (true)
			"""), XbasePackage.eINSTANCE.getXDoWhileExpression());
	}

	@Test
	public void testMissingSemicolonInFeatureCall() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			i
			"""), XbasePackage.eINSTANCE.getXFeatureCall());
	}

	@Test
	public void testMissingSemicolonInFeatureCall2() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			i()
			"""), XbasePackage.eINSTANCE.getXFeatureCall());
	}

	@Test
	public void testMissingSemicolonInMemberFeatureCall() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			System.out.println()
			"""), XbasePackage.eINSTANCE.getXMemberFeatureCall());
	}

	@Test
	public void testMissingSemicolonInImport() throws Exception {
		assertMissingSemicolon(parseHelper.parse("""
			import java.util.List
			"""), XtypePackage.eINSTANCE.getXImportDeclaration());
	}

	@Test
	public void testTwoSemicolonsInImportIsOk() throws Exception {
		parseAndAssertNoErrors("""
			import java.util.List;;
			""");
	}

	@Test
	public void testMissingParenthesesForMemberCall() throws Exception {
		assertMissingParentheses(parseHelper.parse("""
			System.out.println;
			"""), XbasePackage.eINSTANCE.getXMemberFeatureCall());
	}

	@Test
	public void testMissingParenthesesForMemberCall2() throws Exception {
		assertMissingParentheses(parseHelper.parse("""
			System.out.println
			"""), XbasePackage.eINSTANCE.getXMemberFeatureCall());
	}

	@Test
	public void testMissingParenthesesForMethodCall() throws Exception {
		assertMissingParentheses(parseHelper.parse("""
			op m() {}
			m;
			"""), XbasePackage.eINSTANCE.getXFeatureCall());
	}

	@Test
	public void testMissingParenthesesForMethodCall2() throws Exception {
		assertMissingParentheses(parseHelper.parse("""
			op m() {}
			m
			"""), XbasePackage.eINSTANCE.getXFeatureCall());
	}

	@Test
	public void testMissingParenthesesForArrayLengthOK() throws Exception {
		parseAndAssertNoErrors("""
			int i = args.length;
			""");
	}

	@Test
	public void testMissingParenthesesForConstructor() throws Exception {
		assertMissingParentheses(parseHelper.parse("""
			new String;
			"""), XbasePackage.eINSTANCE.getXConstructorCall(), "Expression");
	}

	@Test
	public void testInvalidXbaseOperator() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			int i;
			System.out.println(i === 0);
			"""), XbasePackage.eINSTANCE.getXBinaryOperation(), Diagnostic.SYNTAX_DIAGNOSTIC,
				"no viable alternative at input '='");
	}

	@Test
	public void testParams() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			op m(int i, boolean b, String i) : void {}
			"""), "Duplicate local variable i");
	}

	@Test
	public void testWrongVariableDeclarationInSeveralDeclarations() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			int i = 0, j = "a", k = true;
			"""), """
			Type mismatch: cannot convert from String to int
			Type mismatch: cannot convert from boolean to int\
			""");
	}

	@Test
	public void testValidAccessToVariable() throws Exception {
		validationTestHelper.assertNoIssues(parseHelper.parse("""
			int i = 0;
			System.out.println(i);
			"""));
	}

	@Test
	public void testVariableDeclarationsNoUnusedWarningsWhenUsed() throws Exception {
		validationTestHelper.assertNoIssues(parseHelper.parse("""
			{
				int i = 0, j = 0, k = 0;

				System.out.println(i);
				System.out.println(j);
				System.out.println(k);
			}
			"""));
	}

	@Test
	public void testUnusedSeveralVariableDeclarations() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			{
				int i = 0, j = 0, k = 0;
				System.out.println(j);
			}
			"""), """
			The value of the local variable i is not used
			The value of the local variable k is not used
			""");
	}

	@Test
	public void testLocalVariabilesAreUnusedWhenOnlyLeftOperandOfAnAssignmentOperator()
			throws Exception {
		// local variables are used when they are not used as the left operand of an assignment operator.
		assertIssuesAsStrings(parseHelper.parse("""
			{
				int i = 0;
				i = 1;
			}
			"""), """
			The value of the local variable i is not used
			""");
	}

	@Test
	public void testArrayIsUnusedWhenOnlyLeftOperandOfAnAssignmentOperator() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			{
				int[] i;
				i = new int[] { 0 };
			}
			"""), """
			The value of the local variable i is not used
			""");
	}

	@Test
	public void testArrayUsedWhenAssigningToItsElements() throws Exception {
		parseAndAssertNoIssues("""
			{
				int[] i = { 0 };
				i[0] = 1;
			}
			""");
	}

	@Test
	public void testUnusedField() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			i : int
			method move() : void {
				i = 0;
			}
			"""), """
			The value of the property i is not used
			""");
	}

	@Test
	public void testPostfixOnWrongExpression() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			"a"++;
			"""), """
			++ cannot be resolved.
			""");
	}

	@Test
	public void testWrongPostfixOnMethodCall() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			op m() { return 0; }

			int i = m()++;
			"""), """
			The left-hand side of an assignment must be a variable
			""");
	}

	@Test
	public void testPostfixOnArrayAccess() throws Exception {
		parseAndAssertNoIssues("""
			{
				int i = 0;
				int[] a = {1,2,3,4};
				a[i]++;
			}
			""");
	}

	@Test
	public void testWrongPostfixOnNonAbstractFeatureCall() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			double i = 0.1++;
			"""), """
			The left-hand side of an assignment must be a variable
			""");
	}

	@Test
	public void testPrefixOnWrongExpression() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			++"a";
			"""), """
			++ cannot be resolved.
			""");
	}

	@Test
	public void testWrongPrefixOnMethodCall() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			op m() { return 0; }

			int i = ++m();
			"""), """
			The left-hand side of an assignment must be a variable
			""");
	}

	@Test
	public void testArrayAccessOnMemberFeatureCallNotAnArray() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			int a;
			int l = a[0].length;
			"""), """
			The method or field length is undefined for the type int
			The type of the expression must be an array type but it resolved to int
			""");
	}

	@Test
	public void testArrayAccessOnMemberFeatureCallAndClone() throws Exception {
		parseAndAssertNoErrors("""
			// clone has a generic type, so the result is inferred
			int[] a = null;
			int[] cl1 = a.clone(); // OK
			""");
	}

	@Test
	public void testArrayAccessOnMemberFeatureCallAndClone2() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			// clone has a generic type, so the result is inferred
			int[] a;
			int cl1 = a[0].clone(); // inferred as Object
			"""), """
			The method clone() is not visible
			Type mismatch: cannot convert from Object to int
			""");
	}

	@Test
	public void testArrayAccessOnMemberFeatureCallAndClone3() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			// clone has a generic type, so the result is inferred
			String[] a;
			String cl1 = a[0].clone();
			"""), """
			The method clone() is not visible
			Type mismatch: cannot convert from Object to String
			""");
	}

	@Test
	public void testArrayAccessOnMemberFeatureCallAndClone4() throws Exception {
		parseAndAssertNoErrors("""
			// clone has a generic type, so the result is inferred
			String[][] a = null;
			String[] cl1 = a[0].clone();
			""");
	}

	@Test
	public void testIntegerCannotBeAssignedToByte() throws Exception {
		assertNumberLiteralTypeMismatch(parseHelper.parse("byte b = 1000;"), "byte", "int");
	}

	@Test
	public void testIntegerCannotBeAssignedToChar() throws Exception {
		assertNumberLiteralTypeMismatch(parseHelper.parse("char c = 100000;"), "char", "int");
	}

	@Test
	public void testIntegerCannotBeAssignedToChar2() throws Exception {
		assertTypeMismatch(parseHelper.parse("char c = -10000;"),
				XbasePackage.eINSTANCE.getXUnaryOperation(), "char", "int");
	}

	@Test
	public void testMaxValueChar() throws Exception {
		parseAndAssertNoErrors("char c = 65535;");
	}

	@Test
	public void testIntegerCannotBeAssignedToShort() throws Exception {
		assertNumberLiteralTypeMismatch(parseHelper.parse("short s = 100000;"), "short", "int");
	}

	@Test
	public void testNegativeCanBeAssignedToShort() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/53
		parseAndAssertNoErrors("short s = -10000;");
	}

	@Test
	public void testDoubleNegativeCanBeAssignedToShort() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/53
		parseAndAssertNoErrors("short s = -(-10000);");
	}

	@Test
	public void testNegativePositiveCanBeAssignedToShort() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/53
		parseAndAssertNoErrors("short s = -(+10000);");
	}

	@Test
	public void testPositiveCanBeAssignedToChar() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/53
		parseAndAssertNoErrors("char c = +10;");
	}

	@Test
	public void testTypeMismatchAfterCast() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			char c = 'c';
			String r2 = (char) (int) c;
			"""), XbasePackage.eINSTANCE.getXCastedExpression(), "String", "char");
	}

	@Test
	public void testInvalidCast() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			System.out.println((String) 0);
			"""), "Cannot cast from int or Integer to String");
	}

	@Test
	public void testWarningUnnecessaryCast() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			System.out.println((int) 0);
			"""), "Unnecessary cast from int to int");
	}

	@Test
	public void testTypeMismatchInConditionalExpression() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			int i = 0;
			int l = i > 0 ? true : 10;
			"""), XbasePackage.eINSTANCE.getXBooleanLiteral(), "int", "boolean");
	}

	@Test
	public void testTypeMismatchInConditionalExpression2() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			int i = 0;
			int l = i > 0 ? 10 : true;
			"""), XbasePackage.eINSTANCE.getXBooleanLiteral(), "int", "boolean");
	}

	@Test
	public void testTypeMismatchInConditionalExpression3() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			int i = 0;
			int l = 10 ? 20 : 30;
			"""), XbasePackage.eINSTANCE.getXNumberLiteral(), "boolean", "int");
	}

	@Test
	public void testTypeMismatchWithGenerics() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			java.util.Vector<String> v = new java.util.Vector<Object>();
			System.out.println(v);
			"""), "Type mismatch: cannot convert from Vector<Object> to Vector<String>");
	}

	@Test
	public void testTypeErrorWithWildcardExtends() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			java.util.Vector<? extends String> v = new java.util.Vector<String>();
			v.add("s");
			"""), "Type mismatch: type String is not applicable at this location");
	}

	@Test
	public void testTypeErrorWithWildcardSuper() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			java.util.Vector<? super String> v = new java.util.Vector<String>();
			String s = v.get(0);
			System.out.println(s);
			"""), "Type mismatch: cannot convert from Object to String");
	}

	@Test
	public void testUnusedBlankFinalVariableIsAllowed() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			final int i;
			"""), "");
	}

	@Test
	public void testGenericVariableDeclarationStillRequiresInitialization() throws Exception {
		final var model = parseHelper.parse("method test() {}");
		final var block = EcoreUtil2.getAllContentsOfType(model, XBlockExpression.class)
			.getFirst();
		final var variable = XbaseFactory.eINSTANCE.createXVariableDeclaration();
		variable.setName("value");
		variable.setWriteable(false);
		block.getExpressions().add(variable);
		validationTestHelper.assertError(model, XbasePackage.Literals.XVARIABLE_DECLARATION,
			IssueCodes.MISSING_INITIALIZATION, "Value must be initialized");
	}

	@Test
	public void testBlankFinalVariableCanBeAssignedOnce() throws Exception {
		parseAndAssertNoIssues("""
			final int i;
			i = 1;
			System.out.println(i);
			""");
	}

	@Test
	public void testBlankFinalVariableCanBeAssignedInEitherBranch() throws Exception {
		parseAndAssertNoIssues("""
			final int i;
			if (args.length == 0) {
				i = 0;
			} else {
				i = 1;
			}
			System.out.println(i);
			""");
	}

	@Test
	public void testAdditionalBlankFinalVariableCanBeAssignedOnce() throws Exception {
		parseAndAssertNoIssues("""
			final int i = 0, j;
			j = 1;
			System.out.println(i + j);
			""");
	}

	@Test
	public void testBlankFinalVariableCannotBeAssignedTwice() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			final int i;
			i = 0;
			i = 1;
			System.out.println(i);
			"""), XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
			IssueCodes.ASSIGNMENT_TO_FINAL, "Assignment to final variable");
	}

	@Test
	public void testBlankFinalVariableMustBeDefinitelyAssignedBeforeRead() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			final int i;
			if (args.length == 0) {
				i = 0;
			}
			System.out.println(i);
			"""), XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
			JbaseIssueCodes.NOT_INITIALIZED_VARIABLE,
			"The local variable i may not have been initialized");
	}

	@Test
	public void testBlankFinalVariableCannotBeAssignedInLoop() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			final int i;
			while (args.length > 0) {
				i = 1;
			}
			"""), XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
			IssueCodes.ASSIGNMENT_TO_FINAL, "Assignment to final variable");
	}

	@Test
	public void testAssignmentToFinalVariable() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			{
				final int i = 0;
				i = 1;
				// to avoid warning of unused variable
				System.out.println(i);
			}
			"""), "Assignment to final variable");
	}

	@Test
	public void testAssignmentToFinalArrayVariable() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			{
				final int[] i = {0};
				i = {0};
				// to avoid warning of unused variable
				System.out.println(i);
			}
			"""), "Assignment to final variable");
	}

	@Test
	public void testAssignmentToArrayParameterImplicitOk() throws Exception {
		parseAndAssertNoIssues("""
			args = {"a"};
			""");
	}

	@Test
	public void testAssignmentToFinalArrayVariableElementOk() throws Exception {
		parseAndAssertNoIssues("""
			{
				final int[] i = {0};
				i[0] = 0;
			}
			""");
	}

	@Test
	public void testAssignmentToFinalVariableAdditional() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			{
				final int i = 0, j = 0;
				j = 1;
			}
			"""), "Assignment to final variable");
	}

	@Test
	public void testAssignmentToFinalParameter() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			op m(final int i) : void {
				i = 1;
			}
			"""), "Assignment to final parameter");
	}

	@Test
	public void testAssignmentToFinalParameterInForEachLoop() throws Exception {
		assertIssuesAsStrings(parseHelper.parse("""
			java.util.List<String> strings = null;
			for (final String s : strings)
				s = "a";
			"""), "Assignment to final parameter");
	}

	@Test
	public void testAssignmentToParameter() throws Exception {
		parseAndAssertNoErrors(jbaseTestlanguageInputs.assignToParam());
	}

	@Test
	public void testInstanceOfWithoutExplicitCast() throws Exception {
		parseAndAssertNoIssues("""
			import java.util.Date;

			Object d = new Date();
			if (d instanceof Date) {
				System.out.println(((Date)d).getTime());
			}
			""");
	}

	@Test
	public void testInstanceOfWithoutExplicitCastError() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			import java.util.Date;

			Object d = new Date();
			if (d instanceof Date) {
				System.out.println(d.getTime());
			}
			"""), "The method getTime() is undefined for the type Object");
	}

	@Test
	public void testVarArgsAsOnlyParameter() throws Exception {
		parseAndAssertNoIssues("""
			op m(int... i) : void {
			}
			""");
	}

	@Test
	public void testVarArgsAsLastParameter() throws Exception {
		parseAndAssertNoIssues("""
			op m(int i, String... a) : void {
			}
			""");
	}

	@Test
	public void testInvalidUseOfVarArgs() throws Exception {
		final var input = """
			op m(int... i, String... a) : void {
			\t
			}
			""";
		validationTestHelper.assertError(parseHelper.parse(input),
				jbasePackage.getXJJvmFormalParameter(), JbaseIssueCodes.INVALID_USE_OF_VAR_ARGS,
				input.indexOf("..."), 3, "A vararg must be the last parameter.");
	}

	@Test
	public void testMissingReturnStatementDueToImplicitReturn() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/32
		final var input = """
			op m(int i) : int {
				0;
			}
			""";
		assertMissingReturn(parseHelper.parse(input), XbasePackage.eINSTANCE.getXNumberLiteral());
	}

	@Test
	public void testMissingReturnStatementInEmptyBlock() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/32
		final var input = """
			op m(int i) : int {
			}
			""";
		assertMissingReturn(parseHelper.parse(input), XbasePackage.eINSTANCE.getXBlockExpression());
	}

	@Test
	public void testMissingReturnStatement() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/32
		final var input = """
			op m(int i) : int {
				if (i < 0) {
					return 0;
				}
			}
			""";
		assertMissingReturn(parseHelper.parse(input), XbasePackage.eINSTANCE.getXIfExpression());
	}

	@Test
	public void testReturnStatementInBothIfBranches() throws Exception {
		final var input = """
			op m(int i) : int {
				if (i < 0) {
					return 0;
				} else {
					return 1;
				}
			}
			""";
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	@Test
	public void testIncompatibleOperandTypes_Issue_39() throws Exception {
		// note that the error is reported only in the workbench,
		// after the generated Java code is compiled.
		// so in this test no error is detected.
		// In the workbench test the error is correctly detected:
		// see jbase.ui.tests.JbaseWorkbenchTest.testErrorInGeneratedJavaCode()
		final var input = """
			int a = 2;
			int b = 2;
			int c = 2;
			if (a==b==c==2) {
				System.out.println("TRUE");
			}
			""";
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	@Test
	public void testInvalidThrowExpression() throws Exception {
		final var input = """
			throw new java.util.Date();
			""";
		assertErrorsAsStrings(parseHelper.parse(input),
				"Type mismatch: cannot convert from Date to Throwable");
	}

	@Test
	public void testTryCatchWithoutCurlyBrackets() throws Exception {
		final var input = """
			try
			   int i = 0;
			""";
		assertErrorsAsStrings(parseHelper.parse(input), """
			missing '{' at 'int'
			mismatched input '<EOF>' expecting '}'
			""");
	}

	@Test
	public void testTryCatchWithWrongCatchOrder() throws Exception {
		final var input = """
			try {
			   int i = 0;
			} catch (Throwable t) {
				System.out.println("");
			} catch (Exception e) {
				System.out.println("");
			}
			""";
		assertErrorsAsStrings(parseHelper.parse(input), """
			Unreachable code: The catch block can never match. It is already handled by a previous condition.
			""");
	}

	@Test
	public void testClassObjectWithWrongTypeExpression() throws Exception {
		final var input = """
			String s;
			System.out.println(s.class);
			""";
		assertTypeMismatch(parseHelper.parse(input), XbasePackage.eINSTANCE.getXFeatureCall(),
				"Class", "String");
	}

	@Test
	public void testClassObjectWithWrongTypeExpression2() throws Exception {
		final var input = """
			String s;
			System.out.println(s.getClass().class);
			""";
		validationTestHelper.assertError(parseHelper.parse(input), jbasePackage.getXJClassObject(),
				JbaseIssueCodes.INVALID_CLASS_OBJECT_EXPRESSION, input.indexOf("s."),
				"s.getClass()".length(), "Expected type before .class");
	}

	@Test
	public void testClassObjectWrongParameterizedTypeExpression() throws Exception {
		// JLS 15.8.2 does not allow type arguments in class literals: Class.class is valid,
		// but Class<String>.class is invalid Java syntax, so we expect a syntax diagnostic.
		final var input = """
			Class c1 = Class<String>.class;
			""";
		final var model = parseHelper.parse(input);
		validationTestHelper.assertError(model, model.eClass(), Diagnostic.SYNTAX_DIAGNOSTIC,
				"missing EOF at '>'");
	}

	@Test
	public void testClassObject() throws Exception {
		final var input = """
			System.out.println(String.class);
			""";
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	@Test
	public void testClassObjectGetName() throws Exception {
		final var input = """
			System.out.println(String.class.getName());
			""";
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	@Test
	public void testClassObjectPrimitiveType() throws Exception {
		final var input = """
			System.out.println(int.class.getName());
			""";
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	@Test
	public void testClassObjectPrimitiveType2() throws Exception {
		final var input = """
			Class<Integer> c1 = int.class;
			""";
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	@Test
	public void testClassObjectAsArgument() throws Exception {
		final var input = """
			op m(Class c) : void {}
			m(String.class);
			""";
		validationTestHelper.assertNoErrors(parseHelper.parse(input));
	}

	@Test
	public void testWrongTypeAsTypeLiteralInVariableDeclaration() throws Exception {
		final var input = """
			Class c1 = String; // in Xbase this is valid, but not in Java
			""";
		assertMissingClassInClassObject(parseHelper.parse(input));
	}

	@Test
	public void testWrongTypeAsTypeLiteralInFeatureCall() throws Exception {
		final var input = """
			op m(Class c) {}
			m(String); // in Xbase this is valid, but not in Java
			""";
		assertMissingClassInClassObject(parseHelper.parse(input));
	}

	@Test
	public void testWrongTypeAsTypeLiteralInMemberCall() throws Exception {
		final var input = """
			System.out.println(String); // in Xbase this is valid, but not in Java
			""";
		assertMissingClassInClassObject(parseHelper.parse(input));
	}

	@Test
	public void testWrongTypeAsTypeLiteralInMemberCall2() throws Exception {
		final var input = """
			method m(Class c) : void {
				this.m(String); // in Xbase this is valid, but not in Java
			}
			""";
		assertMissingClassInClassObject(parseHelper.parse(input));
	}

	@Test
	public void testInvalidCharacterConstant() throws Exception {
		final var input = """
			System.out.println('12');
			""";
		validationTestHelper.assertError(parseHelper.parse(input), jbasePackage.getXJCharLiteral(),
				JbaseIssueCodes.INVALID_CHARACTER_CONSTANT, input.indexOf("'12'"), 4,
				"Invalid character constant");
	}

	@Test
	public void testVariableNotInitializedInMain() throws Exception {
		final var input = """
			int i;
			System.out.println(i);
			""";
		assertVariableNotInitialized(parseHelper.parse(input), "i");
	}

	@Test
	public void testVariableNotInitializedInMethod() throws Exception {
		final var input = """
			op m() : void {
				int i;
				System.out.println(i);
			}
			""";
		assertVariableNotInitialized(parseHelper.parse(input), "i");
	}

	@Test
	public void testInvalidExponentiation_Issue_8() throws Exception {
		// https://github.com/LorenzoBettini/jbase/issues/8
		validationTestHelper.assertError(parseHelper.parse("""
			double x = 2 ** 3;
			"""), XbasePackage.eINSTANCE.getXBinaryOperation(), Diagnostic.SYNTAX_DIAGNOSTIC,
				"no viable alternative at input '*'");
	}

	@Test
	public void testValidMultiplication() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			double x = 2 * 3;
			"""));
	}

	private void assertNumberLiteralTypeMismatch(final EObject o, final String expectedType,
			final String actualType) {
		assertTypeMismatch(o, XbasePackage.eINSTANCE.getXNumberLiteral(), expectedType, actualType);
	}

	private void assertTypeMismatch(final EObject object, final EClass type,
			final String expectedType, final String actualType) {
		validationTestHelper.assertError(object, type, IssueCodes.INCOMPATIBLE_TYPES,
				"Type mismatch: cannot convert from " + actualType + " to " + expectedType);
	}

	private void assertNoSideEffectError(final EObject o, final EClass c) {
		validationTestHelper.assertError(o, c, IssueCodes.INVALID_INNER_EXPRESSION,
				"This expression is not allowed in this context, since it doesn't cause any side effects.");
	}

	private void assertUnreachableExpression(final EObject o, final EClass c) {
		validationTestHelper.assertError(o, c, IssueCodes.UNREACHABLE_CODE,
				"Unreachable expression.");
	}

	@Test
	public void testNoDeadCodeWithAdditionalSemicolons() throws Exception {
		parseAndAssertNoErrors(jbaseTestlanguageInputs.additionalSemicolons());
	}

	@Test
	public void testDeadCodeAfterReturnWithAdditionalSemicolons() throws Exception {
		assertUnreachableExpression(parseHelper.parse("""
			op m() : void {
				return;;
			}
			"""), jbasePackage.getXJSemicolonStatement());
	}

	@Test
	public void testNoDeadCodeWithBreakInAWhileWithConditionAlwaysTrue() throws Exception {
		parseAndAssertNoErrors("""
			int d = 1;
			while (1 == 1) {
				d++;
				if (d == 3)
					break;
			}
			d = 0;
			""");
	}

	@Test
	public void testDeadCodeWithContinueInAWhileWithConditionAlwaysTrue() throws Exception {
		assertUnreachableExpression(parseHelper.parse("""
			int d = 1;
			while (1 == 1) {
				d++;
				if (d == 3)
					continue;
			}
			d = 0;
			"""), jbasePackage.getXJSemicolonStatement());
	}

	@Test
	public void testDeadCodeWithoutInAWhileWithConditionAlwaysTrue() throws Exception {
		assertUnreachableExpression(parseHelper.parse("""
			int d = 1;
			while (1 == 1) {
				d++;
			}
			d = 0;
			"""), jbasePackage.getXJSemicolonStatement());
	}

	@Test
	public void testNoDeadCodeWithBreakInADoWhileWithConditionAlwaysTrue() throws Exception {
		parseAndAssertNoErrors("""
			int d = 1;
			do {
				d++;
				if (d == 3)
					break;
			} while (1 == 1);
			d = 0;
			""");
	}

	@Test
	public void testNoDeadCodeWithBreakInSingleIfBranchInADoWhileWithConditionAlwaysTrue()
			throws Exception {
		parseAndAssertNoErrors("""
			int d = 1;
			do {
				d++;
				if (d == 3)
					break;
				else
					d = 1;
			} while (1 == 1);
			d = 0;
			""");
	}

	@Test
	public void testDeadCodeWithContinueInADoWhileWithConditionAlwaysTrue() throws Exception {
		assertUnreachableExpression(parseHelper.parse("""
			int d = 1;
			do {
				d++;
				if (d == 3)
					continue;
			} while (1 == 1);
			d = 0;
			"""), jbasePackage.getXJSemicolonStatement());
	}

	@Test
	public void testNoDeadCodeWithBreakInABasicForLoopWithConditionAlwaysTrue() throws Exception {
		parseAndAssertNoErrors("""
			int d = 1;
			for (;;) {
				d++;
				if (d == 3)
					break;
			}
			d = 0;
			""");
	}

	@Test
	public void testDeadCodeWithContinueInABasicForLoopWithConditionAlwaysTrue() throws Exception {
		assertUnreachableExpression(parseHelper.parse("""
			int d = 1;
			for (;;) {
				d++;
				if (d == 3)
					continue;
			}
			d = 0;
			"""), jbasePackage.getXJSemicolonStatement());
	}

	@Test
	public void testInvalidFunctionalType() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			()=>int f = null;
			"""), jbasePackage.getXJSemicolonStatement(), Diagnostic.SYNTAX_DIAGNOSTIC,
				"no viable alternative at input ')'");
	}

	@Test
	public void testNoPrintlnExtensionMethod() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			println("Hello");
			"""), """
			The method println(String) is undefined
			""");
	}

	@Test
	public void testNoAddToCollectionExtensionMethod() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			java.util.List<String> strings = new java.util.LinkedList<String>();
			strings += "a";
			"""), XbasePackage.eINSTANCE.getXBinaryOperation(), "List<String>", "String");
	}

	@Test
	public void testValidExplicitAddToCollection() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			java.util.List<String> strings = new java.util.LinkedList<String>();
			strings.add("a");
			"""));
	}

	@Test
	public void testAnnotations() throws Exception {
		parseAndAssertNoErrors("""
			import com.google.inject.Inject;

			@Inject
			o : Object

			method @Inject m() {
				return null;
			}
			""");
	}

	@Test
	public void testAnnotationsMissingAttribute() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation;

			@ExampleAnnotation
			o : Object
			"""), XAnnotationsPackage.eINSTANCE.getXAnnotation(),
				IssueCodes.ANNOTATIONS_MISSING_ATTRIBUTE_DEFINITION,
				"The annotation must define the attribute 'value'");
	}

	@Test
	public void testAnnotationsWrongAttributeType() throws Exception {
		assertTypeMismatch(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation;

			@ExampleAnnotation(value = "foo")
			o : Object
			"""), XbasePackage.eINSTANCE.getXStringLiteral(), "Class<? extends ScenarioProcessor>",
				"String");
	}

	@Test
	public void testAnnotationsNotConstantExpression() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation2;

			@ExampleAnnotation2(value = Integer.parseInt("0"))
			o : Object
			"""), XbasePackage.eINSTANCE.getXMemberFeatureCall(),
				IssueCodes.ANNOTATIONS_ILLEGAL_ATTRIBUTE,
				"The value for an annotation attribute must be a constant expression");
	}

	@Test
	public void testAnnotationsCorrectAttributeTypeButInvalidTypeLiteral() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation;
			import org.eclipse.xtext.xbase.testing.typesystem.TypeSystemSmokeTester;

			@ExampleAnnotation(value = TypeSystemSmokeTester)
			o : Object
			"""), XbasePackage.eINSTANCE.getXFeatureCall(),
				IssueCodes.ANNOTATIONS_ILLEGAL_ATTRIBUTE,
				"The value for an annotation attribute must be a constant expression");
	}

	@Test
	public void testAnnotationsCorrectAttributeType() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation;
			import org.eclipse.xtext.xbase.testing.typesystem.TypeSystemSmokeTester;

			@ExampleAnnotation(value = TypeSystemSmokeTester.class)
			o : Object
			"""));
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePair() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3(value = String.class)
			o : Object
			"""));
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePairAndMultiple()
			throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3(value = {String.class, Integer.class})
			o : Object
			"""));
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesAndSingleValue() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3(String.class)
			o : Object
			"""));
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesArrayLiteral() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3({String.class, Integer.class})
			o : Object
			"""));
	}

	@Test
	public void testBitwiseOperatorsOnChars() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			System.out.println('a' & 'b');
			"""));
	}

	@Test
	public void testBitwiseOperatorsOnCharsAndInts() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			System.out.println('a' & 1);
			"""));
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesCommaSeparatedWrong() throws Exception {
		// this is invalid in Java: explicit array literal is required for multiple values
		validationTestHelper.assertError(parseHelper.parse("""
			import jbase.tests.util.ExampleAnnotation3;

			method annotated(@ExampleAnnotation3(String.class, Integer.class) Object o) {}
			"""), jbasePackage.getXJParameterizedTypeReference(),
				Diagnostic.SYNTAX_DIAGNOSTIC,
				"mismatched input ',' expecting ')'");
	}

	@Test
	public void testJbaseExpressionArgumentFactory_Issue53() throws Exception {
		// https://github.com/LorenzoBettini/jbase/issues/53
		assertErrorsAsStrings(parseHelper.parse("""
			toString[0] = 0;
			"""), """
			The type of the expression must be an array type but it resolved to String
			Cannot make a static reference to the non-static method toString from the type __synthetic0
			Invalid number of arguments. The method toString() is not applicable for the arguments (int)
			""");
	}

	@Test
	public void testTryWithResourcesWithEmptyResources() throws Exception {
		// error placed on the '('
		validationTestHelper.assertError(parseHelper.parse("""
			try () {
			\t
			}
			"""), jbasePackage.getXJTryWithResourcesStatement(), JbaseIssueCodes.MISSING_RESOURCES,
				4, 1, "Syntax error on token \"(\", Resources expected after this token");
	}

	@Test
	public void testTryWithResourcesWithMissingSemicolon() throws Exception {
		// the last ; is optional
		final var expression = parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("")
				java.io.FileReader fr2 = new java.io.FileReader("")
			) {
			\t
			}
			""");
		assertEquals(1, getValidationErrors(expression).size());
		assertMissingSemicolon(expression, jbasePackage.getXJTryWithResourcesVariableDeclaration());
	}

	@Test
	public void testTryWithExistingResourceWithMissingSemicolon() throws Exception {
		final var expression = parseHelper.parse("""
			var input = new java.io.StringReader("");
			try (
				input
				java.io.StringReader fr2 = new java.io.StringReader("")
			) {
			\t
			}
			""");
		assertEquals(1, getValidationErrors(expression).size());
		assertMissingSemicolon(expression,
			jbasePackage.getXJTryWithResourcesExistingResource());
	}

	@Test
	public void testTryWithResourcesWithSemicolonOk() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
				java.io.FileReader fr2 = new java.io.FileReader("") // last semicolon optional
			) {
			\t
			}
			"""));
	}

	@Test
	public void testTryWithResourcesWithSemicolonOk2() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
				java.io.FileReader fr2 = new java.io.FileReader(""); // last semicolon OK
			) {
			\t
			}
			"""));
	}

	@Test
	public void testTryWithResourcesWithAdditionalSemicolon() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
				java.io.FileReader fr2 = new java.io.FileReader("");;
			) {
			\t
			}
			"""), "extraneous input ';' expecting ')'");
	}

	@Test
	public void testTryWithResourcesNotAutoCloseableType() throws Exception {
		// Note that the type of the declared variable is taken into consideration
		// not the actual type of the initialization expression
		// error placed on "Object"
		final var input = """
			try (
				Object fr1 = new java.io.FileReader("");
			) {
			\t
			}
			""";
		validationTestHelper.assertError(parseHelper.parse(input),
				jbasePackage.getXJTryWithResourcesVariableDeclaration(),
				JbaseIssueCodes.NOT_AUTO_CLOSEABLE, input.indexOf("Object"), 6,
				"The resource type Object does not implement java.lang.AutoCloseable");
	}

	@Test
	public void testTryWithResourcesAutoCloseableType() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
			) {
			\t
			}
			"""));
	}

	@Test
	public void testTryWithResourcesDeclaredResourcesAvailableInResources() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
				java.io.FileReader fr2 = new java.io.FileReader(fr1.toString());
			) {
			\t
			}
			"""));
	}

	@Test
	public void testTryWithResourcesDeclaredResourcesAvailableInExpression() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
			) {
				System.out.println(fr1);
			}
			"""));
	}

	@Test
	public void testTryWithResourcesDeclaredResourcesNotAvailableInCatch() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
			) {

			} catch (Exception e) {
				System.out.println(fr1);
			}
			"""), """
			The method or field fr1 is undefined
			""");
	}

	@Test
	public void testTryWithResourcesDeclaredResourcesNotAvailableInFinally() throws Exception {
		assertErrorsAsStrings(parseHelper.parse("""
			try (
				java.io.FileReader fr1 = new java.io.FileReader("");
			) {

			} finally {
				System.out.println(fr1);
			}
			"""), """
			The method or field fr1 is undefined
			""");
	}

	@Test
	public void testTryWithResourcesResourceCreatedInMethod() throws Exception {
		validationTestHelper.assertNoErrors(parseHelper.parse("""
			op createReader() {
				return new java.io.FileReader("");
			}

			try (
				java.io.FileReader fr1 = createReader();
			) {

			}\
			"""));
	}

	@Test
	public void testComparisonWithNull_Issue72() throws Exception {
		// Xbase would issue this warning:
		// 'The operator '==' should be replaced by '===' when null is one of the arguments.'
		validationTestHelper.assertNoIssues(parseHelper.parse("""
			Object o = new Object();
			System.out.println(o == null);
			"""));
	}

	private void assertInvalidContinueStatement(final EObject o) {
		validationTestHelper.assertError(o, jbasePackage.getXJContinueStatement(),
				JbaseIssueCodes.INVALID_BRANCHING_STATEMENT,
				"continue cannot be used outside of a loop");
	}

	private void assertIssueCode(final EObject object, final String code) {
		final var issues = validationTestHelper.validate(object);
		assertTrue("Expected issue " + code + " in " + issues,
			issues.stream().anyMatch(issue -> code.equals(issue.getCode())));
	}

	private void assertInvalidBreakStatement(final EObject o) {
		validationTestHelper.assertError(o, jbasePackage.getXJBreakStatement(),
				JbaseIssueCodes.INVALID_BRANCHING_STATEMENT,
				"break cannot be used outside of a loop or a switch");
	}

	private void assertErrorsAsStrings(final EObject object, final CharSequence expected) {
		assertEqualsStrings(expected.toString().trim(), getValidationErrors(object).stream()
				.map(Issue::getMessage).collect(Collectors.joining("\n")));
	}

	protected List<Issue> getValidationErrors(final EObject object) {
		return validationTestHelper.validate(object).stream()
				.filter(issue -> issue.getSeverity() == Severity.ERROR).toList();
	}

	private void assertIssuesAsStrings(final EObject object, final CharSequence expected) {
		assertEqualsStrings(expected.toString().trim(), validationTestHelper.validate(object)
				.stream().map(Issue::getMessage).collect(Collectors.joining("\n")));
	}

	private void assertMissingSemicolon(final EObject o, final EClass c) {
		validationTestHelper.assertError(o, c, JbaseIssueCodes.MISSING_SEMICOLON,
				"Syntax error, insert \";\" to complete Statement");
	}

	private void assertMissingParentheses(final EObject o, final EClass c) {
		assertMissingParentheses(o, c, "method call");
	}

	private void assertMissingParentheses(final EObject o, final EClass c,
			final String messageFinalPart) {
		validationTestHelper.assertError(o, c, JbaseIssueCodes.MISSING_PARENTHESES,
				"Syntax error, insert \"()\" to complete " + messageFinalPart);
	}

	private void assertMissingClassInClassObject(final EObject o) {
		validationTestHelper.assertError(o, XbasePackage.Literals.XFEATURE_CALL,
				JbaseIssueCodes.INCOMPLETE_CLASS_OBJECT,
				"Syntax error, insert \".class\" to complete Expression");
	}

	/**
	 * Redefined since parse returns an EObject here
	 */
	@Override
	protected void parseAndAssertNoErrors(final CharSequence input) throws Exception {
		final var expression = parseHelper.parse(input);
		validationTestHelper.assertNoErrors(expression);
	}

	/**
	 * Redefined since parse returns an EObject here
	 */
	@Override
	protected void parseAndAssertNoIssues(final CharSequence input) throws Exception {
		final var expression = parseHelper.parse(input);
		validationTestHelper.assertNoIssues(expression);
	}

	private void assertMissingReturn(final EObject o, final EClass c) {
		validationTestHelper.assertError(o, c, JbaseIssueCodes.MISSING_RETURN, "Missing return");
	}

	private void assertVariableNotInitialized(final EObject o, final String name) {
		validationTestHelper.assertError(o, XbasePackage.eINSTANCE.getXFeatureCall(),
				JbaseIssueCodes.NOT_INITIALIZED_VARIABLE,
				"The local variable " + name + " may not have been initialized");
	}

	private void assertWarningRawType(final EObject o) {
		validationTestHelper.assertWarning(o, jbasePackage.getXJConstructorCall(),
				IssueCodes.RAW_TYPE,
				"ArrayList is a raw type. References to generic type ArrayList<E> should be parameterized");
	}
}
