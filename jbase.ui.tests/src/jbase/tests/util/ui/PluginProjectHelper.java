package jbase.tests.util.ui;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.ErrorEditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.XtextEditorInfo;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil;
import org.eclipse.xtext.ui.util.PluginProjectFactory;

public class PluginProjectHelper {

	@Inject
	private Provider<PluginProjectFactory> projectFactoryProvider;

	@Inject
	private IWorkbench workbench;

	@Inject
	private XtextEditorInfo editorInfo;

	@Inject
	private FileExtensionProvider fileExtensionProvider;

	private static final String TEST_PROJECT = "TestProject";

	private static final String TEST_FILE = "TestFile";

	private final List<String> defaultBuilderIds = List.of(JavaCore.BUILDER_ID,
			"org.eclipse.pde.ManifestBuilder", "org.eclipse.pde.SchemaBuilder",
			XtextProjectHelper.BUILDER_ID);

	private final List<String> defaultNatureIds = List.of(JavaCore.NATURE_ID,
			"org.eclipse.pde.PluginNature", XtextProjectHelper.NATURE_ID);

	private final List<String> defaultRequiredBundles = List.of("org.eclipse.xtext.xbase.lib");

	public IJavaProject createJavaPluginProject() {
		return createJavaPluginProject(PluginProjectHelper.TEST_PROJECT);
	}

	public IJavaProject createJavaPluginProject(final String projectName) {
		return createJavaPluginProject(projectName, defaultRequiredBundles);
	}

	public IJavaProject createJavaPluginProject(final String projectName,
			final List<String> requiredBundles) {
		return createJavaPluginProject(projectName, requiredBundles, List.of());
	}

	public IJavaProject createJavaPluginProject(final String projectName,
			final List<String> requiredBundles, final List<String> exportedPackages) {
		return createJavaPluginProject(projectName, requiredBundles, exportedPackages,
				defaultBuilderIds, defaultNatureIds);
	}

	public IJavaProject createJavaPluginProject(final String projectName,
			final List<String> requiredBundles, final List<String> exportedPackages,
			final List<String> builderIds, final List<String> natureIds) {
		final var projectFactory = projectFactoryProvider.get();
		projectFactory.setProjectName(projectName);
		projectFactory.addFolders(List.of("src"));
		projectFactory.addFolders(List.of("src-gen"));
		projectFactory.addBuilderIds(builderIds.toArray(String[]::new));
		projectFactory.addProjectNatures(natureIds.toArray(String[]::new));
		projectFactory.addRequiredBundles(requiredBundles);
		projectFactory.addExportedPackages(exportedPackages);
		final var result = projectFactory.createProject(new NullProgressMonitor(), null);
		JavaCore.create(result);
		return JavaProjectSetupUtil.findJavaProject(projectName);
	}

	public String getEditorID() {
		return editorInfo.getEditorId();
	}

	public XtextEditor openEditor(final IFile file) throws Exception {
		final var editor = openEditor(file, getEditorID());
		final var xtextEditor = EditorUtils.getXtextEditor(editor);
		if (xtextEditor != null) {
			xtextEditor.selectAndReveal(0, 0);
			return xtextEditor;
		} else if (editor instanceof ErrorEditorPart errorEditor) {
			final var field = errorEditor.getClass().getDeclaredField("error");
			field.setAccessible(true);
			throw new IllegalStateException("Couldn't open the editor.",
					((Status) field.get(errorEditor)).getException());
		}
		fail("Opened Editor with id:" + getEditorID() + ", is not an XtextEditor");
		return null;
	}

	public IEditorPart openEditor(final IFile file, final String editorId)
			throws PartInitException {
		return workbench.getActiveWorkbenchWindow().getActivePage()
				.openEditor(new FileEditorInput(file), editorId);
	}

	public IFile createTestFile(final CharSequence contents) throws Exception {
		final var extension = fileExtensionProvider.getFileExtensions().iterator().next();
		return IResourcesSetupUtil.createFile(TEST_PROJECT + "/src/" + TEST_FILE + "." + extension,
				contents.toString());
	}

	public void assertNoErrors() throws CoreException {
		final var markers = getErrorMarkers();
		assertEquals("unexpected errors:\n" + markerMessages(markers), 0, markers.size());
	}

	public void assertErrors(final CharSequence expected) throws CoreException {
		assertEqualsStrings(expected, markerMessages(getErrorMarkers()));
	}

	public List<IMarker> getErrorMarkers() throws CoreException {
		return Arrays
				.stream(IResourcesSetupUtil.root().findMarkers(IMarker.PROBLEM, true,
						IResource.DEPTH_INFINITE))
				.filter(marker -> marker.getAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR)
				.toList();
	}

	private String markerMessages(final List<IMarker> markers) throws CoreException {
		final var messages = new ArrayList<String>();
		for (final var marker : markers) {
			messages.add(String.valueOf(marker.getAttribute(IMarker.MESSAGE)));
		}
		return String.join("\n", messages);
	}

	protected void assertEqualsStrings(final CharSequence expected, final CharSequence actual) {
		assertEquals(expected.toString().replaceAll("\r", ""), actual.toString());
	}
}
