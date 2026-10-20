package jbase.tests;

import static org.junit.Assert.*;
import com.google.inject.Inject;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.serializer.impl.Serializer;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseSerializerTest extends JbaseAbstractTest {

	/**
	 * Since we use both Jbase and JbaseTestlanguage we specify EObject
	 * as the type returned by ParseHelper
	 */
	@Inject
	protected ParseHelper<EObject> parseHelper;

	@Inject
	private Serializer serializer;

	@Test
	public void testLambdas() throws Exception {
		assertSerialize("""
			java.util.function.Function<String, Integer> f = s -> s.length();
			java.util.function.BiFunction<Integer, Integer, Integer> sum = (x, y) -> x + y;
			java.util.function.IntUnaryOperator g = (final int x) -> { return x + 1; };
			Runnable r = () -> {};
			""");
	}

	@Test
	public void testAssignmentLeft() throws Exception {
		assertSerialize("""
			i = 1;
			""");
	}

	@Test
	public void testVariableDeclarations() throws Exception {
		assertSerialize("""
			int  i , j , k ;
			""");
	}

	@Test
	public void testMemberFeatureCall() throws Exception {
		assertSerialize("""
			System.out.println("a");
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccesses() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length;
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccesses2() throws Exception {
		assertSerialize("""
			args  [ 0 ] [ 1 ]   . length;
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccessesWithParenthesis() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length();
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccessesWithArguments() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length(0);
			""");
	}

	@Test
	public void testMemberFeatureCallArrayAccessesWithArguments2() throws Exception {
		assertSerialize("""
			args  [ 0 ]   . length(0,1);
			""");
	}

	@Test
	public void testBinaryOperator() throws Exception {
		assertSerialize("""
			for  (int  i  =  0  ; i  <  argsNum ;  i  +=  1 )  {
			\t
			}
			""");
	}

	@Test
	public void testBinaryOperator2() throws Exception {
		assertSerialize("""
			int j = 1 + 0;
			""");
	}

	@Test
	public void testBreak() throws Exception {
		assertSerialize("""
			while (true) {
				break ;
			}
			""");
	}

	@Test
	public void testBreak2() throws Exception {
		assertSerialize("""
			while (true)
				break ;
			""");
	}

	@Test
	public void testCast() throws Exception {
		assertSerialize("""
			(Object) o;
			""");
	}

	@Test
	public void testConditional() throws Exception {
		assertSerialize("""
			1 > 0 ? true : false;
			""");
	}

	@Test
	public void testArrayDimensions() throws Exception {
		assertSerialize("""
			new List[1][] {0, 1, 1 + 2};
			new List[1][2] {0, 1, 1 + 2};
			""");
	}

	@Test
	public void testArrayDimensions2() throws Exception {
		assertSerialize("""
			new List[][2] {0, 1, 1 + 2};
			new List[][] {0, 1, 1 + 2};
			new List[][];
			""");
	}

	@Test
	public void testArrayDimensions3() throws Exception {
		assertSerialize("""
			new List[][2][][1] {0, 1, 1 + 2};
			""");
	}

	@Test
	public void testBitwiseInclusiveOr() throws Exception {
		assertSerialize("""
			0 | 1;
			""");
	}

	@Test
	public void testBitwiseOperators() throws Exception {
		assertSerialize("""
			0 & 1;
			0 | 1;
			0 ^ 1;
			~1;
			""");
	}

	private void assertSerialize(final CharSequence input) throws Exception {
		final var o = parseHelper.parse(input);
		assertEquals(input.toString(), serializer.serialize(o));
	}
}
