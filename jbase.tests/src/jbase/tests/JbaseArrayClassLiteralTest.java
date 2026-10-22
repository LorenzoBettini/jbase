package jbase.tests;

import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;
import jbase.jbase.XJClassObject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.serializer.ISerializer;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.junit.Test;

public class JbaseArrayClassLiteralTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Inject
	private IBatchTypeResolver typeResolver;

	@Inject
	private ISerializer serializer;

	@Test
	public void repeatedDimensionsAreRetainedAndTyped() throws Exception {
		for (final var type : new String[] {
			"boolean", "byte", "short", "char", "int", "long", "float", "double",
			"String", "Integer"
		}) {
			for (var rank = 1; rank <= 4; rank++) {
				final var dimensions = "[]".repeat(rank);
				final var input = "Class<" + type + dimensions + "> c = "
					+ type + dimensions + ".class;";
				final var model = parser.parse(input);
				final var literal = EcoreUtil2.getAllContentsOfType(model, XJClassObject.class)
					.getFirst();
				assertEquals(input, rank, literal.getArrayDimensions().size());
				assertEquals(literal.getArrayDimensions(),
					EcoreUtil.copy(literal).getArrayDimensions());
				for (final var dimension : literal.getArrayDimensions()) {
					assertEquals("[]", dimension);
				}
				final var component = Character.isUpperCase(type.charAt(0))
					? "java.lang." + type : type;
				assertEquals(input, "java.lang.Class<" + component + dimensions + ">",
					typeResolver.resolveTypes(model).getActualType(literal).getIdentifier());
				validationTestHelper.assertNoErrors(model);
			}
		}
	}

	@Test
	public void generatedJavaPreservesDimensionsAndImports() throws Exception {
		checkCompilation("""
			import java.util.Date;
			Class<int[][]> primitiveType = int[][].class;
			Class<String[][][]> referenceType = String[][][].class;
			Class<Date[][][][]> importedType = Date[][][][].class;
			Class<?> wildcardType = int[][][].class;
			""", """
			package jbasetestlanguage;

			import java.util.Date;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Class<int[][]> primitiveType = int[][].class;
			    Class<String[][][]> referenceType = String[][][].class;
			    Class<Date[][][][]> importedType = Date[][][][].class;
			    Class<?> wildcardType = int[][][].class;
			  }
			}
			""");
	}

	@Test
	public void runtimeClassObjectsPreserveDimensions() throws Exception {
		assertExecuteMain("""
			method testMe() {
				System.out.println(int[][].class.getName());
				System.out.println(String[][][].class.getName());
				System.out.println(java.util.Date[][][][].class.getName());
			}
			""", "[[I\n[[[Ljava.lang.String;\n[[[[Ljava.util.Date;\n");
	}

	@Test
	public void annotationValuesPreserveDimensions() throws Exception {
		checkCompilation("""
			import jbase.tests.util.ExampleAnnotation3;
			@ExampleAnnotation3(value = { int[][].class, String[][][].class })
			o : Object
			""", """
			package jbasetestlanguage;

			import jbase.tests.util.ExampleAnnotation3;

			@SuppressWarnings("all")
			public class MyFile {
			  @ExampleAnnotation3(value = { int[][].class, String[][][].class })
			  private Object o;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void serializationPreservesParsedAndEditedDimensions() throws Exception {
		final var input = "Class<?> p = int[][].class; Class<?> r = String[][][].class;";
		final var parsed = parser.parse(input);
		validationTestHelper.assertNoErrors(parsed);
		final var serialized = serializer.serialize(parsed);
		assertEquals(input.replaceAll("\\s+", ""), serialized.replaceAll("\\s+", ""));
		final var reparsed = parser.parse(serialized);
		final var literals = EcoreUtil2.getAllContentsOfType(reparsed, XJClassObject.class);
		assertEquals(2, literals.get(0).getArrayDimensions().size());
		assertEquals(3, literals.get(1).getArrayDimensions().size());
		literals.getFirst().getArrayDimensions().add("[]");
		assertEquals(input.replace("int[][]", "int[][][]").replaceAll("\\s+", ""),
			serializer.serialize(reparsed).replaceAll("\\s+", ""));
	}
}
