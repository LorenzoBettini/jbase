package jbase.tests;

import static org.junit.Assert.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.common.types.util.TypeReferences;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociator;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;
import jbase.testlanguage.validation.JbaseTestlanguageValidator;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseAnonymousClassTest extends JbaseAbstractCompilerTest {
	@Inject
	private ParseHelper<EObject> parser;
	@Inject
	private IBatchTypeResolver types;
	@Inject
	private IJvmModelAssociations associations;
	@Inject
	private IJvmModelAssociator associator;
	@Inject
	private TypeReferences references;
	@Inject
	private JbaseTestlanguageValidator validator;

	@Before
	public void enableInitializationChecks() {
		validator.setSkipVariableInitializationCheck(false);
	}

	@Test
	public void completeGeneratedJava() throws Exception {
		checkCompilation("var value = new Object() {};", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    var value = new Object() {
			    };
			  }
			}
			""");
	}

	@Test
	public void emptyBodiesCompile() throws Exception {
		checkCompilation("""
			Object value = new Object() {};
			java.io.Serializable marker = new java.io.Serializable() {};
			java.util.ArrayList<String> values = new java.util.ArrayList<String>(2) {};
			values.add("a");
			""", null);
	}

	@Test
	public void varRetainsAnonymousFieldsAndMethods() throws Exception {
		final var source = """
			var point = new Object() {
				x: int
				y: int
				method getX(): int { return x; }
				method getY(): int { return this.y; }
			};
			point.x = 10;
			point.y = 20;
			point.x += 20;
			point.y *= 2;
			System.out.println(point.getX() + ", " + point.getY());
			""";
		final var model = parser.parse(source);
		validationTestHelper.assertNoErrors(model);
		final var anonymous = EcoreUtil2.getAllContentsOfType(model, XJAnonymousClass.class)
			.getFirst();
		final var variable = (XVariableDeclaration) anonymous.eContainer();
		final var resolved = types.resolveTypes(model);
		final var variableType = resolved.getActualType(
			(JvmIdentifiableElement) variable).getType();
		assertSame(variableType, resolved.getActualType(anonymous).getType());
		assertTrue(variableType instanceof JvmGenericType type
			&& type.isAnonymous());
		checkCompilation(source, null);
		assertExecuteMain("method testMe(): void {\n" + source + "\n}", "30, 40\n");
	}

	@Test
	public void interfaceImplementationCapturesLocal() throws Exception {
		final var source = """
			method testMe(): void {
				int offset = 40;
				java.util.function.IntSupplier value = new java.util.function.IntSupplier() {
					method getAsInt(): int { return offset + 2; }
				};
				System.out.println(value.getAsInt());
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "42\n");
	}

	@Test
	public void superclassVariableDoesNotExposeAnonymousMembers() throws Exception {
		assertInvalid("Object value = new Object() { method extra(): int { return 1; } }; value.extra();");
		assertInvalid("Object value = new Object() { x: int }; value.x = 1;");
	}

	@Test
	public void missingImplementationAndFinalSuperclassAreRejected() throws Exception {
		assertInvalid("Runnable value = new Runnable() {};");
		assertInvalid("Object value = new String() {};");
		assertInvalid("Object value = new Object(1) {};");
	}

	@Test
	public void illegalCapturesAndBranchesAreRejected() throws Exception {
		assertInvalid("""
			int n = 1;
			Runnable value = new Runnable() { method run(): void { System.out.println(n); } };
			n++;
			""");
		assertInvalid("""
			int n;
			Runnable value = new Runnable() { method run(): void { System.out.println(n); } };
			""");
		assertInvalid("""
			while (true) {
				Runnable value = new Runnable() { method run(): void { break; } };
				break;
			}
			""");
	}

	@Test
	public void genericAndDiamondCreation() throws Exception {
		checkCompilation("""
			java.util.List<String> diamond = new java.util.ArrayList<>() {};
			diamond.add("a");
			var explicit = new java.util.ArrayList<String>() {};
			String first = explicit.get(0);
			java.util.function.Supplier<String> supplier = new java.util.function.Supplier<>() {
				method get(): String { return "hello"; }
			};
			""", null);
		assertInvalid("var value = new java.util.ArrayList<>() { method extra(): int { return 1; } };");
	}

	@Test
	public void nestedClassesAndMutableMethodParameters() throws Exception {
		checkCompilation("""
			var outer = new Object() {
				method create(int n): Object {
					n++;
					return new Object() { method toString(): String { return "nested"; } };
				}
			};
			outer.create(1);
			""", null);
	}

	@Test
	public void superCallsAndDefaultMethods() throws Exception {
		final var source = """
			method testMe(): void {
				var values = new java.util.ArrayList<String>(2) {
					method size(): int { return super.size() + 1; }
				};
				values.add("a");
				System.out.println(values.size());
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "2\n");
	}

	@Test
	public void protectedOverloadedAndVarargsConstructors() throws Exception {
		final var source = """
			import jbase.tests.util.AnonymousClassFixtures.Base;
			method testMe(): void {
				var protectedValue = new Base(42) {};
				var varargsValue = new Base("a", "b") {};
				var arguments = new Base(new Object() {
					method toString(): String { return "one"; }
				}, new Object() { method toString(): String { return "two"; } }) {};
				System.out.println(protectedValue.value());
				System.out.println(varargsValue.value());
				System.out.println(arguments.value());
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "42\n2\n6\n");
		assertInvalid("var value = new jbase.tests.util.AnonymousClassFixtures.Base(true) {};");
	}

	@Test
	public void constructorArgumentAndTargetInference() throws Exception {
		final var source = """
			import jbase.tests.util.AnonymousClassFixtures.Generic;
			method testMe(): void {
				var first = new Generic<>("a") {};
				String value = first.get();
				Generic<Number> second = new Generic<>(1) {};
				System.out.println(value + second.get());
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "a1\n");
	}

	@Test
	public void constructorArgumentsAreImmediateAndMembersUseEnclosingInstance() throws Exception {
		final var source = """
			import jbase.tests.util.AnonymousClassFixtures.Base;
			value: int
			method next(): int { System.out.println("argument"); return ++value; }
			method testMe(): void {
				var instance = new Base(next()) {
					method read(): int { return next() + this.value(); }
				};
				System.out.println(instance.read());
				int n;
				var immediate = new Base(n = 7) {};
				System.out.println(n);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "argument\nargument\n3\n7\n");
	}

	@Test
	public void invalidMembersAndControlFlow() throws Exception {
		assertInvalid("var value = new Object() { x: int x: int };");
		assertInvalid("var value = new Object() { method toString(): int { return 1; } };");
		assertInvalid("var value = new Object() { method get(): int { return 1; } method get(): int { return 2; } };");
		assertInvalid("var value = new jbase.testlanguage.tests.SealedTypes.Shape() {};");
		assertInvalid("var value = new Missing() {};");
		assertInvalid("while (true) { var value = new Object() { method run(): void { continue; } }; }");
		assertInvalid("int value = switch (1) { default -> { var x = new Object() { method get(): int { yield 1; } }; yield 2; } };");
		checkCompilation("""
			int value = switch (1) {
				default -> {
					var x = new Object() { method get(): int { return 1; } };
					yield x.get();
				}
			};
			""", null);
	}

	@Test
	public void anonymousClassesInLambdaAndRawCreation() throws Exception {
		checkCompilation("""
			java.util.function.Supplier<Object> factory = () -> new Object() {};
			var raw = new java.util.ArrayList() { method extra(): int { return 1; } };
			raw.add("a");
			Object value = raw.get(0);
			int n = raw.extra();
			var values = new java.util.HashMap<String, Integer>() {};
			""", null);
	}

	@Test
	public void constructorStatementAndVoidLambda() throws Exception {
		checkCompilation("""
			new Object() {};
			Runnable factory = () -> new Object() {};
			String name = (true ? new Object() {} : new Object() {}).toString();
			""", null);
		assertInvalid("int n; Runnable value = new Runnable() { method run(): void { n = 1; } };");
		assertInvalid("var value = new java.util.ArrayList() {}; String first = value.get(0);");
	}

	@Test
	public void additionalHostAssociationsArePreserved() throws Exception {
		final var model = parser.parse("var value = new Object() {};");
		final var anonymous = EcoreUtil2.getAllContentsOfType(model, XJAnonymousClass.class).getFirst();
		final var host = (JvmGenericType) model.eResource()
			.getContents().get(1);
		final var field = TypesFactory.eINSTANCE.createJvmField();
		field.setSimpleName("metadata");
		field.setType(references.getTypeForName(Object.class, model));
		host.getMembers().add(field);
		associator.associate(anonymous, field);
		validationTestHelper.assertNoErrors(model);
		assertTrue(host.getMembers().contains(field));
	}

	@Test
	public void privateAdditionalMethodsAreAllowedWithDiamond() throws Exception {
		final var model = parser.parse("var value = new java.util.ArrayList<>() { method extra(): int { return 1; } };");
		final var anonymous = EcoreUtil2.getAllContentsOfType(model, XJAnonymousClass.class).getFirst();
		final var type = (JvmGenericType) associations.getJvmElements(anonymous)
			.iterator().next();
		type.getDeclaredOperations().iterator().next()
			.setVisibility(JvmVisibility.PRIVATE);
		validationTestHelper.assertNoErrors(model);
	}

	@Test
	public void invalidAnnotationAndEnumBodiesAreDiagnosed() throws Exception {
		assertInvalid("var value = new Deprecated() {};");
		assertInvalid("var value = new Thread.State() {};");
	}

	@Test
	public void genericSupertypesAreSpecializedInsideMemberBodies() throws Exception {
		checkCompilation("""
			var values = new java.util.ArrayList<String>() {
				method first(): String { return get(0); }
			};
			java.util.List<String> diamond = new java.util.ArrayList<>() {
				method get(int index): String { return super.get(index); }
			};
			var abstractList = new java.util.AbstractList<String>() {
				method get(int index): String { return "element"; }
				method size(): int { return 1; }
			};
			String element = abstractList.get(0);
			java.util.function.Supplier<String> inferred = new java.util.function.Supplier<>() {
				method get() { return "hello"; }
			};
			""", null);
	}

	@Test
	public void incompleteBodiesHaveDiagnosticsWithoutInferenceExceptions() throws Exception {
		assertInvalid("var value = new Object() {");
		assertInvalid("var value = new Object() { method");
		assertInvalid("var value = new Object() { method get(): int");
		assertInvalid("var value = new Object {};");
		assertInvalid("var value = new java.util.ArrayList<String, Integer>() {};");
		assertInvalid("var value = new java.util.ArrayList<String>() { method echo(E x): E { return x; } };");
	}

	@Test
	public void enclosingTypeParametersAreNotShadowedByInferenceParameters() throws Exception {
		final var model = parser.parse("""
			method make(E element): Object {
				return new java.util.ArrayList<String>() {
					method echo(E value): E { return value; }
				};
			}
			""");
		final var host = (JvmGenericType) model.eResource().getContents().get(1);
		final var parameter = TypesFactory.eINSTANCE.createJvmTypeParameter();
		parameter.setName("E");
		host.getTypeParameters().add(parameter);
		// Model the containing generic class supplied by an embedding language.
		associator.associate(model, host);
		validationTestHelper.assertNoErrors(model);
		final var parameters = EcoreUtil2.getAllContentsOfType(model, jbase.jbase.XJJvmFormalParameter.class);
		assertSame(parameter, parameters.get(1).getParameterType().getType());
	}

	private void assertInvalid(final String source) throws Exception {
		assertTrue(validationTestHelper.validate(parser.parse(source)).stream()
			.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
	}
}
