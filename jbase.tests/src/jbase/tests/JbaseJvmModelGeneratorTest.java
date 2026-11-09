package jbase.tests;

import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;
import com.google.inject.MembersInjector;
import jbase.compiler.JbaseJvmModelGenerator;
import jbase.jbase.XJJvmFormalParameter;
import jbase.testlanguage.jvmmodel.JbaseTestlanguageJvmModelInferrer;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.compiler.GeneratorConfig;
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * For compilation tests we use JbaseTestlanguage since we test also
 * parameters.
 */
@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseJvmModelGeneratorTest extends JbaseAbstractCompilerTest {

	/**
	 * We need to set a custom JvmModelInferrer for testing
	 */
	@Inject
	private JvmModelAssociator associator;

	/**
	 * We need this to use a custom JvmModelInferrer for testing
	 */
	@Inject
	private JvmTypesBuilder jvmTypesBuilder;

	/**
	 * We need this to use a custom JvmModelInferrer for testing
	 */
	@Inject
	private MembersInjector<JbaseTestlanguageJvmModelInferrer> inferrerInjector;

	@Inject
	private JbaseJvmModelGenerator generator;

	@Test
	public void genericInterfaceAndNestedTypes() {
		final var types = TypesFactory.eINSTANCE;
		final var parent = types.createJvmGenericType();
		parent.setSimpleName("Outer");
		final var nested = types.createJvmGenericType();
		nested.setSimpleName("Inner");
		nested.setInterface(true);
		nested.getTypeParameters().add(types.createJvmTypeParameter());
		nested.getTypeParameters().getFirst().setName("T");
		parent.getMembers().add(nested);
		final var resource = new ResourceImpl(URI.createURI("synthetic.jbase"));
		new ResourceSetImpl().getResources().add(resource);
		resource.getContents().add(parent);
		final var java = generator.generateType(parent, new GeneratorConfig()).toString();
		assertTrue(java.contains("class Outer"));
		assertTrue(java.contains("interface Inner<T>"));

		final var anonymous = types.createJvmGenericType();
		anonymous.setSimpleName("Anonymous");
		anonymous.setAnonymous(true);
		resource.getContents().add(anonymous);
		assertTrue(generator.generateType(anonymous, new GeneratorConfig()).toString()
			.contains("class Anonymous"));
	}

	@Test
	public void testArrayAssignElementFinalParameter() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.arrayAssignElementFinalParameter(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void m(int[] a) {
			    a[0] = 1;
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAssignToParam() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.assignToParam(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void m(int a) {
			    a = 1;
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAssignToParamAndReturn() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.assignToParamAndReturn(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int m(int a) {
			    return a = 1;
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testFinalParam() throws Exception {
		checkCompilation("""
			op m(final int i) {
				return i;
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int m(final int i) {
			    return i;
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testParameterVarArgs() throws Exception {
		checkCompilation("""
			op m(String... a) : void {
				for (int i = 0; i < a.length; i++)
					System.out.println(a[i]);
			}
			""", """
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
			""");
	}

	@Test
	public void testParameterVarArgs2() throws Exception {
		checkCompilation("""
			op m(int l, String... a) : void {
				for (int i = 0; i < l; i++)
					System.out.println(a[i]);
			}
			""", """
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
			""");
	}

	@Test
	public void testParameterVarArgsWithWrongInferrerTranslateParameter() throws Exception {
		// we intentionally forgot to set the array type reference
		// in the presence of varargs
		associator.setInferrerProvider(() -> {
			final var inferrer = new JbaseTestlanguageJvmModelInferrer() {

				@Override
				protected boolean inferParameter(final JvmOperation operation,
						final XJJvmFormalParameter parameter) {
					// we intentionally forgot to set the array type reference
					final var parameterType = parameter.getParameterType();
					if (parameter.isVarArgs()) {
						// varArgs is a property of JvmExecutable
						operation.setVarArgs(parameter.isVarArgs());
						// parameterType = parameterType.addArrayTypeDimension
					}
					return operation.getParameters().add(jvmTypesBuilder.toParameter(parameter,
							parameter.getName(), parameterType));
				}
			};
			inferrerInjector.injectMembers(inferrer);
			return inferrer;
		});
		try {
			checkCompilation("""
				op m(int l, String... a) : void {
				\t
				}
				""", """
				package jbasetestlanguage;

				@SuppressWarnings("all")
				public class MyFile {
				  public static void m(int l, /* Internal Error: Parameter was vararg but not an array type. */... a) {
				  }

				  public static void main(String[] args) throws Throwable {
				  }
				}
				""", false);
		} finally {
			associator.setInferrerProvider(() -> {
				final var inferrer = new JbaseTestlanguageJvmModelInferrer();
				inferrerInjector.injectMembers(inferrer);
				return inferrer;
			});
		}
	}
}
