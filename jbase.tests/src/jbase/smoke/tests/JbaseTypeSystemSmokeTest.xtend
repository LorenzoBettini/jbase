package jbase.smoke.tests

import org.eclipse.xtext.junit4.smoketest.ProcessedBy
import org.eclipse.xtext.junit4.smoketest.XtextSmokeTestRunner
import org.eclipse.xtext.xbase.junit.typesystem.TypeSystemSmokeTester
import org.junit.runner.RunWith
import org.junit.runners.Suite.SuiteClasses
import jbase.tests.JbaseParsingTest

@RunWith(XtextSmokeTestRunner)
@ProcessedBy(
	value=TypeSystemSmokeTester,
	processInParallel=true
)
@SuiteClasses(
	JbaseParsingTest
)
class JbaseTypeSystemSmokeTest {
	
}