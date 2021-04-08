package org.eclipse.xtext.example.domainmodel.tests

import com.google.inject.Inject
import org.eclipse.xtext.formatting2.FormatterPreferenceKeys
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.formatter.FormatterTestHelper
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(DomainmodelInjectorProvider)
class FormatterTest {

	@Inject extension FormatterTestHelper

	/**
	 * This example tests if the formatted document equals the unformatted document.
	 * This is the most convenient way to test a formatter.   
	 */
	@Test def void example1() {
		assertFormatted[
			toBeFormatted = '''
				entity Foo {
					propertyName:String
				
					op name() {
						int x = 1 + 2 + 4;
						{
							System.out.println();
							System.out.println();
							return null;
						}
					}
				}
			'''
		]
	}

	/**
 	* This example tests whether a messy document is being formatted properly. 
 	* In contrast to the first example, this approach also allows to test formatting strategies that are input-aware. 
 	* Example: "Change newLines between tokens to be one at minimum, two at maximum." 
 	* Here, it depends on the formatters input document whether there will be one or two newLines on the output.    
 	*/
	@Test def void example2() {
		assertFormatted[
			expectation = '''
				entity Foo {
					op foo():String {
						return "xx";
					}
				}
			'''
			toBeFormatted = '''
				entity Foo {  op  foo  (  )  :  String  {  return  "xx";  }  }
			'''
		]
	}

	/**
 	* This example shows how to test property-dependent formatting behavior.
 	*/
	@Test def void example3() {
		assertFormatted[
			preferences[
				put(FormatterPreferenceKeys.indentation, " ")
			]
			expectation = '''
				entity Foo {
				 op foo():String {
				  return "xx";
				 }
				}
			'''
			toBeFormatted = '''
				entity Foo {
					op foo():String {
						return "xx";
					}
				}
			'''
		]
	}
}
