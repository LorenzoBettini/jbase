package jbase.ui.tests

import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.junit4.ui.AbstractWorkbenchTest
import org.eclipse.xtext.ui.XtextProjectHelper
import org.junit.Test
import org.junit.runner.RunWith
import jbase.testlanguage.JbaseTestlanguageUiInjectorProvider

import static org.eclipse.xtext.junit4.ui.util.IResourcesSetupUtil.*
import static org.eclipse.xtext.junit4.ui.util.JavaProjectSetupUtil.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseTestlanguageUiInjectorProvider))
class JbaseWorkbenchTest extends AbstractWorkbenchTest {

	val static TEST_PROJECT = "TestProject"
	val static TEST_FILE = "TestFile"

	@Test def void testWorkbenchIntegration() {
		val project = createProject(TEST_PROJECT)
		makeJavaProject(project)
		addNature(project, XtextProjectHelper.NATURE_ID)
		createTestFile('''
			aString : String
			op m(String s) : String {
				char c = 'c';
				return s;
			}
			m("a string");
		''')
		reallyWaitForAutoBuild
		assertNoErrors
	}

	def private createTestFile(CharSequence contents) {
		createFile(TEST_PROJECT + "/src/" + TEST_FILE + ".jbasetestlanguage", contents.toString)
	}

	def private assertNoErrors() {
		val markers = root.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE).
			filter[
				getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR
			]
		assertEquals(
			"unexpected errors:\n" +
			markers.map[getAttribute(IMarker.LOCATION) + 
				", " + getAttribute(IMarker.MESSAGE)].join("\n"),
			0, 
			markers.size
		)
	}
}