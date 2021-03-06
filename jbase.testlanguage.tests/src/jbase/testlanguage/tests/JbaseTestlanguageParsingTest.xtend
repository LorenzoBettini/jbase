/*
 * generated by Xtext 2.10.0
 */
package jbase.testlanguage.tests

import com.google.inject.Inject
import jbase.testlanguage.jbaseTestlanguage.JbaseTestLanguageModel
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(JbaseTestlanguageInjectorProvider)
class JbaseTestlanguageParsingTest{

	@Inject
	ParseHelper<JbaseTestLanguageModel> parseHelper

	@Test 
	def void loadModel() {
		val result = parseHelper.parse('''
			System.out.println("Hello World!");
		''')
		Assert.assertNotNull(result)
	}

}
