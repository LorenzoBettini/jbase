package jbase.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.jbase.XJLocalClass;
import jbase.jvmmodel.JbaseAnonymousClassInferrer;
import jbase.scoping.JbaseImportSectionNamespaceScopeProvider;
import jbase.testlanguage.jbaseTestlanguage.JbaseTestLanguageModel;
import jbase.testlanguage.jbaseTestlanguage.JbaseTestlanguageFactory;
import jbase.testlanguage.validation.JbaseTestlanguageValidator;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import jbase.validation.JbaseIssueCodes;

/** Exercises host-defined local declarations through Jbase's shared JVM support. */
@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseLocalClassTest extends JbaseAbstractCompilerTest {
	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private JbaseImportSectionNamespaceScopeProvider namespaceScopes;
	@Inject
	private ISerializer serializer;
	@Inject
	private JbaseTestlanguageValidator validator;
	@Inject
	private JbaseAnonymousClassInferrer inferrer;
	@Inject
	private IJvmModelAssociator associator;
	@Inject
	private IJvmModelAssociations associations;

	@Before
	public void enableInitializationChecks() {
		validator.setSkipVariableInitializationCheck(false);
	}

	@Test
	public void emptyClass() throws Exception {
		checkCompilation("class Local {} Local value = new Local();", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    class Local {
			    }
			\s\s\s\s
			    Local value = new Local();
			  }
			}
			""");
	}

	@Test
	public void membersCaptureLocals() throws Exception {
		checkCompilation("""
			int offset = 40;
			class Local {
				property value: int
				method get(): int { return this.value + offset; }
			}
			Local value = new Local();
			value.value = 2;
			System.out.println(value.get());
			""", null);
	}

	@Test
	public void inheritanceAndSelfReferences() throws Exception {
		checkCompilation("""
			class Local extends java.util.ArrayList<String> {
				method self(): Local { return this; }
				method first(): String { return super.get(0); }
			}
			var values = new Local();
			values.add("hello");
			String first = values.self().first();
			""", null);
	}

	@Test
	public void nestedClasses() throws Exception {
		checkCompilation("""
			int offset = 40;
			class Outer {
				method get(): int {
					class Inner { method get(): int { return offset + 2; } }
					return new Inner().get();
				}
			}
			int result = new Outer().get();
			""", null);
	}

	@Test
	public void lexicalScope() throws Exception {
		assertInvalid("Local value = new Local(); class Local {}");
		assertInvalid("{ class Local {} } Local value = new Local();");
	}

	@Test
	public void capturesExecuteWithNativeLocalClassIdentity() throws Exception {
		assertExecuteMain("""
			method testMe(): void {
				int offset = 40;
				class Local implements java.util.function.IntSupplier {
					method getAsInt(): int { return offset + 2; }
				}
				var value = new Local();
				System.out.println(value.getAsInt());
				System.out.println(value.getClass().isLocalClass());
				System.out.println(value.getClass().getSimpleName());
			}
			""", "42\ntrue\nLocal\n");
	}

	@Test
	public void localClassesComposeWithAnonymousClassesAndLambdas() throws Exception {
		checkCompilation("""
			int offset = 40;
			class Local {
				method get(): int {
					var supplier = new java.util.function.IntSupplier() {
						method getAsInt(): int { return offset + 2; }
					};
					return supplier.getAsInt();
				}
			}
			java.util.function.IntSupplier lambda = () -> {
				class InLambda extends Local {}
				return new InLambda().get();
			};
			var anonymous = new Object() {
				method get(): int {
					class InAnonymous extends Local {}
					return new InAnonymous().get();
				}
			};
			int value = lambda.getAsInt() + anonymous.get();
			""", null);
	}

	@Test
	public void independentBlocksReuseNamesAndShadowImportedTypes() throws Exception {
		checkCompilation("""
			import java.util.ArrayList;
			{ class Local {} Local value = new Local(); }
			{ class Local {} Local value = new Local(); }
			class ArrayList { method size(): int { return 42; } }
			int result = new ArrayList().size();
			""", null);
	}

	@Test
	public void localClassesInLoopsSwitchesAndBlankCaptures() throws Exception {
		checkCompilation("""
			int n;
			if (System.nanoTime() > 0) n = 40; else n = 41;
			class Local {
				method get(): int { return n; }
				method blank(): int { int result; result = 2; return result; }
			}
			var anonymous = new Object() { method get(): int { return n; } };
			int captured = anonymous.get();
			for (int i = 0; i < 2; i++) {
				class InLoop extends Local {}
				int value = new InLoop().get();
			}
			int result = switch (1) {
				default -> {
					class InSwitch extends Local {}
					yield new InSwitch().get();
				}
			};
			switch (result) {
				default: class InColon extends Local {} System.out.println(new InColon().get());
			}
			""", null);
	}

	@Test
	public void invalidDeclarationsAndCaptures() throws Exception {
		assertInvalid("class Local {} class Local {}");
		assertInvalid("class Local { method get(): void { class Local {} } }");
		assertInvalid("class Local {} { class Local {} }");
		assertInvalid("if (true) class Local {}");
		assertInvalid("class Local extends String {}");
		assertInvalid("method create(int input): Object { class Local {"
			+ "method get(): int { return input; } } input++; return new Local(); }");
		assertInvalid("class Local extends java.util.AbstractList<String> {}");
		assertInvalid("class Local implements Runnable {}");
		assertInvalid("class Local extends java.io.FileInputStream {}");
		assertInvalid("class Local extends jbase.testlanguage.tests.SealedTypes.Shape {}");
		assertInvalid("int n = 1; class Local { method get(): int { return n; } } n++;");
		assertInvalid("int n; class Local { method get(): int { return n; } } n = 1;");
		assertInvalid("while (true) { class Local { method get(): void { break; } } }");
		assertInvalid("outer: while (true) { class Local {"
			+ "method get(): void { continue outer; } } }");
		assertInvalid("int n = switch (1) { default -> { class Local {"
			+ "method get(): int { yield 1; } } yield 2; } };");
		assertInvalid("class");
		assertInvalid("class Local {");
	}

	@Test
	public void captureDiagnosticsIdentifyTheActualRestriction() throws Exception {
		validationTestHelper.assertError(parser.parse(
			"int n = 1; class Local { method get(): int { return n; } } n++;"),
			XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
			JbaseIssueCodes.NON_EFFECTIVELY_FINAL_CAPTURE,
			"captured by a lambda or class body");
		validationTestHelper.assertError(parser.parse(
			"int n; class Local { method get(): int { return n; } } n = 1;"),
			XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
			JbaseIssueCodes.NOT_INITIALIZED_VARIABLE);
	}

	@Test
	public void serializationWithAndWithoutNodeModel() throws Exception {
		final var model = parser.parse("""
			int offset = 40;
			class Local extends java.util.ArrayList<String> implements java.io.Serializable {
				method self(): Local { return this; }
				method get(): int { return offset + 2; }
			}
			Local value = new Local();
			int result = value.self().get();
			""");
		validationTestHelper.assertNoErrors(model);
		checkCompilation(serializer.serialize(model), null);
		EcoreUtil.resolveAll(model);
		final var copy = EcoreUtil.copy(model);
		final var resource = model.eResource();
		resource.getContents().clear();
		resource.getContents().add(copy);
		checkCompilation(serializer.serialize(copy), null);
	}

	@Test
	public void hostControlsConstructorsAndAdditionalAssociations() throws Exception {
		final var block = XbaseFactory.eINSTANCE.createXBlockExpression();
		final var declaration = JbaseTestlanguageFactory.eINSTANCE.createLocalClass();
		declaration.setName("Local");
		block.getExpressions().add(declaration);
		final var method = TypesFactory.eINSTANCE.createJvmOperation();
		inferrer.inferLocalClasses(method, block, (source, type) -> { }, (source, type) -> {
			final var constructor = TypesFactory.eINSTANCE.createJvmConstructor();
			constructor.setSimpleName(type.getSimpleName());
			type.getMembers().add(constructor);
		});
		assertEquals(1, method.getLocalClasses().getFirst().getSuperTypes().size());
		assertEquals(1, method.getLocalClasses().getFirst().getMembers().size());

		final var model = parser.parse("class Local {} Local value = new Local();");
		final var local = EcoreUtil2.getAllContentsOfType(model, XJLocalClass.class).getFirst();
		final var type = (JvmGenericType) associations.getPrimaryJvmElement(local);
		final var field = TypesFactory.eINSTANCE.createJvmField();
		field.setSimpleName("metadata");
		field.setType(EcoreUtil.copy(type.getSuperTypes().getFirst()));
		type.getMembers().add(field);
		associator.associate(local, field);
		validationTestHelper.assertNoErrors(model);
	}

	@Test
	public void localNamesAreNotAvailableAcrossResources() throws Exception {
		final var first = parser.parse("class Hidden {}");
		validationTestHelper.assertNoErrors(first);
		final var second = parser.parse("Hidden value = new Hidden();",
			URI.createURI("other.jbasetestlanguage"), first.eResource().getResourceSet());
		assertTrue(validationTestHelper.validate(second).stream()
			.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
	}

	@Test
	public void defaultSkeletonAndIncompleteNames() throws Exception {
		final var model = (JbaseTestLanguageModel) parser.parse("class Local {}");
		final var feature = TypesFactory.eINSTANCE.createJvmOperation();
		inferrer.inferLocalClasses(feature, model.getBlock());
		assertEquals(1, feature.getLocalClasses().size());
		final var type = feature.getLocalClasses().getFirst();
		assertTrue(type.getDeclaredConstructors().iterator().hasNext());
		assertEquals(1, type.getSuperTypes().size());
		for (final var inferred : associations.getJvmElements(
				model.getBlock().getExpressions().getFirst())) {
			if (inferred instanceof JvmGenericType generic) generic.setSimpleName(null);
		}
		final var scope = namespaceScopes.getScope(model.getBlock().getExpressions().getFirst(),
			TypesPackage.Literals.JVM_PARAMETERIZED_TYPE_REFERENCE__TYPE);
		assertNull(scope.getSingleElement(QualifiedName.create("Local")));
	}

	private void assertInvalid(final String source) throws Exception {
		assertTrue(source, validationTestHelper.validate(parser.parse(source)).stream()
			.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
	}
}
