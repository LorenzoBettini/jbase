package jbase.tests.util.ui

import com.google.inject.Inject
import com.google.inject.Provider
import java.util.List
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.Status
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.ui.IEditorPart
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.PartInitException
import org.eclipse.ui.internal.ErrorEditorPart
import org.eclipse.ui.part.FileEditorInput
import org.eclipse.xtext.resource.FileExtensionProvider
import org.eclipse.xtext.ui.XtextProjectHelper
import org.eclipse.xtext.ui.editor.XtextEditor
import org.eclipse.xtext.ui.editor.XtextEditorInfo
import org.eclipse.xtext.ui.editor.utils.EditorUtils
import org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil
import org.eclipse.xtext.ui.util.PluginProjectFactory

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*
import static org.junit.Assert.*

class PluginProjectHelper {

	@Inject Provider<PluginProjectFactory> projectFactoryProvider

	@Inject IWorkbench workbench;

	@Inject XtextEditorInfo editorInfo;

	@Inject FileExtensionProvider fileExtensionProvider;

	val static TEST_PROJECT = "TestProject"

	val static TEST_FILE = "TestFile"

	val defaultBuilderIds = newArrayList(JavaCore.BUILDER_ID, 
				"org.eclipse.pde.ManifestBuilder",
				"org.eclipse.pde.SchemaBuilder",
				XtextProjectHelper.BUILDER_ID)
	val defaultNatureIds = newArrayList(JavaCore.NATURE_ID, 
				"org.eclipse.pde.PluginNature", 
				XtextProjectHelper.NATURE_ID)
	val defaultRequiredBundles = newArrayList("org.eclipse.xtext.xbase.lib")

	def IJavaProject createJavaPluginProject() {
		createJavaPluginProject(TEST_PROJECT)
	}

	def IJavaProject createJavaPluginProject(String projectName) {
		createJavaPluginProject(projectName, defaultRequiredBundles)
	}

	def IJavaProject createJavaPluginProject(String projectName, List<String> requiredBundles) {
		createJavaPluginProject(projectName, requiredBundles, #[])
	}

	def IJavaProject createJavaPluginProject(String projectName, List<String> requiredBundles, List<String> exportedPackages) {
		createJavaPluginProject(projectName, requiredBundles, exportedPackages, 
			defaultBuilderIds, defaultNatureIds)
	}

	def IJavaProject createJavaPluginProject(String projectName, 
		List<String> requiredBundles, List<String> exportedPackages,
		List<String> builderIds, List<String> natureIds) {
		val projectFactory = projectFactoryProvider.get
		
		projectFactory.setProjectName(projectName);
		projectFactory.addFolders(newArrayList("src"));
		projectFactory.addFolders(newArrayList("src-gen"));
		projectFactory.addBuilderIds(builderIds);
		projectFactory.addProjectNatures(natureIds);
		projectFactory.addRequiredBundles(requiredBundles);
		projectFactory.addExportedPackages(exportedPackages)
		val result = projectFactory.createProject(new NullProgressMonitor(), null);
		JavaCore.create(result);
		return JavaProjectSetupUtil.findJavaProject(projectName);
	}

	def String getEditorID() {
		return editorInfo.getEditorId();
	}

	def XtextEditor openEditor(IFile file) throws Exception {
		val openEditor = openEditor(file, getEditorID());
		val xtextEditor = EditorUtils.getXtextEditor(openEditor);
		if (xtextEditor !== null) {
			xtextEditor.selectAndReveal(0, 0);
			return xtextEditor;
		} else if (openEditor instanceof ErrorEditorPart) {
			val field = openEditor.getClass().getDeclaredField("error");
			field.setAccessible(true);
			throw new IllegalStateException("Couldn't open the editor.",
					(field.get(openEditor) as Status).getException());
		} else {
			fail("Opened Editor with id:" + getEditorID() + ", is not an XtextEditor");
		}
		return null;
	}

	def IEditorPart openEditor(IFile file, String editorId) throws PartInitException {
		return workbench.getActiveWorkbenchWindow().getActivePage().openEditor(new FileEditorInput(file), editorId);
	}

	def createTestFile(CharSequence contents) {
		createFile(TEST_PROJECT + "/src/" +
			TEST_FILE + "." + fileExtensionProvider.getFileExtensions().iterator().next(),
			contents.toString
		)
	}

	def assertNoErrors() {
		val markers = getErrorMarkers()
		assertEquals(
			"unexpected errors:\n" +
			markers.map[getAttribute(IMarker.MESSAGE)].join("\n"),
			0, 
			markers.size
		)
	}

	def assertErrors(CharSequence expected) {
		val markers = getErrorMarkers()
		assertEqualsStrings(
			expected,
			markers.map[getAttribute(IMarker.MESSAGE)].join("\n")
		)
	}
	
	def getErrorMarkers() {
		root.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE).
			filter[
				getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR
			]
	}

	def protected assertEqualsStrings(CharSequence expected, CharSequence actual) {
		assertEquals(expected.toString().replaceAll("\r", ""), actual.toString());
	}

}