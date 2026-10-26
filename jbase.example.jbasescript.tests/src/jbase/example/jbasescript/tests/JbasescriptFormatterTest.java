package jbase.example.jbasescript.tests;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.formatter.FormatterTestHelper;
import org.eclipse.xtext.testing.formatter.FormatterTestRequest;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbasescriptInjectorProvider.class)
public class JbasescriptFormatterTest {

	@Inject
	private FormatterTestHelper formatterTestHelper;

	@Test
	public void formatsOperationsAndMainBlock() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				op  twice(int  value):int{return value*2;}
				System.out.println(twice( 21 ));
				""");
			request.setExpectation("""
				op twice(int value) : int {
					return value * 2;
				}

				System.out.println(twice(21));
				""");
		});
	}

	@Test
	public void formatsImportsAndOperationsWithoutParametersOrReturnType() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				import  java.util.List ;
				op  greet(){System.out.println( "Hello" );}
				System.out.println( "Done" );
				""");
			request.setExpectation("""
				import java.util.List;

				op greet() {
					System.out.println("Hello");
				}

				System.out.println("Done");
				""".stripTrailing() + System.lineSeparator());
		});
	}

	/**
	 * Java text blocks use LF, while the formatter uses the platform line separator.
	 */
	private void assertFormatted(final Procedure1<FormatterTestRequest> initializer) {
		formatterTestHelper.assertFormatted(request -> {
			initializer.apply(request);
			request.setExpectation(request.getExpectationOrToBeFormatted().toString()
					.replace("\n", System.lineSeparator()));
			request.setToBeFormatted(request.getToBeFormatted().toString()
					.replace("\n", System.lineSeparator()));
		});
	}
}
