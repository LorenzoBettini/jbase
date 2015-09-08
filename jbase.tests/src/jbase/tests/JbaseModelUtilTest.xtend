package jbase.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.common.types.JvmFormalParameter
import org.eclipse.xtext.common.types.TypesFactory
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.xbase.XNumberLiteral
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.junit.Test
import org.junit.runner.RunWith
import jbase.JbaseInjectorProvider
import jbase.testlanguage.jbaseTestlanguage.JbaseTestLanguageModel
import jbase.util.JbaseModelUtil
import jbase.jbase.XJArrayConstructorCall
import jbase.jbase.XJJvmFormalParameter
import jbase.jbase.JbaseFactory

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseModelUtilTest extends JbaseAbstractTest {

	@Inject extension JbaseModelUtil
	@Inject extension IJvmModelAssociations

	@Test def void testArrayContructorCallOneDimensionHasExpression() {
		'''
		new int[0]
		'''.assertArrayDimensionIndexAssociations("[0]")
	}

	@Test def void testArrayContructorCallOneDimensionWithoutExpression() {
		'''
		new int[]
		'''.assertArrayDimensionIndexAssociations("[null]")
	}

	@Test def void testArrayContructorCallEachDimensionHasAnExpression() {
		'''
		new int[0][1][2]
		'''.assertArrayDimensionIndexAssociations("[0, 1, 2]")
	}

	@Test def void testArrayContructorCallNoDimensionHasAnExpression() {
		'''
		new int[][][]
		'''.assertArrayDimensionIndexAssociations("[null, null, null]")
	}

	@Test def void testArrayContructorCallSomeDimensionHasExpression1() {
		'''
		new int[0][][]
		'''.assertArrayDimensionIndexAssociations("[0, null, null]")
	}

	@Test def void testArrayContructorCallSomeDimensionHasExpression2() {
		'''
		new int[][1][]
		'''.assertArrayDimensionIndexAssociations("[null, 1, null]")
	}

	@Test def void testArrayContructorCallSomeDimensionHasExpression3() {
		'''
		new int[][][2]
		'''.assertArrayDimensionIndexAssociations("[null, null, 2]")
	}

	@Test def void testArrayContructorCallSomeDimensionHasExpression4() {
		'''
		new int[0][][2]
		'''.assertArrayDimensionIndexAssociations("[0, null, 2]")
	}

	@Test def void testOriginalParamWithJbaseParam() {
		val p = JbaseFactory.eINSTANCE.createXJJvmFormalParameter
		p.assertSame(p.originalParam)
	}

	@Test def void testOriginalParamWithoutJvmModelAssociations() {
		val p = TypesFactory.eINSTANCE.createJvmFormalParameter
		assertNull(p.originalParam)
	}

	@Test def void testOriginalParamWithJvmModelAssociations() {
		useJbaseTestlanguage
		
		val EObject m = '''
		op m(String p) {}
		m("f");
		'''.parse
		
		val sourceParam = (m as JbaseTestLanguageModel).operations.head.params.head as XJJvmFormalParameter
		val p = sourceParam.jvmElements.head as JvmFormalParameter
		
		assertNotSame(sourceParam, p)
		assertSame(sourceParam, p.originalParam)
	}

	/**
	 * Assumes that dimension expressions, if given, are number literals
	 */
	private def assertArrayDimensionIndexAssociations(CharSequence input, CharSequence expected) {
		expected.
			assertEquals(
				input.lastArrayConstructorCall.arrayDimensionIndexAssociations.
					map[
						e |
						if (e == null) {
							"null"
						} else {
							(e as XNumberLiteral).value
						}
					].toString
			)
	}

	private def lastArrayConstructorCall(CharSequence input) {
		input.parse as XJArrayConstructorCall
	}

}