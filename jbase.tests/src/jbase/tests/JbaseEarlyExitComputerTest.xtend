package jbase.tests

import com.google.inject.Inject
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.xbase.controlflow.IEarlyExitComputer
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseEarlyExitComputerTest extends JbaseAbstractTest {

	@Inject extension IEarlyExitComputer

	@Test def void testSwitchNotEarlyExitWithoutDefault() {
		'''
		switch (e) {
			case 0 : return;
		}
		'''.assertEarlyExit(false) // there's no default
	}

	@Test def void testSwitchEarlyExitWithDefault() {
		'''
		switch (e) {
			case 0 : return;
			default: return;
		}
		'''.assertEarlyExit(true)
	}

	@Test def void testSwitchNotEarlyExitWithDefaultAndCaseWithBreak() {
		'''
		switch (e) {
			case 0 : System.out.println(); break;
			default: return;
		}
		'''.assertEarlyExit(false)
	}

	@Test def void testSwitchEarlyExitWithDefaultAndCaseWithoutBreak() {
		'''
		switch (e) {
			case 0 : System.out.println();
			default: return;
		}
		'''.assertEarlyExit(true)
	}

	@Test def void testSwitchEarlyExitWithDefaultAndCaseWithBreak() {
		'''
		switch (e) {
			case 0 : return; break;
			default: return;
		}
		'''.assertEarlyExit(true)
	}

	def private assertEarlyExit(CharSequence input, boolean expected) {
		input.assertExpression[
			expected.assertEquals(isEarlyExit)
		]
	}
}