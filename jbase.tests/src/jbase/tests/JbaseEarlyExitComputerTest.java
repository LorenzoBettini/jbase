package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.controlflow.IEarlyExitComputer;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseEarlyExitComputerTest extends JbaseAbstractTest {

	@Inject
	private IEarlyExitComputer iEarlyExitComputer;

	@Test
	public void testSwitchNotEarlyExitWithoutDefault() throws Exception {
		// there's no default
		assertEarlyExit("""
			switch (e) {
				case 0 : return;
			}
			""", false);
	}

	@Test
	public void testSwitchEarlyExitWithDefault() throws Exception {
		assertEarlyExit("""
			switch (e) {
				case 0 : return;
				default: return;
			}
			""", true);
	}

	@Test
	public void testSwitchNotEarlyExitWithDefaultAndCaseWithBreak() throws Exception {
		assertEarlyExit("""
			switch (e) {
				case 0 : System.out.println(); break;
				default: return;
			}
			""", false);
	}

	@Test
	public void testSwitchEarlyExitWithDefaultAndCaseWithoutBreak() throws Exception {
		assertEarlyExit("""
			switch (e) {
				case 0 : System.out.println();
				default: return;
			}
			""", true);
	}

	@Test
	public void testSwitchEarlyExitWithDefaultAndCaseWithBreak() throws Exception {
		assertEarlyExit("""
			switch (e) {
				case 0 : return; break;
				default: return;
			}
			""", true);
	}

	private void assertEarlyExit(final CharSequence input, final boolean expected)
			throws Exception {
		assertExpression(input,
				expression -> assertEquals(expected, iEarlyExitComputer.isEarlyExit(expression)));
	}
}
