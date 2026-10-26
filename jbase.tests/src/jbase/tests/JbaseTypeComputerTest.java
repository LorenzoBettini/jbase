package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import jbase.jbase.XJSemicolonStatement;
import jbase.jbase.XJVariableDeclaration;
import jbase.tests.util.SimpleJvmModelTestInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseTypeComputerTest extends JbaseAbstractTest {

	@Inject
	private IBatchTypeResolver typeResolver;

	@Test
	public void testStringLiteral() throws Exception {
		assertActualType("\"a\"", "java.lang.String");
	}

	@Test
	public void testCharLiteral() throws Exception {
		assertActualType("'a'", "char");
	}

	@Test
	public void testCharLiteralWithCharExpectation() throws Exception {
		assertVarExpressionActualType("char c = 'a'", "char");
	}

	@Test
	public void testCharLiteralWithIntExpectation() throws Exception {
		assertVarExpressionActualType("int c = 'a'", "int");
	}

	@Test
	public void testCharLiteralWithStringExpectation() throws Exception {
		assertVarExpressionActualType("String c = 'a'", "char");
	}

	@Test
	public void testCharLiteralWithVoidExpectation() throws Exception {
		assertVarExpressionActualType("void c = 'a'", "char");
	}

	@Test
	public void testCharLiteralWithBooleanExpectation() throws Exception {
		assertVarExpressionActualType("boolean c = 'a'", "char");
	}

	@Test
	public void testNumberLiteral() throws Exception {
		assertActualType("0", "int");
	}

	@Test
	public void testNumberLiteralWithByteExpectation() throws Exception {
		assertVarExpressionActualType("byte b = 0", "byte");
	}

	@Test
	public void testNumberLiteralWithShortExpectation() throws Exception {
		assertVarExpressionActualType("short b = 0", "short");
	}

	@Test
	public void testUnaryOperationWithShortExpectation() throws Exception {
		assertVarExpressionActualType("short b = -1", "short");
	}

	@Test
	public void testDoubleUnaryOperationWithShortExpectation() throws Exception {
		assertVarExpressionActualType("short b = -(+1)", "short");
	}

	@Test
	public void testNumberLiteralWithShortExpectationGreaterThanMaxShort() throws Exception {
		assertVarExpressionActualType("short b = 32768", "int");
	}

	@Test
	public void testNumberLiteralWithCharExpectation() throws Exception {
		assertVarExpressionActualType("char b = 0", "char");
	}

	@Test
	public void testUnaryOperationWithCharExpectation() throws Exception {
		assertVarExpressionActualType("char b = +1", "char");
	}

	@Test
	public void testNegativeUnaryOperationWithCharExpectation() throws Exception {
		assertVarExpressionActualType("char b = -1", "int");
	}

	@Test
	public void testNumberLiteralWithCharExpectationGreaterThanMaxChar() throws Exception {
		assertVarExpressionActualType("char b = 65536", "int");
	}

	@Test
	public void testNumberLiteralWithLongExpectation() throws Exception {
		assertVarExpressionActualType("long b = 65536", "int");
	}

	@Test
	public void testNumberLiteralWithString() throws Exception {
		assertVarExpressionActualType("String b = 65536", "int");
	}

	@Test
	public void testNumberLiteralInBinaryOperation() throws Exception {
		assertVarExpressionActualType("int i = 1 + 128", "int");
	}

	@Test
	public void testEmptyStatement() throws Exception {
		assertReturnType(parseHelper.parse(";"), "void");
	}

	@Test
	public void testUnaryOperationWithString() throws Exception {
		assertVarExpressionActualType("String b = -65536", "int");
	}

	@Test
	public void testUnaryOperation() throws Exception {
		assertActualType("-1", "int");
	}

	@Test
	public void testVariableDeclaration() throws Exception {
		assertLastExpression("int i;",
				expression -> assertActualType(getVariableDeclaration(expression), "void"));
	}

	@Test
	public void testVariableDeclarationAddedToScope() throws Exception {
		assertActualType("""
			int i;
			i;
			""", "int");
	}

	@Test
	public void testAdditionalVars() throws Exception {
		assertLastExpression("int i, j, k;", expression -> {
			final var varDecl = getVariableDeclaration(expression);
			varDecl.getAdditionalVariables()
					.forEach(variable -> assertActualType(variable, "void"));
		});
	}

	@Test
	public void testAdditionalVarsAddedToScope() throws Exception {
		assertActualType("""
			int i, j, k;
			j;
			""", "int");
	}

	@Test
	public void testInstanceOfDoesNotImplyImplicitCast() throws Exception {
		assertLastExpression("""
			Object o;
			if (o instanceof java.util.Date)
				o
			""", expression -> assertActualType(getIfStatement(expression).getThen(),
				"java.lang.Object"));
	}

	@Test
	public void testReassignTypeDelegatedToSuper() throws Exception {
		assertLastExpression("""
			Object o;
			if (o != null)
				o
			""", expression -> assertActualType(getIfStatement(expression).getThen(),
				"java.lang.Object"));
	}

	@Test
	public void testXAssignment() throws Exception {
		assertActualType("""
			int i;
			i = "a"
			""", "int");
	}

	@Test
	public void testXJAssignment() throws Exception {
		// the type of the declared variable
		assertActualType("""
			int[] i;
			i[0] = "a"
			""", "int[]");
	}

	@Test
	public void testXJAssignment2() throws Exception {
		// the type of the declared variable
		assertActualType("""
			int i;
			i[0] = "a"
			""", "int");
	}

	@Test
	public void testXJAssignment3() throws Exception {
		// the variable is not declared
		assertActualType("""
			i[0] = "a"
			""", "Object");
	}

	@Test
	public void testSwitch() throws Exception {
		assertExpression("""
			switch (argsNum) {
				case 0 : System.out.println("0");
				default: System.out.println("default");
			}
			""", expression -> assertReturnType(expression, "void"));
	}

	@Test
	public void testSwitchWithoutCase() throws Exception {
		assertExpression("""
			switch (argsNum) {
				case\s
				default: System.out.println("default");
			}
			""", expression -> assertReturnType(expression, "null"));
	}

	@Test
	public void testSwitchWithoutCaseThen() throws Exception {
		assertExpression("""
			switch (argsNum) {
				case 0 :
				default: System.out.println("default");
			}
			""", expression -> assertReturnType(expression, "void"));
	}

	@Test
	public void testSwitchWithReturn() throws Exception {
		assertExpression("""
			switch (argsNum) {
				case 0 : return "0";
				default: return "1";
			}
			""", expression -> assertReturnType(expression, "java.lang.String"));
	}

	@Test
	public void testSwitchWithCaseWithBreak() throws Exception {
		assertExpression("""
			switch (argsNum) {
				case 0 : System.out.println("0"); break;
				default: return "1";
			}
			""", expression -> assertReturnType(expression, "java.lang.String"));
	}

	@Test
	public void testSwitchWithReturnAndExpectation() throws Exception {
		new SimpleJvmModelTestInjectorProvider().getInjector().injectMembers(this);
		assertExpression("""
			switch (argsNum) {
				case 0 : return "0";
				default: return "1";
			}
			""", expression -> assertReturnType(expression, "java.lang.String"));
	}

	@Test
	public void testSwitchWithReturnAndExpectationWithoutDefault() throws Exception {
		// this is not valid since the default is missing, but there'll
		// be a specific error for that
		new SimpleJvmModelTestInjectorProvider().getInjector().injectMembers(this);
		assertExpression("""
			switch (argsNum) {
				case 0 : return "0";
			}
			""", expression -> assertReturnType(expression, "java.lang.String"));
	}

	@Test
	public void testSwitchWithoutDefault() throws Exception {
		// any type ref
		assertExpression("""
			switch (argsNum) {
				case 0 : System.out.println("0");
				case 1 : System.out.println("1");
			}
			""", expression -> assertReturnType(expression, "null"));
	}

	@Test
	public void testMemberFeatureCallOnArray() throws Exception {
		assertLastExpression("""
			i[0].toString();
			""", expression -> assertActualType(expression, "Object"));
	}

	@Test
	public void testMemberFeatureCallOnArray2() throws Exception {
		assertLastExpression("""
			Integer[] i;
			i[0].toString();
			""", expression -> assertActualType(expression, "java.lang.String"));
	}

	@Test
	public void testArrayConstructorCall() throws Exception {
		assertLastExpression("""
			new String[1];
			""", expression -> assertActualType(expression, "java.lang.String[]"));
	}

	@Test
	public void testArrayConstructorCallWithArrayLiteral() throws Exception {
		assertLastExpression("""
			new String[1] { 0 };
			""", expression -> assertActualType(expression, "java.lang.String[]"));
	}

	@Test
	public void testDiamondConstructorCallStandalone() throws Exception {
		assertLastExpression("""
			new java.util.List<>();
			""", expression -> assertActualType(expression, "java.util.List<java.lang.Object>"));
	}

	@Test
	public void testDiamondConstructorCallInVarDecl() throws Exception {
		assertLastExpression("""
			java.util.List<String> l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getVariableDeclarationRight(expression),
				"java.util.ArrayList<java.lang.String>"));
	}

	@Test
	public void testDiamondConstructorCallInVarDeclWithNestedTypeArg() throws Exception {
		assertLastExpression("""
			java.util.List<java.util.Collection<String>> l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getVariableDeclarationRight(expression),
				"java.util.ArrayList<java.util.Collection<java.lang.String>>"));
	}

	@Test
	public void testDiamondConstructorCallInVarDeclWithWildCard() throws Exception {
		assertLastExpression("""
			java.util.List<? extends String> l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getVariableDeclarationRight(expression),
				"java.util.ArrayList<java.lang.String>"));
	}

	@Test
	public void testDiamondConstructorCallInVarDeclWithWildCard2() throws Exception {
		assertLastExpression("""
			java.util.List<? super String> l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getVariableDeclarationRight(expression),
				"java.util.ArrayList<java.lang.String>"));
	}

	@Test
	public void testDiamondConstructorCallInVarDeclWithWildCard3() throws Exception {
		// this seems to be a bug in Xbase, since it should resolve as
		// java.util.ArrayList<java.util.LinkedList<java.lang.String>>
		assertLastExpression("""
			java.util.List<java.util.LinkedList<? extends String>> l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getVariableDeclarationRight(expression),
				"java.util.ArrayList<java.util.LinkedList<? extends java.lang.String>>"));
	}

	@Test
	public void testDiamondConstructorCallInAssignment() throws Exception {
		assertLastExpression("""
			java.util.List<String> l;
			l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getAssignmentRight(expression),
				"java.util.ArrayList<java.lang.String>"));
	}

	@Test
	public void testDiamondConstructorCallInAssignmentWithNestedTypeArg() throws Exception {
		assertLastExpression("""
			java.util.List<java.util.Collection<String>> l;
			l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getAssignmentRight(expression),
				"java.util.ArrayList<java.util.Collection<java.lang.String>>"));
	}

	@Test
	public void testDiamondConstructorCallInAssignmentWithWildCard() throws Exception {
		assertLastExpression("""
			java.util.List<? extends String> l;
			l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getAssignmentRight(expression),
				"java.util.ArrayList<java.lang.String>"));
	}

	@Test
	public void testDiamondConstructorCallInAssignmentWithWildCard2() throws Exception {
		assertLastExpression("""
			java.util.List<? super String> l;
			l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getAssignmentRight(expression),
				"java.util.ArrayList<java.lang.String>"));
	}

	@Test
	public void testDiamondConstructorCallInAssignmentWithWildCard3() throws Exception {
		// this seems to be a bug in Xbase, since it should resolve as
		// java.util.ArrayList<java.util.LinkedList<java.lang.String>>
		assertLastExpression("""
			java.util.List<java.util.LinkedList<? extends String>> l;
			l = new java.util.ArrayList<>();
			""", expression -> assertActualType(getAssignmentRight(expression),
				"java.util.ArrayList<java.util.LinkedList<? extends java.lang.String>>"));
	}

	@Test
	public void testBranchingStatement() throws Exception {
		assertLastExpression("""
			continue;
			""", expression -> assertActualType(expression, "void"));
	}

	@Test
	public void testLabeledStatement() throws Exception {
		assertLastExpression("exit: {}", expression -> assertActualType(expression, "void"));
	}

	@Test
	public void testClassObject() throws Exception {
		assertLastExpression("""
			String.class;
			""", expression -> assertActualType(expression, "java.lang.Class<java.lang.String>"));
	}

	@Test
	public void testClassObjectWithArrayDimensions() throws Exception {
		assertLastExpression("""
			String[].class;
			""", expression -> assertActualType(expression, "java.lang.Class<java.lang.String[]>"));
	}

	@Test
	public void testClassObjectWithWrongTypeExpression() throws Exception {
		// this is not valid, but there will be an error for that
		assertLastExpression("""
			String s;
			s.class;
			""", expression -> assertActualType(expression, "java.lang.String"));
	}

	@Test
	public void testClassObjectWithWrongTypeExpression2() throws Exception {
		// this is not valid, but there will be an error for that
		assertLastExpression("""
			new String().class;
			""", expression -> assertActualType(expression, "java.lang.String"));
	}

	@Test
	public void testTypeAsTypeLiteral() throws Exception {
		// this is valid in Xbase
		// we'll have an error for that
		assertLastExpression("""
			Class c = String;
			""", expression -> assertActualType(getVariableDeclaration(expression).getRight(),
				"java.lang.Class<java.lang.String>"));
	}

	@Test
	public void testArrayAccessExpressionWithoutMainArrayExpression() throws Exception {
		assertLastExpression("""
			()[0];
			""", expression -> assertActualType(expression, "void"));
	}

	private void assertActualType(final CharSequence input, final String expectedTypeName)
			throws Exception {
		assertLastExpression(input, expression -> assertActualType(expression, expectedTypeName));
	}

	private void assertVarExpressionActualType(final CharSequence input,
			final String expectedTypeName) throws Exception {
		assertLastExpression(input,
				expression -> assertActualType(((XJVariableDeclaration) expression).getRight(),
						expectedTypeName));
	}

	private void assertActualType(final XExpression e, final String expectedTypeName) {
		final var typeRef = getActualType(e);
		assertType(e, typeRef, expectedTypeName);
	}

	private void assertReturnType(final XExpression e, final String expectedTypeName) {
		final var typeRef = getReturnType(e);
		assertType(e, typeRef, expectedTypeName);
	}

	private void assertType(final XExpression e, final LightweightTypeReference typeRef,
			final String expectedTypeName) {
		assertNotNull("type ref was null for " + e, typeRef);
		assertEquals(("expression " + e.getClass().getSimpleName()) + ": " + e, expectedTypeName,
				toString(typeRef));
	}

	private LightweightTypeReference getActualType(final XExpression expression) {
		// the type is assigned to the container semicolon statement
		final var typedExpression = expression
				.eContainer() instanceof XJSemicolonStatement statement ? statement : expression;
		return typeResolver.resolveTypes(typedExpression).getActualType(typedExpression);
	}

	private LightweightTypeReference getReturnType(final XExpression expression) {
		return typeResolver.resolveTypes(expression).getReturnType(expression);
	}

	private String toString(final LightweightTypeReference typeref) {
		return typeref.getIdentifier();
	}
}
