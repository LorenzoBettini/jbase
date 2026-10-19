package jbase.ui.tests;

import com.google.inject.Inject;
import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider;
import jbase.tests.util.ui.PluginProjectHelper;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractWorkbenchTest;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageUiInjectorProvider.class)
public class JbaseWorkbenchTest extends AbstractWorkbenchTest {

	@Inject
	private PluginProjectHelper pluginProjectHelper;

	@Before
	@Override
	public void setUp() {
		pluginProjectHelper.createJavaPluginProject();
	}

	@Test
	public void testWorkbenchIntegration() throws Exception {
		pluginProjectHelper.createTestFile("""
			aString : String
			op m(String s) : String {
				char c = 'c';
				return s;
			}
			m("a string");
			""");
		IResourcesSetupUtil.reallyWaitForAutoBuild();
		pluginProjectHelper.assertNoErrors();
	}

	@Test
	public void testErrorInGeneratedJavaCode() throws Exception {
		// one error in the generated Java file, and one in the original file
		pluginProjectHelper.createTestFile("""
			int a = 2;
			int b = 2;
			int c = 2;
			if (a==b==c==2) {
				System.out.println("TRUE");
			}
			""");
		IResourcesSetupUtil.cleanBuild();
		IResourcesSetupUtil.waitForBuild();
		pluginProjectHelper.assertErrors("""
			Java problem: Incompatible operand types Boolean and Integer
			Incompatible operand types Boolean and Integer\
			""");
	}
}
