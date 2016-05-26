package jbase.ui.tests

import com.google.inject.Inject
import com.google.inject.Provider
import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider
import jbase.tests.util.ui.PluginProjectHelper
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jface.text.TextSelection
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.junit4.ui.AbstractWorkbenchTest
import org.eclipse.xtext.xbase.ui.refactoring.ExpressionUtil
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableRefactoring
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseTestlanguageUiInjectorProvider))
class JbaseExtractVariableTest extends AbstractWorkbenchTest {

	val static STRING_EXPRESSION = '"foo"'

	@Inject extension PluginProjectHelper

	@Inject ExpressionUtil util

	@Inject Provider<ExtractVariableRefactoring> refactoringProvider

	@Before def void createProject() {
		createJavaPluginProject()
	}

	@Test
	def void testFinalVariable() throws Exception {
		// expression is given type int so the name of the variable will be 'i'
		'''
			System.out.println(22 + $333333$);
		'''.assertAfterExtract('''
			final int i = 333333;
			System.out.println(22 + i);
		''', true)
	}

	@Test
	def void testNonFinalVariable() throws Exception {
		// expression is given type int so the name of the variable will be 'i'
		'''
			System.out.println(22 + $333333$);
		'''.assertAfterExtract('''
			int i = 333333;
			System.out.println(22 + i);
		''', false)
	}

	@Test
	def void testInsertBlock() throws Exception {
		'''
			if(true)
				System.out.println(22 + $«STRING_EXPRESSION»$);
		'''.assertAfterExtract('''
			if(true) {
				String string = "foo";
				System.out.println(22 + string);
			}
		''', false)
	}

	def protected assertAfterExtract(CharSequence input, CharSequence expected, boolean isFinal) {
		val inputString = input.toString
		val model = inputString.replace('$','')
		val file = createTestFile(model)
		val editor = openEditor(file)
		try {
			editor.document.readOnly[
				val int offset = inputString.indexOf('$')
				val length = inputString.lastIndexOf('$') - 1 - offset
				val textSelection = new TextSelection(offset, length)
				val selection = util.findSelectedExpression(it, textSelection)
				val refactoring = refactoringProvider.get()
				refactoring.final = isFinal
				refactoring.initialize(editor, selection)
				val status = refactoring.checkAllConditions(new NullProgressMonitor)
				assertTrue(status.toString, status.OK)
				refactoring.createChange(new NullProgressMonitor).perform(new NullProgressMonitor)
			]
			assertEquals(expected.toString, editor.document.get)
		} finally {
			editor.close(false)
		}
	}		
}