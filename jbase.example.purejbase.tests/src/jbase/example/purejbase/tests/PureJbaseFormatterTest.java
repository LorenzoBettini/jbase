package jbase.example.purejbase.tests;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.formatter.FormatterTestHelper;
import org.eclipse.xtext.testing.formatter.FormatterTestRequest;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(PureJbaseInjectorProvider.class)
public class PureJbaseFormatterTest {

	@Inject
	private FormatterTestHelper formatterTestHelper;

	@Test
	public void formatsImportSectionAndMainBlock() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				import  java.util.List ;
				System.out.println( "Hello" );
				""");
			request.setExpectation("""
				import java.util.List;

				System.out.println("Hello");
				""");
		});
	}

	@Test
	public void formatsMainBlockWithoutImports() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				System.out.println( "Hello" );
				""");
			request.setExpectation("""
				System.out.println("Hello");
				""");
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
