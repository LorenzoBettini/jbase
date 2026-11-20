package jbase.ui.tests;

import java.util.Arrays;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.xbase.ui.refactoring.ExpressionUtil;
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableRefactoring;
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableHandler;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XbaseFactory;
import jbase.jbase.XJArrayLiteral;
import jbase.jbase.JbaseFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.google.inject.Provider;

import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider;
import jbase.tests.util.ui.PluginProjectHelper;
import jbase.ui.refactoring.internal.JbaseExtractVariableWizard.InputPage;
import jbase.ui.refactoring.internal.JbaseExtractVariableWizard;

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

	@Inject
	private Provider<ExtractVariableHandler> handlerProvider;


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

	@Test
	public void replacesAllOccurrencesInOneStatement() throws Exception {
		assertAfterExtract("""
			System.out.println($333333$ + 333333);
			""", """
			int i = 333333;
			System.out.println(i + i);
			""", false, true, false, false);
	}

	@Test
	public void replacesAllOccurrencesAcrossStatements() throws Exception {
		assertAfterExtract("""
			System.out.println($333333$);
			System.out.println(333333);
			""", """
			int i = 333333;
			System.out.println(i);
			System.out.println(i);
			""", false, true, false, false);
	}

	@Test
	public void keepsOtherMethodWhenFileOptionIsOff() throws Exception {
		assertAfterExtract("""
			op first() {
				System.out.println($333333$);
			}
			op second() {
				System.out.println(333333);
			}
			""", """
			op first() {
				int i = 333333;
				System.out.println(i);
			}
			op second() {
				System.out.println(333333);
			}
			""", false, true, false, false);
	}

	@Test
	public void extractsSimilarExpressionInOtherMethod() throws Exception {
		assertAfterExtract("""
			op first() {
				System.out.println($333333$);
			}
			op second() {
				System.out.println(333333);
			}
			""", """
			op first() {
				int i = 333333;
				System.out.println(i);
			}
			op second() {
				int i2 = 333333;
				System.out.println(i2);
			}
			""", false, true, true, false);
	}

	@Test
	public void extractsVarDeclaration() throws Exception {
		assertAfterExtract("""
			System.out.println($333333$);
			""", """
			var i = 333333;
			System.out.println(i);
			""", false, false, false, true);
	}

	@Test
	public void extractsVarWhenReplacingSeveralOccurrences() throws Exception {
		assertAfterExtract("""
			System.out.println($333333$ + 333333);
			""", """
			var i = 333333;
			System.out.println(i + i);
			""", false, true, false, true);
	}

	@Test
	public void extractsFinalWhenReplacingSeveralOccurrences() throws Exception {
		assertAfterExtract("""
			System.out.println($333333$ + 333333);
			""", """
			final int i = 333333;
			System.out.println(i + i);
			""", true, true, false, false);
	}

	@Test
	public void insertsBlockForRepeatedExpressions() throws Exception {
		assertAfterExtract("""
			if (true)
				System.out.println($333333$ + 333333);
			""", """
			if (true) {
				int i = 333333;
				System.out.println(i + i);
			}
			""", false, true, false, false);
	}

	@Test
	public void appliesEditedVariableName() throws Exception {
		assertAfterExtract("""
			System.out.println($333333$);
			""", """
			int value = 333333;
			System.out.println(value);
			""", false, false, false, false, "value", false);
	}

	@Test
	public void warnsForRepeatedCallsAndKeepsTheirEvaluationSites() throws Exception {
		assertAfterExtract("""
			System.out.println($System.nanoTime()$);
			System.out.println(System.nanoTime());
			""", """
			long time = System.nanoTime();
			System.out.println(time);
			long time2 = System.nanoTime();
			System.out.println(time2);
			""", false, true, false, false, "time", true);
	}

	@Test
	public void avoidsNameCollisionInAnotherMethod() throws Exception {
		assertAfterExtract("""
			op first() {
				System.out.println($333333$);
			}
			op second() {
				int i2 = 0;
				System.out.println(333333);
			}
			""", """
			op first() {
				int i = 333333;
				System.out.println(i);
			}
			op second() {
				int i2 = 0;
				int i3 = 333333;
				System.out.println(i3);
			}
			""", false, true, true, false);
	}

	@Test
	public void doesNotReplaceSameSpellingWithDifferentLocalBinding() throws Exception {
		assertAfterExtract("""
			op first() {
				int i = 1;
				System.out.println($i$);
			}
			op second() {
				int i = 2;
				System.out.println(i);
			}
			""", """
			op first() {
				int i = 1;
				int copy = i;
				System.out.println(copy);
			}
			op second() {
				int i = 2;
				System.out.println(i);
			}
			""", false, true, true, false, "copy", false);
	}

	@Test
	public void rejectsInvalidNameBeforePlanningMultipleEdits() throws Exception {
		withRefactoring("System.out.println($333333$ + 333333);", refactoring -> {
			refactoring.setReplaceAll(true);
			refactoring.setVariableName("for");
			assertTrue(refactoring.checkFinalConditions(new NullProgressMonitor()).hasError());
		});
	}

	@Test
	public void cancelsBeforePlanningMultipleEdits() throws Exception {
		withRefactoring("System.out.println($333333$ + 333333);", refactoring -> {
			refactoring.setReplaceAll(true);
			final var monitor = new NullProgressMonitor();
			monitor.setCanceled(true);
			assertThrows(OperationCanceledException.class,
				() -> refactoring.checkFinalConditions(monitor));
		});
	}

	@Test
	public void replacesLiteralsInsideNestedBlock() throws Exception {
		assertAfterExtract("""
			if (true) {
				System.out.println($333333$);
				System.out.println(333333);
			}
			""", """
			if (true) {
				int i = 333333;
				System.out.println(i);
				System.out.println(i);
			}
			""", false, true, false, false);
	}

	@Test
	public void reevaluatesExpressionsReadingMutableLocals() throws Exception {
		assertAfterExtract("""
			int i = 1;
			System.out.println($(true ? i : 0)$);
			i = 2;
			System.out.println((true ? i : 0));
			""", """
			int i = 1;
			int copy = true ? i : 0;
			System.out.println((copy));
			i = 2;
			int copy2 = true ? i : 0;
			System.out.println((copy2));
			""", false, true, false, false, "copy", false);
	}

	@Test
	public void warnsWhenExpressionContainsAMethodCall() throws Exception {
		withRefactoring("""
			System.out.println($(true ? System.nanoTime() : 0L)$);
			System.out.println((true ? System.nanoTime() : 0L));
			""", refactoring -> {
				refactoring.setReplaceAll(true);
				assertTrue(refactoring.checkFinalConditions(new NullProgressMonitor())
					.hasWarning());
			});
	}

	@Test
	public void warnsWhenExtractingRepeatedConstructorCalls() throws Exception {
		withRefactoring("""
			System.out.println($new Object()$);
			System.out.println(new Object());
			""", refactoring -> {
				refactoring.setReplaceAll(true);
				assertTrue(refactoring.checkFinalConditions(new NullProgressMonitor())
					.hasWarning());
			});
	}

	@Test
	public void fallsBackForRepeatedExpressionStatements() throws Exception {
		withRefactoring("$System.nanoTime()$; System.out.println(System.nanoTime());",
			refactoring -> {
			refactoring.setReplaceAll(true);
			assertFalse(refactoring.checkFinalConditions(new NullProgressMonitor()).hasError());
		});
	}

	@Test
	public void ignoresMatchingExpressionWithoutStatementSuccessor() throws Exception {
		final var input = """
				property @java.lang.SuppressWarnings("foo") field: int
				System.out.println($"foo"$ + "foo");
				""";
		withRefactoring(input, refactoring -> {
				refactoring.setReplaceAll(true);
				refactoring.setReplaceInFile(true);
				assertFalse(refactoring.checkFinalConditions(new NullProgressMonitor()).hasError());
			});
		withRefactoring(input, refactoring -> {
			refactoring.setReplaceAll(true);
			assertFalse(refactoring.checkFinalConditions(new NullProgressMonitor()).hasError());
		});
	}

	@Test
	public void skipsMatchingStandaloneExpressionOutsideSelection() throws Exception {
		withRefactoring("""
			System.out.println($333333$ + 333333);
			333333;
			""", refactoring -> {
				refactoring.setReplaceAll(true);
				assertFalse(refactoring.checkFinalConditions(new NullProgressMonitor()).hasError());
			});
	}

	@Test
	public void separatesOccurrencesInsideLambdaFromOtherMethods() throws Exception {
		withRefactoring("""
			java.util.function.IntSupplier f = () -> 333333;
			System.out.println($333333$ + 333333);
			""", refactoring -> {
				refactoring.setReplaceAll(true);
				assertFalse(refactoring.checkFinalConditions(new NullProgressMonitor()).hasError());
			});
	}

	@Test
	public void warnsForRepeatedPostfixExpressions() throws Exception {
		withRefactoring("""
			int i = 0;
			System.out.println($i++$);
			System.out.println(i++);
			""", refactoring -> {
				refactoring.setReplaceAll(true);
				assertTrue(refactoring.checkFinalConditions(new NullProgressMonitor()).hasWarning());
			});
	}

	@Test
	public void wrapsSameLineStatement() throws Exception {
		assertAfterExtract("if (true) System.out.println($333333$ + 333333);", """
			if (true) {
				int i = 333333;
				System.out.println(i + i);
			}
			""".stripTrailing(), false, true, false, false);
	}

	@Test
	public void wrapsSameLineStatementWithoutSpaceAfterCondition() throws Exception {
		assertAfterExtract("if (true)System.out.println($333333$ + 333333);", """
			if (true) {
				int i = 333333;
				System.out.println(i + i);
			}
			""".stripTrailing(), false, true, false, false);
	}

	@Test
	public void rejectsVarForNullInitializer() throws Exception {
		assertVarUnavailable("System.out.println($null$);", null);
	}

	@Test
	public void rejectsVarForPolyAndVoidExpressions() throws Exception {
		assertVarUnavailable("int[] values = ${1, 2}$;", XJArrayLiteral.class);
		assertVarUnavailable("System.out.$println(1)$;", XMemberFeatureCall.class);
	}

	@Test
	public void rejectsVarForLambdaAndMethodReference() throws Exception {
		assertVarUnavailable("java.util.function.IntUnaryOperator f = $(var n) -> n$;",
			org.eclipse.xtext.xbase.XClosure.class);
		withRefactoring("System.out.println($333333$);", refactoring -> {
			final var expressionField = refactoring.getClass().getDeclaredField("expression");
			expressionField.setAccessible(true);
			expressionField.set(refactoring, JbaseFactory.eINSTANCE.createXJMethodReference());
			assertFalse(refactoring.canUseVar());
			expressionField.set(refactoring, JbaseFactory.eINSTANCE.createXJArrayLiteral());
			assertFalse(refactoring.canUseVar());
		});
	}

	@Test
	public void recognizesAssignmentNodesAsSideEffecting() throws Exception {
		withRefactoring("System.out.println($333333$);", refactoring -> {
			final var method = refactoring.getClass().getDeclaredMethod("isSideEffecting",
				XExpression.class);
			method.setAccessible(true);
			assertTrue((Boolean) method.invoke(refactoring,
				XbaseFactory.eINSTANCE.createXAssignment()));
		});
	}

	private static String normalizeNewlines(final String text) {
		return text.replace("\r", "");
	}

	private void assertVarUnavailable(final String input,
			final Class<? extends XExpression> expectedType) throws Exception {
		final var file = pluginProjectHelper.createTestFile(input.replace("$", ""));
		final var editor = pluginProjectHelper.openEditor(file);
		try {
			editor.getDocument().readOnly(resource -> {
				final var offset = input.indexOf('$');
				final var length = input.lastIndexOf('$') - offset - 1;
				var selection = util.findSelectedExpression(resource,
					new TextSelection(offset, length));
				if (selection == null && expectedType != null) {
					selection = EcoreUtil2.getAllContentsOfType(
						resource.getContents().getFirst(), XExpression.class).stream()
						.filter(expectedType::isInstance).findFirst().orElse(null);
				}
					assertNotNull(input, selection);
				final var refactoring = (jbase.ui.refactoring.JbaseExtractVariableRefactoring)
					refactoringProvider.get();
				refactoring.initialize(editor, selection);
				assertFalse(refactoring.canUseVar());
				refactoring.setUseVar(true);
				assertTrue(refactoring.checkFinalConditions(new NullProgressMonitor())
					.hasFatalError());
				return null;
			});
		} finally {
			editor.close(false);
		}
	}

	@FunctionalInterface
	private interface RefactoringCheck {
		void check(jbase.ui.refactoring.JbaseExtractVariableRefactoring refactoring)
			throws Exception;
	}

	private void withRefactoring(final String input, final RefactoringCheck check)
			throws Exception {
		final var file = pluginProjectHelper.createTestFile(input.replace("$", ""));
		final var editor = pluginProjectHelper.openEditor(file);
		try {
			editor.getDocument().readOnly(resource -> {
				final var offset = input.indexOf('$');
				final var length = input.lastIndexOf('$') - offset - 1;
				final var expression = util.findSelectedExpression(resource,
					new TextSelection(offset, length));
				final var refactoring = (jbase.ui.refactoring.JbaseExtractVariableRefactoring)
					refactoringProvider.get();
				assertTrue(refactoring.initialize(editor, expression));
				check.check(refactoring);
				return null;
			});
		} finally {
			editor.close(false);
		}
	}

	@Test
	public void usesJbaseHandlerForExtractLocalCommand() {
		assertEquals("jbase.ui.refactoring.internal.JbaseExtractVariableHandler",
			handlerProvider.get().getClass().getName());
	}

	@Test
	public void showsJdtOptionsWithJdtDefaults() throws Exception {
		final var preferences = InstanceScope.INSTANCE.getNode("jbase.ui.extractLocalVariable");
		final var keys = new String[] {"replaceAll", "replaceInFile", "declareFinal", "useVar"};
		final var previous = Arrays.stream(keys).map(key -> preferences.get(key, null))
			.toArray(String[]::new);
		for (final var key : keys) {
			preferences.remove(key);
		}
		final var file = pluginProjectHelper.createTestFile("System.out.println(333333);");
		final var editor = pluginProjectHelper.openEditor(file);
		try {
			editor.getDocument().readOnly(resource -> {
				final var selection = util.findSelectedExpression(resource,
					new TextSelection("System.out.println(".length(), 6));
				final var refactoring = (jbase.ui.refactoring.JbaseExtractVariableRefactoring)
					refactoringProvider.get();
				assertTrue(refactoring.initialize(editor, selection));
				final var shell = new Shell(Display.getCurrent());
				try {
					final var wizard = new JbaseExtractVariableWizard(refactoring);
					wizard.addPages();
					final var page = (InputPage) wizard.getPages()[0];
					page.createControl(shell);
					final var buttons = Arrays.stream(
						((org.eclipse.swt.widgets.Composite) page.getControl()).getChildren())
						.filter(Button.class::isInstance).map(Button.class::cast).toList();
					assertEquals(4, buttons.size());
					assertEquals("&Replace all occurrences of the selected expression with "
						+ "references to the local variable", buttons.get(0).getText());
					assertEquals("&Extract similar expression in all methods", buttons.get(1).getText());
					assertEquals("&Declare the local variable as 'final'", buttons.get(2).getText());
					assertEquals("Declare the local variable &type as 'var'", buttons.get(3).getText());
					assertTrue(buttons.get(0).getSelection());
					assertFalse(buttons.get(1).getSelection());
					assertFalse(buttons.get(2).getSelection());
					assertFalse(buttons.get(3).getSelection());
					assertTrue(buttons.get(3).isEnabled());
					buttons.get(0).setSelection(false);
					buttons.get(0).notifyListeners(SWT.Selection, new Event());
					assertFalse(buttons.get(1).isEnabled());
				} finally {
					shell.dispose();
				}
				return null;
			});
		} finally {
			editor.close(false);
			for (var i = 0; i < keys.length; i++) {
				if (previous[i] == null) {
					preferences.remove(keys[i]);
				} else {
					preferences.put(keys[i], previous[i]);
				}
			}
		}
	}

	protected void assertAfterExtract(final CharSequence input, final CharSequence expected,
			final boolean isFinal) throws Exception {
		assertAfterExtract(input, expected, isFinal, false, false, false);
	}

	protected void assertAfterExtract(final CharSequence input, final CharSequence expected,
			final boolean isFinal, final boolean replaceAll, final boolean replaceInFile,
			final boolean useVar) throws Exception {
		assertAfterExtract(input, expected, isFinal, replaceAll, replaceInFile, useVar,
			null, false);
	}

	protected void assertAfterExtract(final CharSequence input, final CharSequence expected,
			final boolean isFinal, final boolean replaceAll, final boolean replaceInFile,
			final boolean useVar, final String name, final boolean warning) throws Exception {
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
				final var jbaseRefactoring = (jbase.ui.refactoring.JbaseExtractVariableRefactoring)
					refactoring;
				jbaseRefactoring.setReplaceAll(replaceAll);
				jbaseRefactoring.setReplaceInFile(replaceInFile);
				jbaseRefactoring.setUseVar(useVar);
				if (name != null) {
					refactoring.setVariableName(name);
				}
				final var status = refactoring.checkAllConditions(new NullProgressMonitor());
				assertFalse(status.toString(), status.hasError());
				assertEquals(status.toString(), warning, status.hasWarning());
				return refactoring.createChange(new NullProgressMonitor())
						.perform(new NullProgressMonitor());
			});
			assertEquals(normalizeNewlines(expected.toString()),
				normalizeNewlines(editor.getDocument().get()));
		} finally {
			editor.close(false);
		}
	}
}
