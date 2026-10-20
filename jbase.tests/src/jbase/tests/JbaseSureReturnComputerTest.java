package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import jbase.controlflow.JbaseSureReturnComputer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseSureReturnComputerTest extends JbaseAbstractTest {

	@Inject
	private JbaseSureReturnComputer jbaseSureReturnComputer;

	@Test
	public void testSwitchNotSureReturnWithoutDefault() throws Exception {
		// there's no default
		assertSureReturn("""
			switch (e) {
				case 0 : return;
			}
			""", false);
	}

	@Test
	public void testSwitchSureReturnWithDefault() throws Exception {
		assertSureReturn("""
			switch (e) {
				case 0 : return;
				default: return;
			}
			""", true);
	}

	@Test
	public void testSwitchSureReturnWithDefaultAndCaseWithBreak() throws Exception {
		assertSureReturn("""
			switch (e) {
				case 0 : System.out.println(); break;
				default: return;
			}
			""", true);
	}

	@Test
	public void testSwitchSureReturnWithDefaultAndCaseWithoutBreak() throws Exception {
		assertSureReturn("""
			switch (e) {
				case 0 : System.out.println();
				default: return;
			}
			""", true);
	}

	private void assertSureReturn(final CharSequence input, final boolean expected)
			throws Exception {
		assertExpression(input, expression -> assertEquals(expected,
				jbaseSureReturnComputer.isSureReturn(expression)));
	}
}
