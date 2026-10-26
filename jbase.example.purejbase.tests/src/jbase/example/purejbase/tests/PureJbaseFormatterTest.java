package jbase.example.purejbase.tests;

import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.formatter.FormatterTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(PureJbaseInjectorProvider.class)
public class PureJbaseFormatterTest {

	@Inject
	private FormatterTestHelper formatterTestHelper;

	@Test
	public void formatsImportSectionAndMainBlock() {
		formatterTestHelper.assertFormatted(request -> {
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
		formatterTestHelper.assertFormatted(request -> {
			request.setToBeFormatted("""
				System.out.println( "Hello" );
				""");
			request.setExpectation("""
				System.out.println("Hello");
				""");
		});
	}
}
