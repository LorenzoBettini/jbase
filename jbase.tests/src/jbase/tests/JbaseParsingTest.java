package jbase.tests;

import static org.junit.Assert.*;
import jbase.jbase.XJCharLiteral;
import jbase.jbase.XJClassObject;
import jbase.jbase.XJPrefixOperation;
import jbase.jbase.XJTryWithResourcesExistingResource;
import jbase.jbase.XJTryWithResourcesStatement;
import jbase.util.JbaseNodeModelUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.common.types.JvmSynonymTypeReference;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XListLiteral;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XPostfixOperation;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XSynchronizedExpression;
import org.eclipse.xtext.xbase.XThrowExpression;
import org.eclipse.xtext.xbase.XTryCatchFinallyExpression;
import org.eclipse.xtext.xbase.XbasePackage;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseParsingTest extends JbaseAbstractTest {

	@Test
	public void testAnonymousInterfaceRequiresAnInferrer() throws Exception {
		final var model = parseHelper.parse("var value = new Runnable() {};");
		assertTrue(validationTestHelper.validate(model).stream()
			.anyMatch(issue -> issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR));
	}

	@Test
	public void testAnonymousCreationOfPrimitiveIsRejected() throws Exception {
		final var model = parseHelper.parse("var value = new int() {};");
		assertTrue(validationTestHelper.validate(model).stream()
			.anyMatch(issue -> issue.getSeverity() == org.eclipse.xtext.diagnostics.Severity.ERROR));
	}

	@Test
	public void testDefaultAnonymousBodyIsEmpty() throws Exception {
		final var model = parseHelper.parse("var value = new Object() {};");
		assertTrue(model.eResource().getErrors().isEmpty());
		assertLastExpression("var value = new Object() {};", expression -> {
			final var anonymous = (jbase.jbase.XJAnonymousClass) getVariableDeclarationRight(expression);
			assertEquals(jbase.jbase.JbasePackage.Literals.XJ_ANONYMOUS_CLASS_BODY,
				anonymous.getBody().eClass());
		});
		final var invalid = parseHelper.parse("var value = new Object() { method get() {} };");
		assertFalse(invalid.eResource().getErrors().isEmpty());
	}

	@Test
	public void testQualifiedInnerClassCreation() throws Exception {
		assertLastExpression("var value = outer.new Inner();", expression -> {
			final var creation = (jbase.jbase.XJConstructorCall)
				getVariableDeclarationRight(expression);
			assertTrue(creation.getQualifier() instanceof XFeatureCall);
		});
	}

	@Test
	public void testAssignmentLeft() throws Exception {
		assertLastExpression("""
			i = 1;
			""", expression -> getAssignment(expression).getFeature());
	}

	@Test
	public void testAssignmentRight() throws Exception {
		assertLastExpression("""
			int i;
			i = m();
			""", expression -> getAssignment(expression).getValue());
	}

	@Test
	public void testAssignmentRight2() throws Exception {
		assertLastExpression("""
			int j;
			int i;
			i = j;
			""", expression -> getAssignment(expression).getValue());
	}

	@Test
	public void testAssignmentIndex() throws Exception {
		assertLastExpression("""
			i[0] = 1;
			""", expression -> assertTrue(
				getJAssignment(expression).getIndexes().getFirst() instanceof XNumberLiteral));
	}

	//	@Test def void testFeatureCallIndex() {
	//		'''
	//		int[] m() { return null; }
	//		m()[0] = 1;
	//		'''.parse => [
	//			assertTrue((main.expressions.head as JavammXAssignment).index instanceof XNumberLiteral)
	//		]
	//	}
	@Test
	public void testFeatureCallIndex2() throws Exception {
		assertLastExpression("""
			i = m()[0];
			""", expression -> assertTrue(getArrayAccess(getAssignment(expression).getValue())
				.getIndexes().getFirst() instanceof XNumberLiteral));
	}

	@Test
	public void testArrayAccessInParenthesizedExpression() throws Exception {
		assertLastExpression(jbaseInputs.arrayAccessInParenthesizedExpression(),
				expression -> assertTrue(getArrayAccess(getAssignment(expression).getValue())
						.getIndexes().getFirst() instanceof XNumberLiteral));
	}

	@Test
	public void testMultiArrayAccessInRightHandsideExpression() throws Exception {
		assertLastExpression(jbaseInputs.multiArrayAccessInRightHandsideExpression(),
				expression -> {
					final var indexes = getArrayAccess(getAssignment(expression).getValue())
							.getIndexes();
					assertTrue(indexes.getFirst() instanceof XNumberLiteral);
					assertTrue(indexes.getLast() instanceof XBinaryOperation);
				});
	}

	@Test
	public void testMultiArrayAccessInLeftHandsideExpression() throws Exception {
		assertLastExpression("""
			int[][] a;
			a[0][1+2] = 1;
			""", expression -> {
			final var indexes = getJAssignment(expression).getIndexes();
			assertTrue(indexes.getFirst() instanceof XNumberLiteral);
			assertTrue(indexes.getLast() instanceof XBinaryOperation);
		});
	}

	@Test
	public void testSystemOut() throws Exception {
		assertLastExpression("""
			System.out;
			""", expression -> getMemberFeatureCall(expression).getMemberCallTarget());
	}

	@Test
	public void testSystemOutPrintln() throws Exception {
		assertLastExpression("""
			System.out.println("a");
			""", expression -> {
			// the whole call
			final var memberFeatureCall = getMemberFeatureCall(expression);
			// System.out
			memberFeatureCall.getMemberCallTarget();
			// the argument
			memberFeatureCall.getMemberCallArguments().getFirst();
		});
	}

	@Test
	public void testConstructorCall() throws Exception {
		assertLastExpression("""
			new String();
			""", expression -> ((XConstructorCall) expression).getArguments());
	}

	@Test
	public void testArrayConstructorCall() throws Exception {
		assertLastExpression("""
			int[] a = new int[0];
			""",
				expression -> assertTrue(
						getVariableDeclarationRightAsArrayConstructorCall(expression).getIndexes()
								.getFirst() instanceof XNumberLiteral));
	}

	@Test
	public void testMultiArrayConstructorCall() throws Exception {
		assertLastExpression("""
			// of course true is not a valid array index
			// but we only test parsing here
			int[][] a = new int[0][true];
			""", expression -> {
			final var indexes = getVariableDeclarationRightAsArrayConstructorCall(expression)
					.getIndexes();
			assertTrue(indexes.getFirst() instanceof XNumberLiteral);
			assertTrue(indexes.getLast() instanceof XBooleanLiteral);
		});
	}

	@Test
	public void testIncompleteArrayConstructorCall() throws Exception {
		assertLastExpression("""
			int[] a = new int[
			""",
				expression -> assertTrue(
						getVariableDeclarationRightAsArrayConstructorCall(expression).getIndexes()
								.isEmpty()));
	}

	@Test
	public void testIncompleteArrayConstructorCall2() throws Exception {
		// the second dimension is parsed
		assertLastExpression("""
			int[] a = new int[][
			""", expression -> {
			final var variableDeclarationRightAsArrayConstructorCall = getVariableDeclarationRightAsArrayConstructorCall(
					expression);
			assertTrue(variableDeclarationRightAsArrayConstructorCall.getIndexes().isEmpty());
			assertEquals(2, variableDeclarationRightAsArrayConstructorCall.getDimensions().size());
		});
	}

	@Test
	public void testArrayConstructorCallWithDimensionsAndNoIndexes() throws Exception {
		assertLastExpression("""
			int[] a = new int[][][];
			""", expression -> {
			final var variableDeclarationRightAsArrayConstructorCall = getVariableDeclarationRightAsArrayConstructorCall(
					expression);
			assertTrue(variableDeclarationRightAsArrayConstructorCall.getIndexes().isEmpty());
			assertEquals(3, variableDeclarationRightAsArrayConstructorCall.getDimensions().size());
		});
	}

	@Test
	public void testArrayConstructorCallWithDimensionsAndIndexes() throws Exception {
		assertLastExpression("""
			int[] a = new int[0][0][0];
			""", expression -> {
			final var variableDeclarationRightAsArrayConstructorCall = getVariableDeclarationRightAsArrayConstructorCall(
					expression);
			assertEquals(3, variableDeclarationRightAsArrayConstructorCall.getIndexes().size());
			assertEquals(3, variableDeclarationRightAsArrayConstructorCall.getDimensions().size());
		});
	}

	@Test
	public void testArrayConstructorCallWithDimensionsAndSomeIndexes() throws Exception {
		assertLastExpression("""
			int[] a = new int[0][][0];
			""", expression -> {
			final var variableDeclarationRightAsArrayConstructorCall = getVariableDeclarationRightAsArrayConstructorCall(
					expression);
			assertEquals(2, variableDeclarationRightAsArrayConstructorCall.getIndexes().size());
			assertEquals(3, variableDeclarationRightAsArrayConstructorCall.getDimensions().size());
		});
	}

	@Test
	public void testArrayConstructorCallWithDimensionsAndArrayLiteral() throws Exception {
		assertLastExpression("""
			int[] a = new int[][][] {};
			""", expression -> {
			final var variableDeclarationRightAsArrayConstructorCall = getVariableDeclarationRightAsArrayConstructorCall(
					expression);
			assertEquals(3, variableDeclarationRightAsArrayConstructorCall.getDimensions().size());
			assertNotNull(variableDeclarationRightAsArrayConstructorCall.getArrayLiteral());
		});
	}

	@Test
	public void testArrayConstructorCallWithDimensionsAndIncompleteArrayLiteral() throws Exception {
		assertLastExpression("""
			int[] a = new int[][][] {
			""", this::getVariableDeclarationRightAsArrayConstructorCall);
	}

	@Test
	public void testDiamondConstructorCall() throws Exception {
		assertLastExpression("""
			new java.util.List<>();
			""", expression -> assertTrue(
				getConstructorCall(expression).getTypeArguments().isEmpty()));
	}

	@Test
	public void testArrayLiteral() throws Exception {
		assertLastExpression(jbaseInputs.arrayLiteral(), expression -> assertEquals(3,
				((XListLiteral) getVariableDeclarationRight(expression)).getElements().size()));
	}

	@Test
	public void testStringLiteral() throws Exception {
		assertLastExpression("""
			"a";
			""", expression -> assertEquals(1, ((XStringLiteral) expression).getValue().length()));
	}

	@Test
	public void testCharLiteral() throws Exception {
		assertLastExpression("""
			'a';
			""", expression -> assertEquals(1, ((XJCharLiteral) expression).getValue().length()));
	}

	@Test
	public void testIncompleteVariableDeclaration() throws Exception {
		assertLastExpression("""
			int i = a
			""", expression -> assertNotNull(getVariableDeclarationRight(expression)));
	}

	@Test
	public void testIncompleteVariableDeclaration2() throws Exception {
		assertLastExpression("""
			int i =
			""", expression -> assertNull(getVariableDeclarationRight(expression)));
	}

	@Test
	public void testIncompleteVariableDeclaration3() throws Exception {
		assertLastExpression("""
			int i = ;
			""", expression -> assertNull(getVariableDeclarationRight(expression)));
	}

	@Test
	public void testIncompleteFeatureCall() throws Exception {
		assertLastExpression("""
			my
			""", expression -> assertNull(getFeatureCall(expression).getActualReceiver()));
	}

	@Test
	public void testIncompleteMemberCall() throws Exception {
		assertLastExpression("""
			String s = "";
			s.m
			""", expression -> assertNotNull(getMemberFeatureCall(expression).getActualReceiver()));
	}

	@Test
	public void testIncompleteMemberCall2() throws Exception {
		assertLastExpression("""
			s.m
			""", expression -> assertNotNull(getMemberFeatureCall(expression).getActualReceiver()));
	}

	@Test
	public void testIncompleteMemberCall4() throws Exception {
		assertLastExpression("""
			s.
			""", expression -> assertNotNull(getMemberFeatureCall(expression).getActualReceiver()));
	}

	@Test
	public void testWhileWithBlocks() throws Exception {
		assertLastExpression("""
			while (i < 10) {
				i = i + 1;
			}
			""", expression -> assertNotNull(getWhileExpression(expression).getPredicate()));
	}

	@Test
	public void testSeveralVariableDeclarations() throws Exception {
		assertLastExpression("""
			int i, j = 0, k;
			""", expression -> {
			final var variableDeclaration = getVariableDeclaration(expression);
			assertNotNull(variableDeclaration.getType());
			final var additionalVariables = variableDeclaration.getAdditionalVariables();
			assertEquals(2, additionalVariables.size());
			assertNotNull(additionalVariables.get(0).getRight());
			assertNull(additionalVariables.get(1).getRight());
		});
	}

	@Test
	public void testSeveralVariableDeclarationsInForLoop() throws Exception {
		assertLastExpression("""
			for (int i, j = 0, k; i < 0; i++) {}
			""", expression -> {
			final var variableDeclaration = getVariableDeclaration(
					getBasicForLoop(expression).getInitExpressions().getFirst());
			assertNotNull(variableDeclaration.getType());
			final var additionalVariables = variableDeclaration.getAdditionalVariables();
			assertEquals(2, additionalVariables.size());
			assertNotNull(additionalVariables.get(0).getRight());
			assertNull(additionalVariables.get(1).getRight());
		});
	}

	@Test
	public void testSeveralAssignmentsInForLoop() throws Exception {
		assertLastExpression("""
			int i;
			int j;
			int k;
			for (i = 0, j = 0, k = 0; i < 0; i++) {}
			""", expression -> assertEquals(3,
				getBasicForLoop(expression).getInitExpressions().size()));
	}

	@Test
	public void testPostfixOperation() throws Exception {
		assertLastExpression("""
			int i = 0;
			i++;
			""", expression -> assertTrue(
				((XPostfixOperation) expression).getOperand() instanceof XFeatureCall));
	}

	@Test
	public void testPrefixOperation() throws Exception {
		assertLastExpression("""
			int i = 0;
			++i;
			""", expression -> assertTrue(
				((XJPrefixOperation) expression).getOperand() instanceof XFeatureCall));
	}

	@Test
	public void testMemberFeatureCallIndex() throws Exception {
		assertLastExpression("""
			int[][] arr;
			arr[0].length;
			""", expression -> assertFalse(
				getMemberCallTargetArrayAccess(expression).getIndexes().isEmpty()));
	}

	@Test
	public void testIncompleteMemberFeatureCallIndex() throws Exception {
		assertLastExpression("""
			arr[0].;
			""", expression -> assertFalse(
				getMemberCallTargetArrayAccess(expression).getIndexes().isEmpty()));
	}

	@Test
	public void testMemberCallTargetOfJavammMemberFeatureCallIsArrayAccessExpression()
			throws Exception {
		assertLastExpression("""
			int[][] arr;
			arr[0].length;
			""", this::getMemberCallTargetArrayAccess);
	}

	@Test
	public void testCastedExpression() throws Exception {
		assertLastExpression("""
			(int) 10;
			""", expression -> {
			final var casted = getCasted(expression);
			assertEquals("int", casted.getType().getSimpleName());
			assertTrue(casted.getTarget() instanceof XNumberLiteral);
		});
	}

	@Test
	public void testCastPrecedenceOverDivision() throws Exception {
		assertLastExpression("""
			int somma = 15;
			int numero = 6;
			double media = (double) somma / numero;
			""", expression -> {
			final var division = getXBinaryOperation(getVariableDeclarationRight(expression));
			final var cast = getCasted(division.getLeftOperand());
			assertEquals("double", cast.getType().getSimpleName());
			assertEquals("somma", getFeatureCall(cast.getTarget()).getFeature().getSimpleName());
			assertEquals("numero",
				getFeatureCall(division.getRightOperand()).getFeature().getSimpleName());
		});
	}

	@Test
	public void testCastPrecedenceOverBinaryOperations() throws Exception {
		for (final var operator : new String[] {
				"*", "/", "%", "+", "-", "<<", ">>", ">>>", "<", "<=", ">", ">=",
				"==", "!=", "&", "^", "|" }) {
			final var parsed = parseHelper.parse("(int) 1 " + operator + " 2;");
			assertTrue(parsed.eResource().getErrors().toString(),
				parsed.eResource().getErrors().isEmpty());
			applyTester(parsed, expression -> {
				final var binary = getXBinaryOperation(expression);
				assertEquals("1", ((XNumberLiteral) getCasted(binary.getLeftOperand())
					.getTarget()).getValue());
				assertEquals("2", ((XNumberLiteral) binary.getRightOperand()).getValue());
			});
		}
		for (final var operator : new String[] { "&&", "||" }) {
			final var parsed = parseHelper.parse("(boolean) true " + operator + " false;");
			assertTrue(parsed.eResource().getErrors().toString(),
				parsed.eResource().getErrors().isEmpty());
			applyTester(parsed, expression -> {
				final var binary = getXBinaryOperation(expression);
				assertTrue(((XBooleanLiteral) getCasted(binary.getLeftOperand())
					.getTarget()).isIsTrue());
				assertFalse(((XBooleanLiteral) binary.getRightOperand()).isIsTrue());
			});
		}
	}

	@Test
	public void testCastOfUnaryOperation() throws Exception {
		for (final var operator : new String[] { "+", "-", "~" }) {
			assertLastExpression("(int) " + operator + "1;", expression -> {
				final var unary = getXUnaryOperation(getCasted(expression).getTarget());
				assertEquals("1", ((XNumberLiteral) unary.getOperand()).getValue());
			});
		}
		assertLastExpression("(boolean) !true;", expression -> {
			final var unary = getXUnaryOperation(getCasted(expression).getTarget());
			assertTrue(((XBooleanLiteral) unary.getOperand()).isIsTrue());
		});
	}

	@Test
	public void testCastOfParenthesizedBinaryOperation() throws Exception {
		assertLastExpression("(double) (15 / 6);", expression -> {
			final var division = getXBinaryOperation(getCasted(expression).getTarget());
			assertEquals("15", ((XNumberLiteral) division.getLeftOperand()).getValue());
			assertEquals("6", ((XNumberLiteral) division.getRightOperand()).getValue());
		});
	}

	@Test
	public void testCastOfPostfixOperation() throws Exception {
		assertLastExpression("""
			int i = 1;
			(double) i++;
			""", expression -> {
			final var postfix = (XPostfixOperation) getCasted(expression).getTarget();
			assertEquals("i", getFeatureCall(postfix.getOperand()).getFeature().getSimpleName());
		});
	}

	@Test
	public void testIncompleteCastedExpressionParsedAsParenthesizedExpression() throws Exception {
		assertLastExpression("""
			(int) ;
			""", this::getFeatureCall);
	}

	@Test
	public void testCastedExpressionWithParenthesis() throws Exception {
		assertLastExpression("""
			(char) ((int) 10);
			""", expression -> {
			final var casted = getCasted(expression);
			assertEquals("char", casted.getType().getSimpleName());
			final var castedValue = getCasted(casted.getTarget());
			assertEquals("int", castedValue.getType().getSimpleName());
			assertTrue(castedValue.getTarget() instanceof XNumberLiteral);
		});
	}

	@Test
	public void testCastedExpressionRightAssociativity() throws Exception {
		assertLastExpression("""
			(char) (int) 10; // equivalent to (char) ((int) 10)
			""", expression -> {
			final var casted = getCasted(expression);
			assertEquals("char", casted.getType().getSimpleName());
			final var castedValue = getCasted(casted.getTarget());
			assertEquals("int", castedValue.getType().getSimpleName());
			assertTrue(castedValue.getTarget() instanceof XNumberLiteral);
		});
	}

	@Test
	public void testConditionalExpression() throws Exception {
		assertLastExpression("""
			int j = 0;
			int i = j > 0 ? 1 : '2';
			""", expression -> {
			final var conditional = getConditional(getVariableDeclarationRight(expression));
			assertTrue(conditional.getThen() instanceof XNumberLiteral);
			assertTrue(conditional.getElse() instanceof XStringLiteral);
		});
	}

	@Test
	public void testIncompleteConditionalExpression() throws Exception {
		assertLastExpression("""
			int i = j > 0 ?\s
			""", expression -> {
			final var conditional = getConditional(getVariableDeclarationRight(expression));
			assertNull(conditional.getThen());
		});
	}

	@Test
	public void testIncompleteConditionalExpression2() throws Exception {
		assertLastExpression("""
			int i = j > 0 ? 1
			""", expression -> {
			final var conditional = getConditional(getVariableDeclarationRight(expression));
			assertTrue(conditional.getThen() instanceof XNumberLiteral);
			assertNull(conditional.getElse());
		});
	}

	@Test
	public void testIncompleteConditionalExpression3() throws Exception {
		assertLastExpression("""
			int i = j > 0 ? 1 :
			""", expression -> {
			final var conditional = getConditional(getVariableDeclarationRight(expression));
			assertTrue(conditional.getThen() instanceof XNumberLiteral);
			assertNull(conditional.getElse());
		});
	}

	@Test
	public void testInstanceOf() throws Exception {
		assertLastExpression("""
			String s;
			boolean b = s instanceof String;
			""", expression -> {
			final var instanceOf = getInstanceOf(getVariableDeclarationRight(expression));
			assertTrue(instanceOf.getExpression() instanceof XFeatureCall);
		});
	}

	@Test
	public void testIncompleteInstanceOf() throws Exception {
		assertLastExpression("""
			boolean b = s instanceof\s
			""", expression -> {
			final var instanceOf = getInstanceOf(getVariableDeclarationRight(expression));
			assertTrue(instanceOf.getExpression() instanceof XFeatureCall);
		});
	}

	@Test
	public void testNoQuestionMarkParsedAsBinaryExpression() throws Exception {
		assertLastExpression("""
			int j = 0;
			boolean b = j > 0;
			""", expression -> assertTrue(
				getVariableDeclarationRight(expression) instanceof XBinaryOperation));
	}

	@Test
	public void testSwitchWithoutCase() throws Exception {
		assertExpression("""
			switch (argsNum) {
				case\s
				default: System.out.println("default");
			}
			""", expression -> assertNull(getSwitch(expression).getCases().getFirst().getCase()));
	}

	@Test
	public void testSwitchWithoutCaseThen() throws Exception {
		// then part is always not null in Jbase
		assertExpression("""
			switch (argsNum) {
				case 0
				default: System.out.println("default");
			}
			""",
				expression -> assertNotNull(getSwitch(expression).getCases().getFirst().getThen()));
	}

	@Test
	public void testThrowWithoutExpression() throws Exception {
		assertLastExpression("""
			throw
			""", expression -> assertTrue("class: " + expression.getClass(),
				expression instanceof XThrowExpression));
	}

	@Test
	public void testThrowWithExpressionWithoutSemicolon() throws Exception {
		assertLastExpression("""
			throw new Exception()
			""", it -> {
			final var expression = getThrowExpression(it).getExpression();
			assertTrue("class: " + expression.getClass(), expression instanceof XConstructorCall);
		});
	}

	@Test
	public void testTryCatchIncomplete() throws Exception {
		assertLastExpression("""
			try
			""", expression -> {
			assertTrue("class: " + expression.getClass(),
					expression instanceof XTryCatchFinallyExpression);
			assertFalse("class: " + expression.getClass(),
					expression instanceof XJTryWithResourcesStatement);
		});
	}

	@Test
	public void testTryWithResourcesIncomplete() throws Exception {
		assertLastExpression("""
			try (
			""", expression -> assertTrue("class: " + expression.getClass(),
				expression instanceof XJTryWithResourcesStatement));
	}

	@Test
	public void testTryWithResourcesIncomplete2() throws Exception {
		assertLastExpression("""
			try ()
			""", expression -> assertTrue("class: " + expression.getClass(),
				expression instanceof XJTryWithResourcesStatement));
	}

	@Test
	public void testTryWithResourcesIncomplete3() throws Exception {
		assertLastExpression("""
			try (String s =
			""", expression -> assertEquals(1,
				getTryWithResources(expression).getResources().size()));
	}

	@Test
	public void testTryWithResources() throws Exception {
		assertLastExpression("""
			try (String s = "")
			""", expression -> assertEquals(1,
				getTryWithResources(expression).getResources().size()));
	}

	@Test
	public void testTryWithExistingVariableResource() throws Exception {
		assertLastExpression("""
			try (input) {}
			""", expression -> {
			final var resources = getTryWithResources(expression).getResources().stream()
				.filter(XJTryWithResourcesExistingResource.class::isInstance).toList();
			assertEquals(1, resources.size());
			assertTrue(resources.getFirst()
				.getRight() instanceof XFeatureCall);
		});
	}

	@Test
	public void testTryWithResources2() throws Exception {
		assertLastExpression("""
			try (String s = "" ; String s = "")
			""", expression -> assertEquals(2,
				getTryWithResources(expression).getResources().size()));
	}

	@Test
	public void testTryWithResources3() throws Exception {
		assertLastExpression("""
			try (String s = "" ; String s = "";)
			""", expression -> assertEquals(2,
				getTryWithResources(expression).getResources().size()));
	}

	@Test
	public void testTryWithResourcesWithCatches() throws Exception {
		assertLastExpression("""
			try (String s = "" ; String s = "";) {
			\t
			} catch (Exception e) {
			\t
			} catch (Exception e) {
			\t
			}
			""", expression -> assertEquals(2,
				getTryWithResources(expression).getCatchClauses().size()));
	}

	@Test
	public void testTryWithResourcesWithCatchesAndFinally() throws Exception {
		assertLastExpression("""
			try (String s = "" ; String s = "";) {
			\t
			} catch (Exception e) {
			\t
			} catch (Exception e) {
			\t
			} finally {
			\t
			}
			""", expression -> {
			assertEquals(2, getTryWithResources(expression).getCatchClauses().size());
			assertNotNull(getTryWithResources(expression).getFinallyExpression());
		});
	}

	@Test
	public void testTryWithResourcesWithFinally() throws Exception {
		assertLastExpression("""
			try (String s = "" ; String s = "";) {
			\t
			} finally {
			\t
			}
			""", expression -> {
			assertTrue(getTryWithResources(expression).getCatchClauses().isEmpty());
			assertNotNull(getTryWithResources(expression).getFinallyExpression());
		});
	}

	@Test
	public void testMultiCatch() throws Exception {
		final var source = """
			try {
				throw new java.io.IOException();
			} catch (java.io.IOException | IllegalArgumentException ex) {
			}
			""";
		parseAndAssertNoErrors(source);
		assertLastExpression(source, expression -> {
			final var catchClause =
				((XTryCatchFinallyExpression) expression).getCatchClauses().getFirst();
			final var parameterType = catchClause.getDeclaredParam().getParameterType();
			assertTrue(parameterType instanceof JvmSynonymTypeReference);
			assertEquals(2, ((JvmSynonymTypeReference) parameterType).getReferences().size());
		});
	}

	@Test
	public void testSynchronizedIncomplete() throws Exception {
		assertLastExpression("""
			synchronized (
			""", expression -> assertTrue("class: " + expression.getClass(),
				expression instanceof XSynchronizedExpression));
	}

	@Test
	public void testClassObject() throws Exception {
		assertLastExpression("""
			String.class
			""", expression -> assertTrue("class: " + expression.getClass(),
				expression instanceof XJClassObject));
	}

	@Test
	public void testClassObjectWithArrayDimensions() throws Exception {
		assertLastExpression("""
			String[].class
			""", expression -> {
			final var classObject = getClassObject(expression);
			assertEquals("String", classObject.getTypeExpression().toString());
			assertEquals(1, classObject.getArrayDimensions().size());
		});
	}

	@Test
	public void testClassObjectWithWrongTypeExpression() throws Exception {
		// we parse it as a ClassObject though it is not well-typed
		assertLastExpression("""
			a.class
			""", expression -> {
			final var classObject = getClassObject(expression);
			assertEquals("a", classObject.getTypeExpression().toString());
		});
	}

	@Test
	public void testBooleanAnd() throws Exception {
		assertBinaryOperation("""
			true && false;
			""", "org.eclipse.xtext.xbase.lib.BooleanExtensions.operator_and(boolean,boolean)");
	}

	@Test
	public void testBitwiseAnd() throws Exception {
		assertBinaryOperation("""
			0 & 1;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseAnd(int,int)");
	}

	@Test
	public void testBooleanOr() throws Exception {
		assertBinaryOperation("""
			true || false;
			""", "org.eclipse.xtext.xbase.lib.BooleanExtensions.operator_or(boolean,boolean)");
	}

	@Test
	public void testBitwiseOr() throws Exception {
		assertBinaryOperation("""
			0 | 1;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseOr(int,int)");
	}

	@Test
	public void testBitwiseXor() throws Exception {
		assertBinaryOperation("""
			0 ^ 1;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseXor(int,int)");
	}

	@Test
	public void testBitwiseNot() throws Exception {
		assertUnaryOperation("""
			~1;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseNot(int)");
	}

	@Test
	public void testLeftShift() throws Exception {
		assertBinaryOperation("""
			1 << 2;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_doubleLessThan(int,int)");
	}

	@Test
	public void testRightShift() throws Exception {
		assertBinaryOperation("""
			1 >> 2;
			""",
				"org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_doubleGreaterThan(int,int)");
	}

	@Test
	public void testUnsignedRightShift() throws Exception {
		assertBinaryOperation("""
			1 >>> 2;
			""",
				"org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_tripleGreaterThan(int,int)");
	}

	@Test
	public void testBitwiseOnChar() throws Exception {
		assertBinaryOperation("""
			'a' >>> 'b';
			""",
				"org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_tripleGreaterThan(int,int)");
	}

	@Test
	public void testAndOrPrecedence() throws Exception {
		assertPrecedence("""
			1 && 2 || 3 && 4
			""", """
			((1 && 2) || (3 && 4))
			""");
	}

	@Test
	public void testBitwisePrecedenceAnd() throws Exception {
		assertPrecedence("""
			1 && 2 & 3
			""", """
			(1 && (2 & 3))
			""");
	}

	@Test
	public void testBitwisePrecedenceAndOr() throws Exception {
		assertPrecedence("""
			1 | 2 && 3
			""", """
			((1 | 2) && 3)
			""");
	}

	@Test
	public void testBitwisePrecedence() throws Exception {
		assertPrecedence("""
			1 && 2 & 3 ^ 4
			""", """
			(1 && ((2 & 3) ^ 4))
			""");
	}

	@Test
	public void testBooleanBitwiseXor() throws Exception {
		assertBinaryOperation("""
			true ^ false;
			""", "jbase.lib.JbaseBooleanExtensions.bitwiseXor(boolean,boolean)");
	}

	@Test
	public void testBooleanBitwiseAnd() throws Exception {
		assertBinaryOperation("""
			true & false;
			""", "jbase.lib.JbaseBooleanExtensions.bitwiseAnd(boolean,boolean)");
	}

	@Test
	public void testBooleanBitwiseOr() throws Exception {
		assertBinaryOperation("""
			true | false;
			""", "jbase.lib.JbaseBooleanExtensions.bitwiseOr(boolean,boolean)");
	}

	@Test
	public void testMultiAssignmentPlus() throws Exception {
		assertBinaryOperation("""
			int i = 0;
			int j = 1;
			i += j;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_plus(int,int)");
	}

	@Test
	public void testMultiAssignmentLeftShift() throws Exception {
		assertShiftAssignment("<<=", "doubleLessThan");
	}

	@Test
	public void testMultiAssignmentRightShift() throws Exception {
		assertShiftAssignment(">>=", "doubleGreaterThan");
	}

	@Test
	public void testMultiAssignmentUnsignedRightShift() throws Exception {
		assertShiftAssignment(">>>=", "tripleGreaterThan");
	}

	private void assertShiftAssignment(final String operator, final String method) throws Exception {
		assertLastExpression("""
			int n = 8;
			n %s 1;
			""".formatted(operator), expression -> {
			final var operation = getXBinaryOperation(expression);
			assertTrue(operation.eResource().getErrors().toString(),
				operation.eResource().getErrors().isEmpty());
			assertEquals("org.eclipse.xtext.xbase.lib.IntegerExtensions.operator_"
				+ method + "(int,int)", operation.getFeature().getIdentifier());
			assertTrue(operation.isReassignFirstArgument());
		});
	}

	@Test
	public void testMultiAssignmentBitwiseAnd() throws Exception {
		assertBinaryOperation("""
			int i = 0;
			int j = 1;
			i &= j;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseAnd(int,int)");
	}

	@Test
	public void testMultiAssignmentBitwiseAndBoolean() throws Exception {
		assertBinaryOperation("""
			boolean b = true;
			boolean c = false;
			b &= c;
			""", "jbase.lib.JbaseBooleanExtensions.bitwiseAnd(boolean,boolean)");
	}

	@Test
	public void testMultiAssignmentBitwiseOr() throws Exception {
		assertBinaryOperation("""
			int i = 0;
			int j = 1;
			i |= j;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseOr(int,int)");
	}

	@Test
	public void testMultiAssignmentBitwiseOrBoolean() throws Exception {
		assertBinaryOperation("""
			boolean b = true;
			boolean c = false;
			b |= c;
			""", "jbase.lib.JbaseBooleanExtensions.bitwiseOr(boolean,boolean)");
	}

	@Test
	public void testMultiAssignmentBitwiseXor() throws Exception {
		assertBinaryOperation("""
			int i = 0;
			int j = 1;
			i ^= j;
			""", "org.eclipse.xtext.xbase.lib.IntegerExtensions.bitwiseXor(int,int)");
	}

	@Test
	public void testMultiAssignmentBitwiseXorBoolean() throws Exception {
		assertBinaryOperation("""
			boolean b = true;
			boolean c = false;
			b ^= c;
			""", "jbase.lib.JbaseBooleanExtensions.bitwiseXor(boolean,boolean)");
	}

	private void assertBinaryOperation(final CharSequence input,
			final String expectedResolvedFeature) throws Exception {
		assertLastExpression(input, expression -> {
			final var xBinaryOperation = getXBinaryOperation(expression);
			assertEquals(expectedResolvedFeature, xBinaryOperation.getFeature().getIdentifier());
		});
	}

	private void assertUnaryOperation(final CharSequence input,
			final String expectedResolvedFeature) throws Exception {
		assertLastExpression(input, expression -> {
			final var xUnaryOperation = getXUnaryOperation(expression);
			assertEquals(expectedResolvedFeature, xUnaryOperation.getFeature().getIdentifier());
		});
	}

	private void assertPrecedence(final CharSequence input, final CharSequence expected)
			throws Exception {
		assertLastExpression(input,
				expression -> assertEquals(expected.toString().trim(), stringRepr(expression)));
	}

	private String stringRepr(final EObject model) {
		if (model instanceof XBinaryOperation operation) {
			final var operatorNode = NodeModelUtils.findNodesForFeature(model,
					XbasePackage.eINSTANCE.getXAbstractFeatureCall_Feature()).getFirst();
			return "(" + stringRepr(operation.getLeftOperand()) + " "
					+ NodeModelUtils.getTokenText(operatorNode) + " "
					+ stringRepr(operation.getRightOperand()) + ")";
		}
		return new JbaseNodeModelUtil().getProgramText(model);
	}
}
