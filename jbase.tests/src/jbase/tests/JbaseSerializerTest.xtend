package jbase.tests

import com.google.inject.Inject
import jbase.JbaseInjectorProvider
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.serializer.impl.Serializer
import org.junit.Test
import org.junit.runner.RunWith

import static extension org.junit.Assert.*

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseSerializerTest extends JbaseAbstractTest {

	@Inject Serializer serializer

	@Test def void testAssignmentLeft() {
		assertSerialize('''
			i = 1;
		''')
	}

	@Test def void testVariableDeclarations() {
		'''
			int  i , j , k ;
		'''.assertSerialize
	}

	@Test def void testMemberFeatureCall() {
		'''
			System.out.println("a");
		'''.assertSerialize
	}

	@Test def void testMemberFeatureCallArrayAccesses() {
		'''
			args  [ 0 ]   . length;
		'''.assertSerialize
	}

	@Test def void testMemberFeatureCallArrayAccesses2() {
		'''
			args  [ 0 ] [ 1 ]   . length;
		'''.assertSerialize
	}

	@Test def void testMemberFeatureCallArrayAccessesWithParenthesis() {
		'''
			args  [ 0 ]   . length();
		'''.assertSerialize
	}

	@Test def void testMemberFeatureCallArrayAccessesWithArguments() {
		'''
			args  [ 0 ]   . length(0);
		'''.assertSerialize
	}

	@Test def void testMemberFeatureCallArrayAccessesWithArguments2() {
		'''
			args  [ 0 ]   . length(0,1);
		'''.assertSerialize
	}

	def private assertSerialize(CharSequence input) {
		val o = input.parse
		input.toString.assertEquals(serializer.serialize(o))
	}
}