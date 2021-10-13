package org.eclipse.xtext.example.domainmodel.tests

import com.google.inject.Inject
import org.eclipse.xtext.junit4.TemporaryFolder
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.lib.util.ReflectExtensions
import org.eclipse.xtext.xbase.testing.CompilationTestHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(DomainmodelInjectorProvider)
class CompilerTest {
	
	@Rule
	@Inject public TemporaryFolder temporaryFolder 
	@Inject extension CompilationTestHelper
	@Inject extension ReflectExtensions
	
	@Test
	def void testGeneratedJava() {
		'''
			entity Foo {
				name : String
				op doStuff(String x) : String {
					return x + " " + this.name;
				}
			}
		'''.compile [
			val obj = it.compiledClass.getConstructor().newInstance
			obj.invoke('setName', 'Foo')
			assertEquals("Hello Foo", obj.invoke('doStuff','Hello'))
		]
	}

	@Test
	def void testGeneratedJavaFromSeveralInputs() {
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
			val barObj = it.getCompiledClass("Bar").getConstructor().newInstance
			barObj.invoke('setName', 'Bar')
			val fooObj = it.getCompiledClass("Foo").getConstructor().newInstance
			fooObj.invoke('setBar', barObj)
			assertEquals("Hello Bar", fooObj.invoke('doStuff','Hello'))
		]
	}
	
	@Test
	def void compareGeneratedJava() {
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