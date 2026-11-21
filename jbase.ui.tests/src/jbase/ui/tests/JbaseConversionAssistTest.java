package jbase.ui.tests;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.xtext.ui.editor.quickfix.QuickAssistInvocationContext;

import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.quickfix.XtextQuickAssistProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider;
import jbase.ui.refactoring.internal.JbaseConversionAssistProcessor;
import jbase.tests.util.ui.PluginProjectHelper;

/** Verifies that Jbase editors receive the conversion assist processor. */
@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageUiInjectorProvider.class)
public class JbaseConversionAssistTest {
	@Inject private XtextQuickAssistProcessor processor;
	@Inject private PluginProjectHelper projects;

	@Test
	public void processorIsBoundForEditors() {
		assertTrue(processor instanceof JbaseConversionAssistProcessor);
		assertTrue(processor.canAssist(null));
	}

	@Test
	public void offersAndAppliesReferenceConversion() throws Exception {
		assertAssist("java.util.function.ToIntFunction<String> f = String::length;",
			"length", "Convert method reference to lambda", "(arg0) -> arg0.length()");
	}

	@Test
	public void offersAndAppliesLambdaConversion() throws Exception {
		assertAssist("java.util.function.ToIntFunction<String> f = s -> s.length();",
			"length", "Convert lambda to method reference", "java.lang.String::length");
	}

	private void assertAssist(final String source, final String cursorText,
			final String label, final String replacement) throws Exception {
		projects.createJavaPluginProject();
		final var file = projects.createTestFile(source);
		final var editor = projects.openEditor(file);
		try {
			final var viewer = (ISourceViewer) Proxy.newProxyInstance(
				ISourceViewer.class.getClassLoader(), new Class<?>[] { ISourceViewer.class },
				(proxy, method, arguments) -> switch (method.getName()) {
					case "getDocument" -> editor.getDocument();
					case "getAnnotationModel" -> editor.getDocumentProvider()
						.getAnnotationModel(editor.getEditorInput());
					default -> null;
				});
			final var context = new QuickAssistInvocationContext(viewer,
				source.indexOf(cursorText), 0, true);
			final var proposals = processor.computeQuickAssistProposals(context);
			final var conversion = java.util.Arrays.stream(proposals)
				.filter(proposal -> label.equals(proposal.getDisplayString()))
				.findFirst().orElseThrow();
			conversion.apply(editor.getDocument());
			assertTrue(editor.getDocument().get().contains(replacement));
		} finally {
			editor.close(false);
		}
	}
}
