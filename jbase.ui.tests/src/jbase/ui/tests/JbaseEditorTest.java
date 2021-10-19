package jbase.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.monitor;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.root;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.reconciler.XtextReconciler;
import org.eclipse.xtext.ui.testing.AbstractEditorTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider;
import jbase.tests.util.ui.PluginProjectHelper;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageUiInjectorProvider.class)
public class JbaseEditorTest extends AbstractEditorTest {

	@Inject
	private PluginProjectHelper pluginProjectHelper;

	@Before
	@Override
	public void setUp() {
		this.pluginProjectHelper.createJavaPluginProject();
	}

	@Test
	public void testModificationInEditorDoesNotGenerateXtextReconcilerJobException() throws Exception {
		// see https://github.com/LorenzoBettini/jbase/issues/131
		String testProgram = "System.out.println(\"\");";
		IFile createTestFile = pluginProjectHelper.createTestFile(testProgram);
		waitForBuild();
		pluginProjectHelper.assertNoErrors();
		XtextEditor openEditor = openEditor(createTestFile);
		IDocument document = openEditor.getDocumentProvider()
				.getDocument(openEditor.getEditorInput());
		int offset = document.get().indexOf('"');
		document.replace(offset+1, 0, "h");
		assertEquals("System.out.println(\"h\");", document.get());
		Job.getJobManager().join(XtextReconciler.class.getName(), monitor());
		assertNoXtextReconcilerJobInLog();
		openEditor.doSave(monitor());
		waitForBuild();
		pluginProjectHelper.assertNoErrors();
	}

	private void assertNoXtextReconcilerJobInLog() throws IOException {
		IPath location = root().getLocation();
		Path logPath = Paths.get(location + "/.metadata/.log");
		if (!Files.exists(logPath))
			return;
		List<String> readAllLines = Files.readAllLines(logPath);
		for (String string : readAllLines) {
			if (string.contains("An internal error occurred during: \"XtextReconcilerJob\""))
				fail("found XtextReconcilerJob exception:\n" +
					readAllLines.stream().collect(Collectors.joining("\n")));
		}
	}
}
