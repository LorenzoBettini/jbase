package jbase.tests

import com.google.inject.Inject
import com.google.inject.MembersInjector
import jbase.jbase.XJJvmFormalParameter
import jbase.testlanguage.jvmmodel.JbaseTestlanguageJvmModelInferrer
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import org.junit.Test
import org.junit.runner.RunWith

/**
 * For compilation tests we use JbaseTestlanguage since we test also
 * parameters.
 */
@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseTestlanguageInjectorProvider))
class JbaseJvmModelGeneratorTest extends JbaseAbstractCompilerTest {

	/**
	 * We need to set a custom JvmModelInferrer for testing
	 */
	@Inject JvmModelAssociator associator

	/**
	 * We need this to use a custom JvmModelInferrer for testing
	 */
	@Inject extension JvmTypesBuilder jvmTypesBuilder

	/**
	 * We need this to use a custom JvmModelInferrer for testing
	 */
	@Inject MembersInjector<JbaseTestlanguageJvmModelInferrer> inferrerInjector

	@Test def void testArrayAssignElementFinalParameter() {
		arrayAssignElementFinalParameter.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void m(int[] a) {
    a[0] = 1;
  }

  public static void main(String[] args) throws Throwable {
  }
}
'''
			)
	}

	@Test def void testAssignToParam() {
		assignToParam.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void m(int a) {
    a = 1;
  }

  public static void main(String[] args) throws Throwable {
  }
}
'''
			)
	}

	@Test def void testAssignToParamAndReturn() {
		assignToParamAndReturn.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static int m(int a) {
    return a = 1;
  }

  public static void main(String[] args) throws Throwable {
  }
}
'''
			)
	}

	@Test def void testFinalParam() {
		'''
		op m(final int i) {
			return i;
		}
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static int m(final int i) {
    return i;
  }

  public static void main(String[] args) throws Throwable {
  }
}
'''
			)
	}

	@Test def void testParameterVarArgs() {
		'''
		op m(String... a) : void {
			for (int i = 0; i < a.length; i++)
				System.out.println(a[i]);
		}
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void m(String... a) {
    for (int i = 0; (i < a.length); i++) {
      System.out.println(a[i]);
    }
  }

  public static void main(String[] args) throws Throwable {
  }
}
'''
		)
	}

	@Test def void testParameterVarArgs2() {
		'''
		op m(int l, String... a) : void {
			for (int i = 0; i < l; i++)
				System.out.println(a[i]);
		}
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void m(int l, String... a) {
    for (int i = 0; (i < l); i++) {
      System.out.println(a[i]);
    }
  }

  public static void main(String[] args) throws Throwable {
  }
}
'''
		)
	}

	@Test def void testParameterVarArgsWithWrongInferrerTranslateParameter() {
		associator.inferrerProvider = [
			new JbaseTestlanguageJvmModelInferrer() {
				
				override protected inferParameter(JvmOperation it, XJJvmFormalParameter p) {
					// we intentionally forgot to set the array type reference
					// in the presence of varargs
					var parameterType = p.parameterType
					if (p.varArgs) {
						// varArgs is a property of JvmExecutable
						varArgs = p.varArgs
						// parameterType = parameterType.addArrayTypeDimension
					}
					parameters += p.toParameter(p.name, parameterType)
				}
				
			} => [ inferrerInjector.injectMembers(it) ]
		]
		'''
		op m(int l, String... a) : void {
			
		}
		'''.assertCompilesTo(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void m(int l, /* Internal Error: Parameter was vararg but not an array type. */... a) {
  }

  public static void main(String[] args) throws Throwable {
  }
}
'''
		)
		
		// reset the original inferrer
		associator.inferrerProvider = [
			new JbaseTestlanguageJvmModelInferrer() {
				
			} => [ inferrerInjector.injectMembers(it) ]
		]
	}

}
