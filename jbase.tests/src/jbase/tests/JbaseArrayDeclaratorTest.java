package jbase.tests;

import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseArrayDeclaratorTest extends JbaseAbstractCompilerTest {

	@Inject
	private ParseHelper<EObject> parser;

	@Test
	public void declaratorDimensionsAndTrailingCommasCompileAndRun() throws Exception {
		final var source = """
			method inspect(String values[]): void {}
			method testMe() {
				int empty[];
				int values[] = {1, 2,};
				int scalar = 1, plain = 0, more[] = {2,};
				int[][] matrix = new int[] [] {{3, 4,},};
				java.util.function.Consumer<String[]> consumer =
					(String items[]) -> System.out.println(items[0]);
				consumer.accept(new String[] {"lambda",});
				System.out.println(values[1] + scalar + more[0] + matrix[0][1]);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "lambda\n9\n");
	}

	@Test
	public void lambdaDeclaratorDimensionsMustMatchTargetArrayType() throws Exception {
		final var model = parser.parse("""
			method testMe() {
				java.util.function.Consumer<String> scalar =
					(String items[]) -> {};
				java.util.function.Consumer<Integer[]> array =
					(String items[]) -> {};
			}
			""");
		assertTrue(validationTestHelper.validate(model).stream()
			.anyMatch(issue -> issue.getSeverity() == Severity.ERROR));
	}

	@Test
	public void malformedArrayInitializersStillFailParsing() throws Exception {
		final var model = parser.parse("int[] values = {1,,};");
		assertTrue(!model.eResource().getErrors().isEmpty());
	}
}
