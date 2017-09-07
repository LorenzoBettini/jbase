package jbase.ui.tests

import com.google.inject.Inject
import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider
import jbase.tests.util.ui.PDETargetPlatformUtils
import jbase.tests.util.ui.PluginProjectHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseTestlanguageUiInjectorProvider))
class JbaseWorkbenchTest extends AbstractWorkbenchTest {

	@Inject extension PluginProjectHelper

	@BeforeClass
	def static void beforeClass() {
		PDETargetPlatformUtils.setTargetPlatform();
	}

	@Before
	override void setUp() {
		createJavaPluginProject
	}

	@Test def void testWorkbenchIntegration() {
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

	@Test
	def void testErrorInGeneratedJavaCode() {
		createTestFile(
'''
int a = 2;
int b = 2;
int c = 2;
if (a==b==c==2) {
	System.out.println("TRUE");
}
'''
		)
		
		cleanBuild
		waitForBuild
		// one error in the generated Java file, and one in the original file
		assertErrors(
		'''
		Java problem: Incompatible operand types Boolean and Integer
		Incompatible operand types Boolean and Integer'''
		)
	}

}