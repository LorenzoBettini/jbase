package jbase.tests;

import com.google.inject.Inject;
import jbase.testlanguage.validation.JbaseTestlanguageValidator;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseAssertStatementTest extends JbaseAbstractCompilerTest {

	@Inject
	private JbaseTestlanguageValidator validator;

	@Before
	public void initValidator() {
		validator.setSkipVariableInitializationCheck(false);
	}

	@Test
	public void assertionsCompileToNativeJava() throws Exception {
		checkCompilation("""
			boolean condition = true;
			assert condition;
			assert condition : "message";
			assert condition : 1;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    boolean condition = true;
			    assert condition;
			    assert condition : "message";
			    assert condition : 1;
			  }
			}
			""");
	}

	@Test
	public void assertionConditionMustBeBoolean() throws Exception {
		validationTestHelper.assertError(parseHelper.parse("assert 1;"),
			XbasePackage.Literals.XNUMBER_LITERAL, IssueCodes.INCOMPATIBLE_TYPES,
			"Type mismatch: cannot convert from int to boolean");
	}

	@Test
	public void disabledAssertionsDoNotEvaluateTheirCondition() throws Exception {
		assertExecuteMain("""
			op testMe() : void {
				assert fail();
			}

			op fail() : boolean {
				System.out.println("evaluated");
				return false;
			}
			""", "");
	}
}
