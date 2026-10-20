package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import jbase.jbase.JbaseFactory;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJSemicolonStatement;
import jbase.testlanguage.jbaseTestlanguage.JbaseTestLanguageModel;
import jbase.util.JbaseModelUtil;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseModelUtilTest extends JbaseAbstractTest {

	@Inject
	private JbaseModelUtil jbaseModelUtil;

	@Inject
	private IJvmModelAssociations iJvmModelAssociations;

	@Test
	public void testArrayContructorCallOneDimensionHasExpression() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[0]
			""", "[0]");
	}

	@Test
	public void testArrayContructorCallOneDimensionWithoutExpression() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[]
			""", "[null]");
	}

	@Test
	public void testArrayContructorCallEachDimensionHasAnExpression() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[0][1][2]
			""", "[0, 1, 2]");
	}

	@Test
	public void testArrayContructorCallNoDimensionHasAnExpression() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[][][]
			""", "[null, null, null]");
	}

	@Test
	public void testArrayContructorCallSomeDimensionHasExpression1() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[0][][]
			""", "[0, null, null]");
	}

	@Test
	public void testArrayContructorCallSomeDimensionHasExpression2() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[][1][]
			""", "[null, 1, null]");
	}

	@Test
	public void testArrayContructorCallSomeDimensionHasExpression3() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[][][2]
			""", "[null, null, 2]");
	}

	@Test
	public void testArrayContructorCallSomeDimensionHasExpression4() throws Exception {
		assertArrayDimensionIndexAssociations("""
			new int[0][][2]
			""", "[0, null, 2]");
	}

	@Test
	public void testOriginalParamWithJbaseParam() {
		final var p = JbaseFactory.eINSTANCE.createXJJvmFormalParameter();
		assertSame(p, jbaseModelUtil.getOriginalParam(p));
	}

	@Test
	public void testOriginalParamWithoutJvmModelAssociations() {
		final var p = TypesFactory.eINSTANCE.createJvmFormalParameter();
		assertNull(jbaseModelUtil.getOriginalParam(p));
	}

	@Test
	public void testOriginalParamWithJvmModelAssociations() throws Exception {
		useJbaseTestlanguage();
		final EObject m = parseHelper.parse("""
			op m(String p) {}
			m("f");
			""");
		final var sourceParam = ((JbaseTestLanguageModel) m).getOperations().getFirst().getParams()
				.getFirst();
		final var p = (JvmFormalParameter) iJvmModelAssociations.getJvmElements(sourceParam)
				.iterator().next();
		assertNotSame(sourceParam, p);
		assertSame(sourceParam, jbaseModelUtil.getOriginalParam(p));
	}

	@Test
	public void testRawTypeFalse() throws Exception {
		assertLastExpression("""
			new java.util.LinkedList<>();
			""", expression -> assertFalse(
				jbaseModelUtil.isRawType(getConstructorCall(expression))));
	}

	@Test
	public void testRawTypeFalse2() throws Exception {
		assertLastExpression("""
			new java.util.LinkedList<String>();
			""", expression -> assertFalse(
				jbaseModelUtil.isRawType(getConstructorCall(expression))));
	}

	@Test
	public void testRawTypeFalse3() throws Exception {
		assertLastExpression("""
			new String();
			""", expression -> assertFalse(
				jbaseModelUtil.isRawType(getConstructorCall(expression))));
	}

	@Test
	public void testRawTypeFalse4() throws Exception {
		assertLastExpression("""
			new int();
			""", expression -> assertFalse(
				jbaseModelUtil.isRawType(getConstructorCall(expression))));
	}

	@Test
	public void testRawTypeTrue() throws Exception {
		assertLastExpression("""
			new java.util.LinkedList();
			""",
				expression -> assertTrue(jbaseModelUtil.isRawType(getConstructorCall(expression))));
	}

	/**
	 * Assumes that dimension expressions, if given, are number literals
	 */
	private void assertArrayDimensionIndexAssociations(final CharSequence input,
			final CharSequence expected) throws Exception {
		final var indexes = jbaseModelUtil
				.arrayDimensionIndexAssociations(lastArrayConstructorCall(input)).stream()
				.map(expression -> expression == null ? "null"
						: ((XNumberLiteral) expression).getValue())
				.toList();
		assertEquals(expected, indexes.toString());
	}

	private XJArrayConstructorCall lastArrayConstructorCall(final CharSequence input)
			throws Exception {
		return (XJArrayConstructorCall) ((XJSemicolonStatement) parseHelper.parse(input))
				.getExpression();
	}
}
