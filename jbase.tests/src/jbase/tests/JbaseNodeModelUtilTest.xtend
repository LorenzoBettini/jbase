package jbase.tests

import com.google.inject.Inject
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.xbase.XAssignment
import org.junit.Test
import org.junit.runner.RunWith
import jbase.JbaseInjectorProvider
import jbase.util.JbaseNodeModelUtil

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseNodeModelUtilTest extends JbaseAbstractTest {
	
	@Inject extension JbaseNodeModelUtil

	@Test def void testTerminatingSemicolon() {
		'''
		i = 1;
		'''.assertLastExpressionText(
		"i = 1;"
		)
	}

	@Test def void testTerminatingSemicolon2() {
		'''
		i = 1;
		j = 0 ;
		'''.assertLastExpressionText(
		"j = 0 ;"
		)
	}

	@Test def void testAdditionalSemicolon() {
		// additional semicolons are removed in blocks
		'''
		{ i = 1 ;; }
		'''.assertLastExpressionText(
		"i = 1 ;"
		)
	}

	@Test def void testMissingSemicolon() {
		'''
		i = 1
		'''.assertLastExpressionText(
		"i = 1"
		)
	}

	@Test def void testMissingSemicolonDetected() {
		'''
		i = 1
		'''.assertLastExpressionTextSemicolon(false)
	}

	@Test def void testSemicolonDetected() {
		'''
		i = 1;
		'''.assertLastExpressionTextSemicolon(true)
	}

	@Test def void testSemicolonDetectedOnNextLine() {
		'''
		i = 1
		;
		'''.assertLastExpressionTextSemicolon(true)
	}

	@Test def void testOffset() {
		'''
		i = 1
		;
		'''.parse => [
			(it as XAssignment) => [
				4.assertEquals(value.elementOffsetInProgram)
			]
		]
	}

	def private assertLastExpressionText(CharSequence input, CharSequence expected) {
		assertLastExpression(input) [
			expected.assertEquals(programText)
		]
	}

	def private assertLastExpressionTextSemicolon(CharSequence input, boolean expectedSemicolon) {
		assertLastExpression(input) [
			expectedSemicolon.assertEquals(hasSemicolon)
		]
	}

}