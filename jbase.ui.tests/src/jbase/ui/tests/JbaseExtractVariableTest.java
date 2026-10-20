package jbase.ui.tests;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.xbase.ui.refactoring.ExpressionUtil;
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableRefactoring;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider;
import jbase.tests.util.ui.PluginProjectHelper;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageUiInjectorProvider.class)
public class JbaseExtractVariableTest extends AbstractWorkbenchTest {

	private static final String STRING_EXPRESSION = "\"foo\"";

	@Inject
	private PluginProjectHelper pluginProjectHelper;

	@Inject
	private ExpressionUtil util;

	@Inject
	private Provider<ExtractVariableRefactoring> refactoringProvider;

	@Before
	public void createProject() {
		pluginProjectHelper.createJavaPluginProject();
	}

	@Test
	public void testFinalVariable() throws Exception {
		// expression is given type int so the name of the variable will be 'i'
		assertAfterExtract("""
			System.out.println(22 + $333333$);
			""", """
			final int i = 333333;
			System.out.println(22 + i);
			""", true);
	}

	@Test
	public void testNonFinalVariable() throws Exception {
		// expression is given type int so the name of the variable will be 'i'
		assertAfterExtract("""
			System.out.println(22 + $333333$);
			""", """
			int i = 333333;
			System.out.println(22 + i);
			""", false);
	}

	@Test
	public void testInsertBlock() throws Exception {
		assertAfterExtract("""
			if(true)
				System.out.println(22 + $%s$);
			""".formatted(STRING_EXPRESSION), """
			if(true) {
				String string = "foo";
				System.out.println(22 + string);
			}
			""", false);
	}

	@Test
	public void testExtractVariableWithDiamondOperator() throws Exception {
		assertAfterExtract("""
			import java.util.List;
			import java.util.LinkedList;
			import java.util.ArrayList;

			List<LinkedList<String>> list = $new ArrayList<>()$;
			""", """
			import java.util.ArrayList;
			import java.util.LinkedList;
			import java.util.List;

			final ArrayList<LinkedList<String>> linkedLists = new ArrayList<>();
			List<LinkedList<String>> list = linkedLists;
			""", true);
	}

	@Test
	public void testExtractVariableWithDiamondOperatorInMethodCall() throws Exception {
		assertAfterExtract("""
			import java.util.ArrayList;

			System.out.println($new ArrayList<>()$);
			""", """
			import java.util.ArrayList;

			final ArrayList<Object> objects = new ArrayList<>();
			System.out.println(objects);
			""", true);
	}

	protected void assertAfterExtract(final CharSequence input, final CharSequence expected,
			final boolean isFinal) throws Exception {
		final var inputString = input.toString();
		final var model = inputString.replace("$", "");
		final var file = pluginProjectHelper.createTestFile(model);
		final var editor = pluginProjectHelper.openEditor(file);
		try {
			editor.getDocument().readOnly(resource -> {
				final var offset = inputString.indexOf("$");
				final int length = inputString.lastIndexOf("$") - 1 - offset;
				final var textSelection = new TextSelection(offset, length);
				final var selection = util.findSelectedExpression(resource, textSelection);
				final var refactoring = refactoringProvider.get();
				refactoring.setFinal(isFinal);
				refactoring.initialize(editor, selection);
				final var status = refactoring.checkAllConditions(new NullProgressMonitor());
				assertTrue(status.toString(), status.isOK());
				return refactoring.createChange(new NullProgressMonitor())
						.perform(new NullProgressMonitor());
			});
			assertEquals(expected.toString(), editor.getDocument().get());
		} finally {
			editor.close(false);
		}
	}
}
