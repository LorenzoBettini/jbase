package org.eclipse.xtext.example.domainmodel.tests

import com.google.inject.Inject
import org.eclipse.xtext.example.domainmodel.DomainmodelInjectorProvider
import org.eclipse.xtext.example.domainmodel.domainmodel.DomainModel
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.util.ReplaceRegion
import org.eclipse.xtext.xbase.imports.ImportOrganizer
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

/**
 * @author Jan Koehnlein - copied and adapted from Xtend
 */
@RunWith(typeof(XtextRunner))
@InjectWith(typeof(DomainmodelInjectorProvider))
class OrganizeImportsTest {
	
	@Inject extension ParseHelper<DomainModel> 
	@Inject ImportOrganizer importOrganizer

	def protected assertIsOrganizedTo(CharSequence model, CharSequence expected) {
		val domainModel = parse(model.toString)
		val changes = importOrganizer.getOrganizedImportChanges(domainModel.eResource as XtextResource)
		val builder = new StringBuilder(model)
		val sortedChanges= changes.sortBy[offset]
		var ReplaceRegion lastChange = null
		for(it: sortedChanges) {
			if(lastChange !== null && lastChange.endOffset > offset)
				fail("Overlapping text edits: " + lastChange + ' and ' +it)
			lastChange = it
		}
		for(it: sortedChanges.reverse)
			builder.replace(offset, offset + length, text)
		assertEquals(expected.toString, builder.toString)
	}

	@Test def testSimple() {
		'''
			package foo {
				entity Foo extends java.io.Serializable {}
			}
		'''.assertIsOrganizedTo('''
			import java.io.Serializable;
			
			package foo {
				entity Foo extends Serializable {}
			}
		''')
	}

	@Test def testDefaultPackage() {
		'''
			entity Foo extends java.io.Serializable {}
		'''.assertIsOrganizedTo('''
			import java.io.Serializable;

			entity Foo extends Serializable {}
		''')
	}

	@Test def testDefaultPackageLeadingWhitespace() {
		'''
			«»
			   	
			entity Foo extends java.io.Serializable {}
		'''.assertIsOrganizedTo('''
			import java.io.Serializable;

			entity Foo extends Serializable {}
		''')
	}

	@Test def testDefaultPackageWithJavaDoc() {
		'''
			/**
			 * some doc
			 */
			entity Foo extends java.io.Serializable {}
		'''.assertIsOrganizedTo('''
			import java.io.Serializable;

			/**
			 * some doc
			 */
			entity Foo extends Serializable {}
		''')
	}

	@Test def testGetOrganizedImportSection_01() {
		'''
			import java.lang.String;
			import java.util.List;

			entity Foo {
			  op test(List<String> s) : void {}
			}
		'''.assertIsOrganizedTo('''
			import java.util.List;

			entity Foo {
			  op test(List<String> s) : void {}
			}
		''')
	}

	@Test def testGetOrganizedImportSection_02() {
		'''
			import java.lang.String;
			import java.util.List;
			import java.util.List;
			import java.util.List;
			import java.lang.Integer;

			entity Foo {
			  op test(List<String> s) : void {}
			}
		'''.assertIsOrganizedTo('''
			import java.util.List;

			entity Foo {
			  op test(List<String> s) : void {}
			}
		''')
	}

	@Test def testGetOrganizedImportSection_03() {
		'''
			import java.util.*;

			entity Foo {
			  op test(List<String> s) : void{
			    final List<String> x = newArrayList("foo","bar");
			    Collections.sort(x);
			  }
			}
		'''.assertIsOrganizedTo('''
			import java.util.Collections;
			import java.util.List;

			entity Foo {
			  op test(List<String> s) : void{
			    final List<String> x = newArrayList("foo","bar");
			    Collections.sort(x);
			  }
			}
		''')
	}

	@Test def testGetOrganizedImportSection_04() {
		'''
			import java.util.*;
			import java.io.*;

			entity Foo {
			  op test(List<String> s) : void {
			    final List<String> x = new ArrayList<Map<StringBuilder,? extends Serializable>>();
			  }
			}
		'''.assertIsOrganizedTo('''
			import java.io.Serializable;
			import java.util.ArrayList;
			import java.util.List;
			import java.util.Map;

			entity Foo {
			  op test(List<String> s) : void {
			    final List<String> x = new ArrayList<Map<StringBuilder,? extends Serializable>>();
			  }
			}
		''')
	}

	@Test def testInnerClasses_01() {
		'''
			entity Foo {
			  op test(org.eclipse.emf.ecore.resource.Resource$Factory a, org.eclipse.emf.ecore.resource.Resource$Factory$Registry b) : void {}
			}
		'''.assertIsOrganizedTo('''
			import org.eclipse.emf.ecore.resource.Resource;

			entity Foo {
			  op test(Resource.Factory a, Resource.Factory.Registry b) : void {}
			}
		''')
	}

	@Test def testInnerClasses_02() {
		'''
			import org.eclipse.emf.ecore.resource.Resource;
			import org.eclipse.emf.ecore.EPackage;

			entity Foo {
			  op test() : void {
			    Object x = Resource$Factory$Registry.INSTANCE;
			    Object y = EPackage$Registry.INSTANCE;
			  }
			}
		'''.assertIsOrganizedTo('''
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.resource.Resource;

			entity Foo {
			  op test() : void {
			    Object x = Resource.Factory.Registry.INSTANCE;
			    Object y = EPackage.Registry.INSTANCE;
			  }
			}
		''')
	}

	@Test def testInnerClasses_03() {
		'''
			entity Foo {
			  op test() : boolean {
				 return org.eclipse.emf.ecore.resource.Resource$Factory.class == org.eclipse.emf.ecore.resource.Resource$Factory$Registry.class;
			  }
			}
		'''.assertIsOrganizedTo('''
			import org.eclipse.emf.ecore.resource.Resource;

			entity Foo {
			  op test() : boolean {
				 return Resource.Factory.class == Resource.Factory.Registry.class;
			  }
			}
		''')
	}

	@Test def testInnerClasses_04() {
		'''
			import org.eclipse.emf.ecore.resource.Resource;
			import org.eclipse.emf.ecore.EPackage;

			entity Foo {
			  op test() : void {
			    Resource$Factory$Registry.class == EPackage$Registry.class;
			  }
			}
		'''.assertIsOrganizedTo('''
			import org.eclipse.emf.ecore.EPackage;
			import org.eclipse.emf.ecore.resource.Resource;

			entity Foo {
			  op test() : void {
			    Resource.Factory.Registry.class == EPackage.Registry.class;
			  }
			}
		''')
	}

	@Test def testInnerClasses_05() {
		'''
			import org.eclipse.emf.ecore.resource.Resource$Factory$Registry;

			entity Foo {
			  op test() : void {
			    Registry.class;
			  }
			}
		'''.assertIsOrganizedTo('''
			import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;

			entity Foo {
			  op test() : void {
			    Registry.class;
			  }
			}
		''')
	}

	@Test def testNameClashSameFileWins_1() {
		'''
			package foo {
				entity Foo {
					x : java.lang.String
				}

				entity String {}
			}
		'''.assertIsOrganizedTo('''
			package foo {
				entity Foo {
					x : java.lang.String
				}

				entity String {}
			}
		''')
	}

	@Test def testNameClashSameFileWins() {
		'''
			package foo {
				entity Foo extends java.io.Serializable{}

				entity Serializable {}
			}
		'''.assertIsOrganizedTo('''
			package foo {
				entity Foo extends java.io.Serializable{}

				entity Serializable {}
			}
		''')
	}

	@Test def testNameClashOrder_01() {
		'''
			entity Foo {
			  op test(java.util.List<String> s) : java.awt.List {
			    return null;
			  }
			}
		'''.assertIsOrganizedTo('''
			import java.awt.List;

			entity Foo {
			  op test(java.util.List<String> s) : List {
			    return null;
			  }
			}
		''')
	}

	@Test def testNameClashOrder_02() {
		'''
			entity Foo {
			  op test(java.awt.List<String> s) : java.util.List  {
			    return null;
			  }
			}
		'''.assertIsOrganizedTo('''
			import java.util.List;

			entity Foo {
			  op test(java.awt.List<String> s) : List  {
			    return null;
			  }
			}
		''')
	}

	@Test def testNameClashMoreCommon() {
		'''
			entity Foo {
			  op test(java.awt.List s) : java.util.List<String> {
			  	new java.awt.List();
			    return null;
			  }
			}
		'''.assertIsOrganizedTo('''
			import java.awt.List;

			entity Foo {
			  op test(List s) : java.util.List<String> {
			  	new List();
			    return null;
			  }
			}
		''')
	}

	@Test def testNameClashInnerClasses() {
		'''
			import org.eclipse.xtext.xbase.XbasePackage;
			import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotationsPackage;

			entity Foo {
			  op test(XbasePackage$Literals x, XAnnotationsPackage$Literals y) : void {
			  }
			}
		'''.assertIsOrganizedTo('''
			import org.eclipse.xtext.xbase.XbasePackage;
			import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotationsPackage;

			entity Foo {
			  op test(XbasePackage.Literals x, XAnnotationsPackage.Literals y) : void {
			  }
			}
		''')
	}


	@Test def testNameClashInnerClassesWithPreference() {
		'''
			import org.eclipse.xtext.xbase.XbasePackage$Literals;
			import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotationsPackage;

			entity Foo {
			  op test(Literals x, XAnnotationsPackage$Literals y) : void {
			  }
			}
		'''.assertIsOrganizedTo('''
			import org.eclipse.xtext.xbase.XbasePackage.Literals;
			import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotationsPackage;

			entity Foo {
			  op test(Literals x, XAnnotationsPackage.Literals y) : void {
			  }
			}
		''')
	}

	@Test def testArrayType() {
		'''
			import java.io.Serializable;

			package foo.bar {
				entity Foo {
				  op test() : Serializable[][] {
				    return null;
				  }
				}
			}
		'''.assertIsOrganizedTo('''
			import java.io.Serializable;

			package foo.bar {
				entity Foo {
				  op test() : Serializable[][] {
				    return null;
				  }
				}
			}
		''')
	}

	@Test def testClassWithSameName() {
		'''
			package foo.bar {
				entity Serializable {
				  clazz : Class<? extends java.io.Serializable>
				}
			}
		'''.assertIsOrganizedTo('''
			package foo.bar {
				entity Serializable {
				  clazz : Class<? extends java.io.Serializable>
				}
			}
		''')
	}

	@Test def testJavaDoc() {
		'''
			/**
			 * {@link java.util.List}
			 */
			entity Foo {}
		'''.assertIsOrganizedTo('''
			import java.util.List;
			
			/**
			 * {@link List}
			 */
			entity Foo {}
		''')
	}
	
	@Test def testLocalNameClash() {
		'''
			package foo {
				entity Referrer {
					foo0: foo.bar.Foo
					foo1: foo.baz.Foo
				}
				package bar {
					entity Foo {
						self: Foo
					}
				}
				package baz {
					entity Foo {
						self: Foo
					}
				}
			}
		'''.assertIsOrganizedTo('''
			package foo {
				entity Referrer {
					foo0: foo.bar.Foo
					foo1: foo.baz.Foo
				}
				package bar {
					entity Foo {
						self: Foo
					}
				}
				package baz {
					entity Foo {
						self: Foo
					}
				}
			}
		''')
	}
	
	@Test def testSamePackage() {
		'''
			package bar {
				entity Foo {}
				entity Bar {
					foo: bar.Foo
				}
			}
		'''.assertIsOrganizedTo('''
			package bar {
				entity Foo {}
				entity Bar {
					foo: Foo
				}
			}
		''')
	}
	
	@Test def testSuperPackage() {
		'''
			package bar {
				entity Foo {}
				package baz {
					entity Bar {
						foo: bar.Foo
					}
				}
			}
		'''.assertIsOrganizedTo('''
			import bar.Foo;
			
			package bar {
				entity Foo {}
				package baz {
					entity Bar {
						foo: Foo
					}
				}
			}
		''')
	}
	
	@Test def testSubPackage() {
		'''
			import bar.Foo;
			
			package bar {
				entity Foo {
					bar : bar.baz.Bar
				}
				package baz {
					entity Bar {}
				}
			}
		'''.assertIsOrganizedTo('''
			import bar.baz.Bar;
			
			package bar {
				entity Foo {
					bar : Bar
				}
				package baz {
					entity Bar {}
				}
			}
		''')
	}
	
}