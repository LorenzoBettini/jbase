package jbase.tests;

import static org.junit.Assert.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.XForLoopExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.jbase.JbaseFactory;
import jbase.jbase.JbasePackage;
import jbase.jbase.XJTryWithResourcesExistingResource;
import jbase.jbase.XJVariableDeclaration;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import jbase.validation.JbaseIssueCodes;
import jbase.validation.JbaseValidator;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseVarTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> programParser;

	@Inject
	private IBatchTypeResolver typeResolver;

	@Inject
	private ISerializer serializer;

	@Inject
	private JbaseValidator validator;

	@Test
	public void inferredTypes() throws Exception {
		final String[][] cases = {
			{ "0", "int" }, { "-1", "int" }, { "1L", "long" },
			{ "1.0", "double" }, { "true", "boolean" }, { "'a'", "char" },
			{ "\"hello\"", "java.lang.String" }, { "(byte) 1", "byte" },
			{ "new int[] {1, 2}", "int[]" },
			{ "new java.util.ArrayList<String>()", "java.util.ArrayList<java.lang.String>" },
			{ "new java.util.ArrayList<>()", "java.util.ArrayList<java.lang.Object>" },
			{ "new java.util.ArrayList()", "java.util.ArrayList" },
			{ "java.util.Collections.emptyList()", "java.util.List<java.lang.Object>" },
			{ "(String) null", "java.lang.String" },
			{ "Integer.valueOf(1)", "java.lang.Integer" },
			{ "String.class", "java.lang.Class<java.lang.String>" },
			{ "java.util.Objects.requireNonNull(null)", "java.lang.Object" }
		};
		for (final var modifier : new String[] { "", "final " }) {
			for (final var testCase : cases) {
				final var model = parseValid(modifier + "var value = " + testCase[0] + ";");
				final var declaration = EcoreUtil2.getAllContentsOfType(model,
					XJVariableDeclaration.class).getFirst();
				assertNull(declaration.getType());
				assertEquals(!modifier.isEmpty(), declaration.isFinal());
				assertEquals(modifier.isEmpty(), declaration.isWriteable());
				assertType(model, declaration, testCase[1]);
			}
		}
	}

	@Test
	public void existingVariableResourceInference() throws Exception {
		for (final var modifier : new String[] { "", "final " }) {
			final var model = programParser.parse(modifier
				+ "var input = new java.io.ByteArrayInputStream(new byte[0]); try (input) {}");
			final var resource = EcoreUtil2.getAllContentsOfType(model,
				XJTryWithResourcesExistingResource.class).getFirst();
			assertTrue(resource.getRight() instanceof XFeatureCall);
			final var reference = (XFeatureCall) resource.getRight();
			assertTrue(reference.getFeature().getClass().getName(),
				reference.getFeature() instanceof XVariableDeclaration);
			assertEquals(modifier.isEmpty(),
				((XVariableDeclaration) reference.getFeature()).isWriteable());
			validationTestHelper.assertNoErrors(model);
			assertType(model, reference.getFeature(), "java.io.ByteArrayInputStream");
		}
	}

	@Test
	public void existingVariableResourceMustBeEffectivelyFinal() throws Exception {
		final var model = programParser.parse("""
			var input = new java.io.ByteArrayInputStream(new byte[0]);
			input = new java.io.ByteArrayInputStream(new byte[1]);
			try (input) {}
			""");
		validationTestHelper.assertError(model,
			JbasePackage.eINSTANCE.getXJTryWithResourcesExistingResource(),
			JbaseIssueCodes.INVALID_RESOURCE,
			"A resource must refer to a final or effectively final local variable or a final field");
	}

	@Test
	public void assignedOnceLocalCanBeAnExistingResource() throws Exception {
		parseValid("""
			java.io.StringReader input;
			input = new java.io.StringReader("hello");
			try (input) {}
			""");
	}

	@Test
	public void existingVariableResourceMustBeAutoCloseable() throws Exception {
		final var model = programParser.parse("""
			var input = "not closeable";
			try (input) {}
			""");
		validationTestHelper.assertError(model,
			JbasePackage.eINSTANCE.getXJTryWithResourcesExistingResource(),
			JbaseIssueCodes.NOT_AUTO_CLOSEABLE,
			"The resource type String does not implement java.lang.AutoCloseable");
	}

	@Test
	public void incompleteExistingResourcesAreIgnored() {
		final var resource = JbaseFactory.eINSTANCE.createXJTryWithResourcesExistingResource();
		validator.checkExistingResource(resource);
		resource.setRight(XbaseFactory.eINSTANCE.createXStringLiteral());
		validator.checkExistingResource(resource);
		resource.setRight(XbaseFactory.eINSTANCE.createXFeatureCall());
		validator.checkExistingResource(resource);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unsupportedResourceEntriesAreRejected() {
		final var statement = JbaseFactory.eINSTANCE.createXJTryWithResourcesStatement();
		statement.getResources().add(XbaseFactory.eINSTANCE.createXVariableDeclaration());
		statement.getResources().add(XbaseFactory.eINSTANCE.createXVariableDeclaration());
		validator.checkTryWithResources(statement);
	}

	@Test
	public void existingResourceSemicolonsAreRecognized() {
		final var statement = JbaseFactory.eINSTANCE.createXJTryWithResourcesStatement();
		final var existingResource =
			JbaseFactory.eINSTANCE.createXJTryWithResourcesExistingResource();
		existingResource.setSemicolon(";");
		statement.getResources().add(existingResource);
		statement.getResources().add(XbaseFactory.eINSTANCE.createXVariableDeclaration());
		validator.checkTryWithResources(statement);
	}

	@Test
	public void existingResourceMustReferenceAVariable() throws Exception {
		final var model = programParser.parse("try (String) {}");
		validationTestHelper.assertError(model,
			JbasePackage.eINSTANCE.getXJTryWithResourcesExistingResource(),
			JbaseIssueCodes.INVALID_RESOURCE,
			"A resource must refer to a final or effectively final local variable or a final field");
	}

	@Test
	public void existingResourceCanBeALambdaParameter() throws Exception {
		final var model = parseValid("""
			java.util.function.Consumer<java.io.StringReader> consumer =
				input -> { try (input) {} };
			""");
		final var resource = EcoreUtil2.getAllContentsOfType(model,
			XJTryWithResourcesExistingResource.class).getFirst();
		assertTrue(((XFeatureCall) resource.getRight()).getFeature()
			instanceof JvmFormalParameter);
	}

	@Test
	public void laterUsesDoNotInfluenceInference() throws Exception {
		final var model = parseValid("""
			var values = new java.util.ArrayList<>();
			values.add("hello");
			for (var value : values) { System.out.println(value); }
			""");
		final var declaration = EcoreUtil2.getAllContentsOfType(model,
			XJVariableDeclaration.class).getFirst();
		assertType(model, declaration, "java.util.ArrayList<java.lang.Object>");
	}

	@Test
	public void reassignmentPreservesInferredType() throws Exception {
		parseValid("var value = 1; value = 2; value++; ++value;");
		final var model = programParser.parse("var value = 1; value = \"text\";");
		validationTestHelper.assertError(model, XbasePackage.Literals.XSTRING_LITERAL,
			IssueCodes.INCOMPATIBLE_TYPES);
	}

	@Test
	public void finalVarCannotBeAssigned() throws Exception {
		for (final var assignment : new String[] { "value = 2", "value++", "++value" }) {
			final var model = programParser.parse("final var value = 1; " + assignment + ";");
			validationTestHelper.assertError(model, XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
				IssueCodes.ASSIGNMENT_TO_FINAL);
		}
	}

	@Test
	public void varStillWorksAsAnIdentifier() throws Exception {
		parseValid("int var = 1; var = var + 1; System.out.println(var);");
		parseValid("var var = 1; var++; System.out.println(var);");
		parseValid("op var(int var) : int { return var; } var result = var(1);");
	}

	@Test
	public void missingInitializer() throws Exception {
		assertInvalidVar("var value;", "without an initializer");
		assertInvalidVar("final var value;", "without an initializer");
	}

	@Test
	public void nullInitializer() throws Exception {
		assertInvalidVar("var value = null;", "initialized to 'null'");
		assertInvalidVar("final var value = (null);", "initialized to 'null'");
		assertInvalidVar("var value = true ? null : null;", "initialized to 'null'");
	}

	@Test
	public void arrayInitializer() throws Exception {
		assertInvalidVar("var value = {1, 2};", "explicit target type");
		assertInvalidVar("final var value = {};", "explicit target type");
	}

	@Test
	public void compoundDeclaration() throws Exception {
		assertInvalidVar("var first = 1, second = 2;", "compound declaration");
		assertInvalidVar("final var first = 1, second = 2;", "compound declaration");
		assertInvalidVar("for (var i = 0, j = 1; i < j; i++) {}", "compound declaration");
	}

	@Test
	public void invalidInitializers() throws Exception {
		for (final var source : new String[] {
			"var value = value;", "var value = System.out.println(1);",
			"var[] value = new int[1];", "var value[] = new int[1];",
			"method m(var value) {}", "try {} catch (var e) {}",
			"for (var value : 1) {}", "for (var value : ) {}"
		}) {
			final var model = programParser.parse(source);
			assertTrue(source, validationTestHelper.validate(model).stream()
				.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
		}
	}

	@Test
	public void basicForLoop() throws Exception {
		parseValid("for (var i = 0; i < 3; i++) { System.out.println(i); }");
		parseValid("for (final var i = 0; i < 1;) { System.out.println(i); break; }");
	}

	@Test
	public void enhancedForLoop() throws Exception {
		final String[][] cases = {
			{ "new int[] {1, 2}", "int" },
			{ "new String[] {\"a\"}", "java.lang.String" },
			{ "java.util.Arrays.asList(\"a\", \"b\")", "java.lang.String" },
			{ "new java.util.ArrayList()", "java.lang.Object" },
			{ "new java.util.ArrayList<>()", "java.lang.Object" },
			{ "java.util.Collections.emptyList()", "java.lang.Object" }
		};
		for (final var modifier : new String[] { "", "final " }) {
			for (final var testCase : cases) {
				final var model = parseValid("for (" + modifier + "var value : "
					+ testCase[0] + ") { System.out.println(value); }");
				final var loop = EcoreUtil2.getAllContentsOfType(model,
					XForLoopExpression.class).getFirst();
				assertType(model, loop.getDeclaredParam(), testCase[1]);
			}
		}
	}

	@Test
	public void wildcardInference() throws Exception {
		final var model = parseValid("""
			method m(java.util.List<? extends Number> values) {
				var first = values.get(0);
				System.out.println(first.intValue());
				for (var value : values) { System.out.println(value.intValue()); }
			}
			""");
		final var declaration = EcoreUtil2.getAllContentsOfType(model,
			XJVariableDeclaration.class).getFirst();
		assertType(model, declaration, "java.lang.Number");
	}

	@Test
	public void enhancedForFinalVar() throws Exception {
		final var model = programParser.parse(
			"for (final var value : new int[] {1}) { value = 2; }");
		validationTestHelper.assertError(model, XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
			IssueCodes.ASSIGNMENT_TO_FINAL);
	}

	@Test
	public void resourceInference() throws Exception {
		for (final var modifier : new String[] { "", "final " }) {
			final var model = parseValid("try (" + modifier
				+ "var input = new java.io.StringReader(\"hello\")) { input.read(); }");
			final var declaration = EcoreUtil2.getAllContentsOfType(model,
				XJVariableDeclaration.class).getFirst();
			assertType(model, declaration, "java.io.StringReader");
		}
		parseValid("""
			try (var first = new java.io.StringReader("hello");
				final var second = new java.io.BufferedReader(first)) {
				second.readLine();
			}
			""");
	}

	@Test
	public void resourceInitializerHasNoTargetType() throws Exception {
		final var model = programParser.parse(
			"try (var value = java.util.Objects.requireNonNull(null)) {}");
		validationTestHelper.assertError(model,
			JbasePackage.eINSTANCE.getXJTryWithResourcesVariableDeclaration(),
			JbaseIssueCodes.NOT_AUTO_CLOSEABLE);
		final var declaration = EcoreUtil2.getAllContentsOfType(model,
			XJVariableDeclaration.class).getFirst();
		assertType(model, declaration, "java.lang.Object");
	}

	@Test
	public void resourceDeclarationRequiresInitializer() throws Exception {
		final var model = programParser.parse("try (var value = ) {}");
		validationTestHelper.assertError(model,
			JbasePackage.eINSTANCE.getXJTryWithResourcesVariableDeclaration(),
			JbaseIssueCodes.INVALID_RESOURCE,
			"A resource declaration requires an initializer");
	}

	@Test
	public void resourceIsImplicitlyFinal() throws Exception {
		final var model = programParser.parse("""
			try (var input = new java.io.StringReader("hello")) {
				input = new java.io.StringReader("other");
			}
			""");
		validationTestHelper.assertError(model, XbasePackage.Literals.XABSTRACT_FEATURE_CALL,
			IssueCodes.ASSIGNMENT_TO_FINAL);
	}

	@Test
	public void invalidResources() throws Exception {
		final var model = programParser.parse("try (var value = \"hello\") {}");
		validationTestHelper.assertError(model,
			JbasePackage.eINSTANCE.getXJTryWithResourcesVariableDeclaration(),
			JbaseIssueCodes.NOT_AUTO_CLOSEABLE);
		assertInvalidVar("try (var value = null) {}", "initialized to 'null'");
	}

	@Test
	public void serialization() throws Exception {
		for (final var source : new String[] {
			"var value = 1;", "final var value = 1;",
			"for (var value : new int[] {1}) {}",
			"try (final var value = new java.io.StringReader(\"a\")) {}",
			"var input = new java.io.StringReader(\"a\"); try (input) {}"
		}) {
			final var model = parseValid(source);
			final var copy = EcoreUtil.copy(model);
			model.eResource().getContents().set(0, copy);
			final var serialized = serializer.serialize(copy);
			parseValid(serialized);
			assertEquals(serialized.strip(),
				serializer.serialize(programParser.parse(serialized)).strip());
		}
	}

	@Test
	public void generatedJavaPreservesVarSyntax() throws Exception {
		checkCompilation("""
			var value = 1;
			final var text = "hello";
			value++;
			System.out.println(text + value);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    var value = 1;
			    final var text = "hello";
			    value++;
			    System.out.println((text + Integer.valueOf(value)));
			  }
			}
			""");
	}

	@Test
	public void compileAndExecute() throws Exception {
		final var source = """
			method testMe() {
				var total = 0;
				for (var i = 0; i < 3; i++) { total += i; }
				for (final var value : new int[] {4, 5}) { total += value; }
				var numbers = new int[] {total};
				numbers[0]++;
				final var values = new java.util.ArrayList<String>();
				values.add("hello");
				for (var value : values) { System.out.println(value.length()); }
				try (var input = new java.io.StringReader("abc")) {
					System.out.println(input.read());
				}
				System.out.println(numbers[0]);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "5\n97\n13\n");
	}

	private EObject parseValid(final String source) throws Exception {
		final var model = programParser.parse(source);
		validationTestHelper.assertNoErrors(model);
		return model;
	}

	private void assertType(final EObject model, final JvmIdentifiableElement declaration,
			final String expected) {
		assertEquals(expected, typeResolver.resolveTypes(model).getActualType(declaration)
			.getIdentifier());
	}

	private void assertInvalidVar(final String source, final String message) throws Exception {
		final var model = programParser.parse(source);
		validationTestHelper.assertError(model, JbasePackage.eINSTANCE.getXJVariableDeclaration(),
			JbaseIssueCodes.INVALID_VAR_DECLARATION, message);
	}
}
