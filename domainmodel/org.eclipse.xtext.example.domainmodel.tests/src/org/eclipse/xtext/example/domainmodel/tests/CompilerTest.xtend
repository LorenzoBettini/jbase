/*******************************************************************************
 * Copyright (c) 2012, 2019 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.example.domainmodel.tests

import com.google.inject.Inject
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.TemporaryFolder
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.lib.util.ReflectExtensions
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(Java21InjectorProvider)
class CompilerTest {

	@Rule
	@Inject public TemporaryFolder temporaryFolder
	@Inject extension CompilationTestHelper
	@Inject extension ReflectExtensions

	@Test def void testEntitySuperclassAndInterfaces() throws Exception {
		'''
			entity Foo extends java.util.ArrayList<String>
				implements java.util.function.IntSupplier, java.lang.Cloneable {
				op getAsInt():int { return size() + 40; }
			}
		'''.compile [
			assertTrue(errorsAndWarnings.toString, errorsAndWarnings.empty)
			assertEquals('''
				import java.util.ArrayList;
				import java.util.function.IntSupplier;
				import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
				import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

				@SuppressWarnings("all")
				public class Foo extends ArrayList<String> implements IntSupplier, Cloneable {
				  public Foo() {
				  }

				  public Foo(Procedure1<Foo> initializer) {
				    initializer.apply(this);
				  }

				  public int getAsInt() {
				    int _size = this.size();
				    return (_size + 40);
				  }

				  @Override
				  public String toString() {
				    String result = new ToStringBuilder(this).addAllFields().toString();
				    return result;
				  }
				}
			'''.toString, singleGeneratedCode)
			val obj = compiledClass.getDeclaredConstructor().newInstance
			val values = obj as java.util.List<String>
			values.add("one")
			values.add("two")
			assertEquals(42, (obj as java.util.function.IntSupplier).asInt)
			assertTrue(obj instanceof Cloneable)
		]
	}

	@Test def void testLocalClassesCaptureParametersAndReuseProperties() throws Exception {
		'''
			entity Foo implements java.util.function.IntSupplier {
				op getAsInt():int { return calculate(3); }
				op calculate(int input):int {
					final var offset = 2;
					local Counter implements java.util.function.IntSupplier {
						value : int
						op getAsInt():int { return this.value + input + offset; }
					}
					Counter counter = new Counter();
					counter.setValue(37);
					return counter.getAsInt();
				}
			}
		'''.compile [
			assertTrue(errorsAndWarnings.toString, errorsAndWarnings.empty)
			val obj = compiledClass.getDeclaredConstructor().newInstance
			assertEquals(42, obj.invoke('calculate', 3))
			assertEquals(42, (obj as java.util.function.IntSupplier).asInt)
		]
	}

	@Test def void testLocalInheritanceAndNestedClasses() throws Exception {
		'''
			entity Foo {
				op calculate(int input):int {
					local Values extends java.util.ArrayList<String> {
						op self():Values { return this; }
						op result():int {
							local Nested { op get():int { return input + 2; } }
							return new Nested().get();
						}
					}
					return new Values().self().result();
				}
			}
		'''.compile [
			assertTrue(errorsAndWarnings.toString, errorsAndWarnings.empty)
			val obj = compiledClass.getDeclaredConstructor().newInstance
			assertEquals(42, obj.invoke('calculate', 40))
		]
	}

	@Test def void testAnonymousInterfaceCapturesLocals() throws Exception {
		'''
			import java.util.function.IntUnaryOperator;
			entity Foo {
				op calculate(int input):int {
					final var offset = 2;
					var add = new IntUnaryOperator() {
						@Override
						op applyAsInt(int value):int { return value + input + offset; }
					};
					return add.applyAsInt(4);
				}
			}
		'''.compile [
			val obj = compiledClass.getDeclaredConstructor().newInstance
			assertEquals(9, obj.invoke('calculate', 3))
		]
	}

	@Test def void testAnonymousPropertiesAndExplicitAccessors() throws Exception {
		'''
			entity Foo {
				op calculate():int {
					var counter = new Object() {
						value : int
						op getValue():int { return this.value + 1; }
					};
					counter.setValue(41);
					return counter.getValue();
				}
			}
		'''.compile [
			val obj = compiledClass.getDeclaredConstructor().newInstance
			assertEquals(42, obj.invoke('calculate'))
		]
	}

	@Test def void testNestedAnonymousClasses() throws Exception {
		'''
			import java.util.function.IntSupplier;
			entity Foo {
				op calculate(int input):int {
					var supplier = new IntSupplier() {
						op getAsInt():int {
							return new IntSupplier() {
								op getAsInt():int { return input + 1; }
							}.getAsInt();
						}
					};
					return supplier.getAsInt();
				}
			}
		'''.compile [
			val obj = compiledClass.getDeclaredConstructor().newInstance
			assertEquals(42, obj.invoke('calculate', 41))
		]
	}

	@Test def void testAnonymousSuperclassConstructorAndSuper() throws Exception {
		'''
			import java.util.ArrayList;
			entity Foo {
				op calculate():int {
					var values = new ArrayList<String>(10) {
						@Override
						op size():int { return super.size() + 1; }
					};
					values.add("hello");
					return values.size();
				}
			}
		'''.compile [
			val obj = compiledClass.getDeclaredConstructor().newInstance
			assertEquals(2, obj.invoke('calculate'))
		]
	}

	@Test def void testEmptyAnonymousClass() throws Exception {
		'''
			entity Foo {
				op create():Object { return new Object() {}; }
			}
		'''.compile [
			assertEquals('''
				import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
				import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

				@SuppressWarnings("all")
				public class Foo {
				  public Foo() {
				  }

				  public Foo(Procedure1<Foo> initializer) {
				    initializer.apply(this);
				  }

				  public Object create() {
				    return new Object() {
				    };
				  }

				  @Override
				  public String toString() {
				    String result = new ToStringBuilder(this).addAllFields().toString();
				    return result;
				  }
				}
			'''.toString, singleGeneratedCode)
			val obj = compiledClass.getDeclaredConstructor().newInstance
			assertTrue(obj.invoke('create').class.anonymousClass)
		]
	}

	@Test def void testJavaLambdaWithInferredLocals() throws Exception {
		'''
			import java.util.function.IntUnaryOperator;
			entity Foo {
				op calculate(int input):int {
					final var offset = 2;
					var add = (IntUnaryOperator) n -> {
						var result = n + offset;
						return result;
					};
					return add.applyAsInt(input);
				}
			}
		'''.compile [
			val obj = it.compiledClass.getDeclaredConstructor().newInstance
			assertEquals(5, obj.invoke('calculate', 3))
		]
	}

	@Test def void testGeneratedJava() throws Exception {
		'''
			entity Foo {
				name : String
				op doStuff(String x) : String {
					return x + " " + this.name;
				}
			}
		'''.compile [
			val obj = it.compiledClass.getDeclaredConstructor().newInstance
			obj.invoke('setName', 'Foo')
			assertEquals("Hello Foo", obj.invoke('doStuff','Hello'))
		]
	}

	@Test def void testGeneratedJavaFromSeveralInputs() throws Exception {
		#[
		'''
			entity Foo {
				bar : Bar
				op doStuff(String x) : String {
					return x + " " + bar.getName();
				}
			}
		''',
		'''
			entity Bar {
				name : String
			}
		'''
		].compile [
			val barObj = it.getCompiledClass("Bar").getDeclaredConstructor().newInstance
			barObj.invoke('setName', 'Bar')
			val fooObj = it.getCompiledClass("Foo").getDeclaredConstructor().newInstance
			fooObj.invoke('setBar', barObj)
			assertEquals("Hello Bar", fooObj.invoke('doStuff','Hello'))
		]
	}

	@Test def void testGeneratedJavaInheritedMethod() throws Exception {
		#[
		'''
			entity Foo extends Bar {
				// getName and setName inherited from Bar
				op doStuff(String x) : String {
					return x + " " + getName();
				}
			}
		''',
		'''
			entity Bar {
				name : String
			}
		'''
		].compile [
			val fooObj = it.getCompiledClass("Foo").getDeclaredConstructor().newInstance
			fooObj.invoke('setName', 'World')
			assertEquals("Hello World", fooObj.invoke('doStuff','Hello'))
		]
	}


	@Test def void compareGeneratedJava() throws Exception {
		'''
			entity Foo {
				name : String
			}
		'''.compile[assertEquals('''
			import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
			import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

			@SuppressWarnings("all")
			public class Foo {
			  public Foo() {
			  }

			  public Foo(Procedure1<Foo> initializer) {
			    initializer.apply(this);
			  }

			  private String name;

			  public String getName() {
			    return this.name;
			  }

			  public void setName(String name) {
			    this.name = name;
			  }

			  @Override
			  public String toString() {
			    String result = new ToStringBuilder(this).addAllFields().toString();
			    return result;
			  }
			}
		'''.toString, getSingleGeneratedCode)
		]
	}

	@Test def void testGeneratedJavaWithOverloadedMethods() throws Exception {
		'''
			entity Foo {
				op doStuff(String x) : String {
					return x;
				}
				op doStuff(int i) : String {
					return "int "+i;
				}
			}
		'''.compile [
			val obj = it.compiledClass.getDeclaredConstructor().newInstance
			assertEquals("Hello", obj.invoke('doStuff','Hello'))
			assertEquals("int 10", obj.invoke('doStuff',10))
		]
	}

	@Test def void testExplicitGetterReplacesGeneratedOne() throws Exception {
		'''
			entity Foo {
				name : String
				/** explicit getter will replace the generated one */
				op getName() {
					return name.toUpperCase();
				}
			}
		'''.compile[
			assertEquals('''
				import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
				import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

				@SuppressWarnings("all")
				public class Foo {
				  public Foo() {
				  }

				  public Foo(Procedure1<Foo> initializer) {
				    initializer.apply(this);
				  }

				  private String name;

				  public void setName(String name) {
				    this.name = name;
				  }

				  /**
				   * explicit getter will replace the generated one
				   */
				  public String getName() {
				    return this.name.toUpperCase();
				  }

				  @Override
				  public String toString() {
				    String result = new ToStringBuilder(this).addAllFields().toString();
				    return result;
				  }
				}
			'''.toString, getSingleGeneratedCode
			)
			// make sure it compiles in Java
			val obj = it.compiledClass.getDeclaredConstructor().newInstance
			obj.invoke('setName', "hello")
			assertEquals("HELLO", obj.invoke('getName'))
		]
	}

	@Test def void testExplicitSetterReplacesGeneratedOne() throws Exception {
		'''
			entity Foo {
				name : String
				/** explicit setter will replace the generated one,
					even if it's not void */
				op setName(String name) : String {
					return this.name = name.toUpperCase();
				}
			}
		'''.compile[
			assertEquals('''
				import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
				import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

				@SuppressWarnings("all")
				public class Foo {
				  public Foo() {
				  }

				  public Foo(Procedure1<Foo> initializer) {
				    initializer.apply(this);
				  }

				  private String name;

				  public String getName() {
				    return this.name;
				  }

				  /**
				   * explicit setter will replace the generated one,
				   * even if it's not void
				   */
				  public String setName(String name) {
				    return this.name = name.toUpperCase();
				  }

				  @Override
				  public String toString() {
				    String result = new ToStringBuilder(this).addAllFields().toString();
				    return result;
				  }
				}
			'''.toString, getSingleGeneratedCode
			)
			// make sure it compiles in Java
			val obj = it.compiledClass.getDeclaredConstructor().newInstance
			assertEquals("HELLO", obj.invoke('setName', "hello"))
			assertEquals("HELLO", obj.invoke('getName'))
		]
	}

	@Test def void testExplicitGetterSetterReplaceGeneratedOnes() throws Exception {
		'''
			entity Foo {
				name : String
				/** explicit getter will replace the generated one */
				op getName() {
					return name.toUpperCase();
				}
				/** explicit setter will replace the generated one,
					even if it's not void */
				op setName(String name) : String {
					return this.name = name.toUpperCase();
				}
			}
		'''.compile[
			assertEquals('''
				import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
				import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

				@SuppressWarnings("all")
				public class Foo {
				  public Foo() {
				  }

				  public Foo(Procedure1<Foo> initializer) {
				    initializer.apply(this);
				  }

				  private String name;

				  /**
				   * explicit getter will replace the generated one
				   */
				  public String getName() {
				    return this.name.toUpperCase();
				  }

				  /**
				   * explicit setter will replace the generated one,
				   * even if it's not void
				   */
				  public String setName(String name) {
				    return this.name = name.toUpperCase();
				  }

				  @Override
				  public String toString() {
				    String result = new ToStringBuilder(this).addAllFields().toString();
				    return result;
				  }
				}
			'''.toString, getSingleGeneratedCode
			)
			// make sure it compiles in Java
			val obj = it.compiledClass.getDeclaredConstructor().newInstance
			assertEquals("HELLO", obj.invoke('setName', "hello"))
			assertEquals("HELLO", obj.invoke('getName'))
		]
	}

	@Test
	def void testAnnotations() {
		'''
			import org.junit.Test;

			entity Foo {
				bar : String

				@Test
				op doStuff(String x) : String {
					return x + " " + bar;
				}
			}
		'''.compile[
			assertEquals(
			'''
			import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
			import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;
			import org.junit.Test;

			@SuppressWarnings("all")
			public class Foo {
			  public Foo() {
			  }

			  public Foo(Procedure1<Foo> initializer) {
			    initializer.apply(this);
			  }

			  private String bar;

			  public String getBar() {
			    return this.bar;
			  }

			  public void setBar(String bar) {
			    this.bar = bar;
			  }

			  @Test
			  public String doStuff(String x) {
			    return ((x + " ") + this.bar);
			  }

			  @Override
			  public String toString() {
			    String result = new ToStringBuilder(this).addAllFields().toString();
			    return result;
			  }
			}
			'''.toString, getSingleGeneratedCode)
			compiledClass
		]
	}
}
