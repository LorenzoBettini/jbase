package jbase.smoke.tests;

import jbase.tests.JbaseParsingTest;
import org.eclipse.xtext.testing.smoketest.ProcessedBy;
import org.eclipse.xtext.testing.smoketest.XtextSmokeTestRunner;
import org.eclipse.xtext.xbase.testing.typesystem.TypeSystemSmokeTester;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(XtextSmokeTestRunner.class)
@ProcessedBy(value = TypeSystemSmokeTester.class, processInParallel = true)
@Suite.SuiteClasses(JbaseParsingTest.class)
public class JbaseTypeSystemSmoke {
}
