/*******************************************************************************
 * Copyright (c) 2016, 2024 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.example.domainmodel.tests

import org.eclipse.xtext.formatting2.FormatterPreferenceKeys
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.formatter.AbstractFormatterTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(DomainmodelInjectorProvider)
class FormatterTest extends AbstractFormatterTest {

	@Test def void testAnonymousClass() {
		'''
			entity Foo { op create():Object {return new Object(){value:int op getValue():int{return this.value;}};} }
		'''.assertFormattedTo('''
			entity Foo {
				op create():Object {
					return new Object() {
						value:int
						op getValue():int {
							return this.value;
						}
					};
				}
			}
		''')
	}

	@Test def void testJavaLambdaWithInferredLocals() {
		'''
			import java.util.function.IntUnaryOperator;
			entity Foo { op calculate(int input):int {
			final var offset=2;
			IntUnaryOperator add=( int n )-> {var result=n+offset;return result;};
			return add.applyAsInt(input);
			} }
		'''.assertFormattedTo('''
			import java.util.function.IntUnaryOperator;

			entity Foo {
				op calculate(int input):int {
					final var offset = 2;
					IntUnaryOperator add = (int n) -> {
						var result = n + offset;
						return result;
					};
					return add.applyAsInt(input);
				}
			}
		''')
	}

	/**
	 * This example tests if the formatted document equals the unformatted document.
	 * This is the most convenient way to test a formatter.
	 */
	@Test def void example1() {
		'''
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
		'''.assertUnformattedEqualsFormatted
	}

	/**
	* This example tests whether a messy document is being formatted properly.
	* In contrast to the first example, this approach also allows to test formatting strategies that are input-aware.
	* Example: "Change newLines between tokens to be one at minimum, two at maximum."
	* Here, it depends on the formatters input document whether there will be one or two newLines on the output.
	*/
	@Test def void example2() {
		'''
			entity Foo {  op  foo  (  )  :  String  {  return "xx";  }  }
		'''.assertFormattedTo('''
			entity Foo {
				op foo():String {
					return "xx";
				}
			}
		''')
	}

	/**
	* This example shows how to test property-dependent formatting behavior.
	*/
	@Test def void example3() {
		'''
			entity Foo {
				op foo():String {
					return "xx";
				}
			}
		'''.assertFormattedTo('''
			entity Foo {
			 op foo():String {
			  return "xx";
			 }
			}
		''',[put(FormatterPreferenceKeys.indentation, " ")])
	}
	@Test def void testPackageWithInheritanceAndParameters() {
		'''
			package example {
				entity A {
					value:String
				}
				entity B extends A {
					op add(int a, int b):int {
						return a + b;
					}
					op inferred(int a) {
						return a;
					}
				}
			}
		'''.assertUnformattedEqualsFormatted
	}

}
