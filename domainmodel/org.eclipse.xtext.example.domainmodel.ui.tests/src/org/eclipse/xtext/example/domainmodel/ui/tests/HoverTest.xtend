/*******************************************************************************
 * Copyright (c) 2018, 2026 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.example.domainmodel.ui.tests

import com.google.inject.Inject
import org.eclipse.jface.text.Region
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.ui.testing.AbstractHoverTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil.createJavaProject

/**
 * @author miklossy - Initial contribution and API
 */
@RunWith(XtextRunner)
@InjectWith(DomainmodelUiInjectorProvider)
class HoverTest extends AbstractHoverTest {

	@Inject ValidationTestHelper validationTestHelper

	@Before def void setup() throws Exception {
		/*
		 * Xbase-based languages require java project
		 */
		 projectName.createJavaProject
	}

	@Test def hover_over_import_statement() {
		'''
			import java.util.List

			entity Foo {
				bar : List<String>
			}
		'''.hasHoverOver("java.util.List", '''An ordered collection''')
	}

	@Test def hover_over_link_in_javadoc() {
		'''
			/**
			 * {@link java.util.List}
			 */
			 entity Foo {}
		'''.hasHoverOver("java.util.List", '''An ordered collection''')
	}

	@Test def hover_over_java_typed_property() {
		'''
			entity Foo {
				name : Object
			}
		'''.hasHoverOver("name", '''Property name : Object''')
	}

	@Test def hover_over_java_type() {
		'''
			entity Foo {
				name : Object
			}
		'''.hasHoverOver("Object", '''</code> is the root of the class hierarchy.''')
	}

	@Test def hover_over_entity_typed_property() {
		'''
			entity Foo {
				b : Bar
			}

			entity Bar {}
		'''.hasHoverOver("b", '''Property b : Bar''')
	}

	@Test def hover_over_entity_type() {
		'''
			entity Foo {
				b : Bar
			}

			/**
			 * Documentation of the entity Bar.
			 */
			entity Bar {}
		'''.hasHoverOver("Bar", '''Documentation of the entity Bar.''')
	}

	@Test def hover_over_operation() {
		'''
			entity Foo {
				b : String
				op getB() {
					return b;
				}
			}
		'''.hasValidHoverOver("getB", '''getB() : String''')
	}

	@Test def hover_over_operation_with_explicit_return_type() {
		'''
			entity Foo {
				b : String
				op setB(String b) : void {
					this.b = b;
				}
			}
		'''.hasValidHoverOver("setB", '''setB(String b) : void''')
	}

	private def void hasValidHoverOver(CharSequence source, String hoverText, String expected) {
		val file = dslFile(projectName, fileName, fileExtension, source)
		val editor = openInEditor(file)
		editor.document.readOnly [ resource |
			validationTestHelper.assertNoErrors(resource.contents.head)
			null
		]
		val region = new Region(source.toString.indexOf(hoverText), hoverText.length)
		hoverPopupHasContent(hoveringOver(file, region), expected)
	}

}
