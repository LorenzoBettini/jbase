package jbase.tests

import com.google.inject.Inject
import jbase.controlflow.JbaseSureReturnComputer
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseSureReturnComputerTest extends JbaseAbstractTest {

	@Inject extension JbaseSureReturnComputer

	@Test def void testSwitchNotSureReturnWithoutDefault() {
		'''
		switch (e) {
			case 0 : return;
		}
		'''.assertSureReturn(false) // there's no default
	}

	@Test def void testSwitchSureReturnWithDefault() {
		'''
		switch (e) {
			case 0 : return;
			default: return;
		}
		'''.assertSureReturn(true)
	}

	@Test def void testSwitchSureReturnWithDefaultAndCaseWithBreak() {
		'''
		switch (e) {
			case 0 : System.out.println(); break;
			default: return;
		}
		'''.assertSureReturn(true)
	}

	@Test def void testSwitchSureReturnWithDefaultAndCaseWithoutBreak() {
		'''
		switch (e) {
			case 0 : System.out.println();
			default: return;
		}
		'''.assertSureReturn(true)
	}

	def private assertSureReturn(CharSequence input, boolean expected) {
		input.assertExpression[
			expected.assertEquals(isSureReturn)
		]
	}
}