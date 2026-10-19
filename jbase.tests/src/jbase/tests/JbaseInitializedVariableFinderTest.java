package jbase.tests;

import com.google.inject.Inject;
import jbase.util.JbaseNodeModelUtil;
import jbase.validation.JbaseInitializedVariableFinder;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseInjectorProvider.class)
public class JbaseInitializedVariableFinderTest extends JbaseAbstractTest {

	@Inject
	private JbaseInitializedVariableFinder jbaseInitializedVariableFinder;

	@Inject
	private JbaseNodeModelUtil jbaseNodeModelUtil;

	@Test
	public void testNotInitializedNull() throws Exception {
		// test it does not throw
		jbaseInitializedVariableFinder.detectNotInitialized(null, expression -> {
		});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNotInitializedInternalNull() throws Exception {
		jbaseInitializedVariableFinder.detectNotInitialized(null,
				new JbaseInitializedVariableFinder.InitializedVariables(), expression -> {
				});
	}

	@Test
	public void testNotInitializedWithNotVariableReference() throws Exception {
		assertNotInitializedReferences("""
			foo;
			""", "");
	}

	@Test
	public void testNotInitializedInCall() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			System.out.println(i);
			System.out.println(j);
			""", "i in System.out.println(i)");
	}

	@Test
	public void testNotInitializedInCall2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			System.out.println( i );
			i = 1; // now initialized
			System.out.println(i);
			System.out.println(j);
			""", "i in System.out.println( i )");
	}

	@Test
	public void testNotInitializedInCall3() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			System.out.println( j < i );
			""", "i in j < i");
	}

	@Test
	public void testInitializedInCall() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			System.out.println( (i = j) == (j = i) );
			System.out.println(i); // now initialized
			""", "");
	}

	@Test
	public void testNotInitializedInCall4() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			System.out.println( (j = i) == (i = j) );
			""", "i in (j = i)");
	}

	@Test
	public void testNotInitializedInUnaryOperation() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = -i;
			""", "i in -i");
	}

	@Test
	public void testNotInitializedInAssignment() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;
			j = i;
			""", "i in j = i");
	}

	@Test
	public void testNotInitializedInAssignment2() throws Exception {
		// even if foo is unresolved we still inspect the contents
		assertNotInitializedReferences("""
			int i;
			foo = i;
			""", "i in foo = i");
	}

	@Test
	public void testNotInitializedInVariableDeclaration() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = i;
			i = j; // OK
			""", "i in int j = i");
	}

	@Test
	public void testNotInitializedInSeveralVariableDeclarations() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int z = 0;
			int j = i, k = i, w = z;
			i = j; // OK
			""", """
			i in int j = i, k = i, w = z
			i in k = i\
			""");
	}

	@Test
	public void testNotInitializedInSeveralVariableDeclarations2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int z = 0;
			int j, k = i, w = z;
			i = k; // OK
			""", "i in k = i");
	}

	@Test
	public void testNotInitializedInArrayAccess() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int[] a = null;
			a[i] = 0;
			""", "i in a[i] = 0");
	}

	@Test
	public void testNotInitializedInArrayAccess2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int[] a = null;
			i = a[i];
			""", "i in a[i]");
	}

	@Test
	public void testInitializedInArrayAccess() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int[] a = null;
			a[i=1] = 0;
			System.out.println(i);
			""", "");
	}

	@Test
	public void testInitializedInArrayAccess2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int[] a = null;
			System.out.println(a[i=1]);
			""", "");
	}

	@Test
	public void testNotInitializedInArrayConstructor() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int[] a = new int[i];
			""", "i in new int[i]");
	}

	@Test
	public void testInitializedInArrayConstructor() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int[] a = new int[i=1];
			System.out.println(i);
			""", "");
	}

	@Test
	public void testNotInitializedAfterBlock() throws Exception {
		// even if foo is unresolved we still inspect the contents
		assertNotInitializedReferences("""
			int i;
			int j;
			int k;
			{
				j = 0;
			}
			k = i; // ERROR
			k = j; // OK
			""", "i in k = i");
	}

	@Test
	public void testNotInitializedInIf() throws Exception {
		assertNotInitializedReferences("""
			int i;
			if (i) {
			\t
			}
			""", "i in if (i) { }");
	}

	@Test
	public void testNotInitializedInIf2() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			int i;
			if (j) {
				j = i;
			}
			""", "i in j = i");
	}

	@Test
	public void testNotInitializedInIf3() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			int i;
			if (j) {
				j = 0;
			} else {
				int k = i;
			}
			""", "i in int k = i");
	}

	@Test
	public void testNotInitializedInIf4() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			int i;
			if (j) {
				i = 0;
			}
			System.out.println(i); // ERROR
			""", "i in System.out.println(i)");
	}

	@Test
	public void testInitializedInIfBothBranches() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			int i;
			if (j) {
				i = 0;
			} else {
				i = 1;
			}
			System.out.println(i); // OK
			""", "");
	}

	@Test
	public void testInitializedInConditional() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;
			int k;
			int z;
			boolean b = (i = 0) < (j = i) ? (k = 1) > 0 : (k = 1) > (z = 1);
			System.out.println(i); // OK
			System.out.println(j); // OK
			System.out.println(k); // OK
			System.out.println(z); // ERROR
			""", "z in System.out.println(z)");
	}

	@Test
	public void testInitializedInFor() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			int i;
			for (i = 0; i < 0; i++) {
				j = i;
			}
			""", "");
	}

	@Test
	public void testInitializedInFor2() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			for (int i = 0; i < 0; i++) {
				j = i;
			}
			""", "");
	}

	@Test
	public void testInitializedInFor3() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			int i;
			for (i = 0; i < 0; i++) {
				j = i;
			}
			System.out.println(i);  // OK
			""", "");
	}

	@Test
	public void testNotInitializedInFor() throws Exception {
		assertNotInitializedReferences("""
			int j=0;
			int i;
			for (; i < 0; i++) {
				j = i;
			}
			""", """
			i in i < 0
			i in i++
			i in j = i\
			""");
	}

	@Test
	public void testNotInitializedInFor2() throws Exception {
		assertNotInitializedReferences("""
			int j;
			int i = 0;
			int k;
			for (k = 0; k < 0; j = i + 1) {
				j = i;
			}
			System.out.println(j); // ERROR
			System.out.println(k); // OK
			""", "j in System.out.println(j)");
	}

	@Test
	public void testNotInitializedInForEach() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;
			for (int ii : integers) {
				j = i;
			}
			""", "i in j = i");
	}

	@Test
	public void testNotInitializedInForEach2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;
			for (int ii : integers) {
				i = 0;
			}
			j = i;
			""", "i in j = i");
	}

	@Test
	public void testNotInitializedInWhile() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			while (j < 0) {
				i = 0;
			}
			j = i; // ERROR
			""", "i in j = i");
	}

	@Test
	public void testNotInitializedInWhile2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			while (j < 0) {
				j = i; // ERROR
			}
			""", "i in j = i");
	}

	@Test
	public void testNotInitializedInWhile3() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			while ((i = j) < 0) {
				System.out.println(i); // OK
			}
			System.out.println(i); // OK
			""", "");
	}

	@Test
	public void testNotInitializedInDoWhile() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j = 0;
			do {
				j = i;
			} while (i < 0);
			""", """
			i in j = i
			i in i < 0\
			""");
	}

	@Test
	public void testInitializedInDoWhile() throws Exception {
		assertNotInitializedReferences("""
			int i;
			do {
				i = 0;
			} while (i < 0);
			System.out.println(i);
			""", "");
	}

	@Test
	public void testInitializedInDoWhile2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;
			do {
				j = 0;
			} while ((i = j) < 0);
			System.out.println(i);
			""", "");
	}

	@Test
	public void testInitializedInSwitchCase() throws Exception {
		// cases without break are inspected before the cases
		// with break, that's why i in z = i; is detected before
		assertNotInitializedReferences("""
			nt i;
			int k;
			int z;
			int w;

			switch (key) {
			case -1:
				k = i; // ERROR
				break;
			case 0:
				z = i; // ERROR
				i = 0;
				z = i ;
			default:
				w = i; // ERROR
				i = 0;
				break;
			}
			""", """
			i in z = i
			i in k = i
			i in w = i\
			""");
	}

	@Test
	public void testInitializedInSwitch() throws Exception {
		assertNotInitializedReferences("""
			int key = 0;
			int i;
			int j;

			switch (key) {
			case 0:
				i = 0;
				break;
			default:
				i = 0;
				j = 0;
				break;
			}

			System.out.println(i); // OK
			System.out.println(j); // ERROR
			""", "j in System.out.println(j)");
	}

	@Test
	public void testInitializedInSwitch2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case -1:
				j = 0;
				break;
			case 0:
				i = 0;
				break;
			default:
				i = 0;
				j = 0;
				break;
			}

			System.out.println(i); // ERROR
			System.out.println(j); // ERROR
			""", """
			i in System.out.println(i)
			j in System.out.println(j)\
			""");
	}

	@Test
	public void testInitializedInSwitch3() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case -1:
				j = 0;
			case 0:
				i = 0;
				break;
			default:
				i = 0;
				j = 0;
				break;
			}

			System.out.println(i); // OK
			System.out.println(j); // ERROR
			""", "j in System.out.println(j)");
	}

	@Test
	public void testInitializedInSwitch4() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case -1:
				j = 0;
				break;
			case 0:
				i = 0;
			default:
				i = 0;
				j = 0;
				break;
			}

			System.out.println(i); // ERROR
			System.out.println(j); // OK
			""", "i in System.out.println(i)");
	}

	@Test
	public void testInitializedInSwitchWithoutDefault() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case -1:
				j = 0;
				break;
			case 0:
				i = 0;
			}

			System.out.println(i); // ERROR
			System.out.println(j); // ERROR
			""", """
			i in System.out.println(i)
			j in System.out.println(j)\
			""");
	}

	@Test
	public void testInitializedInSwitchWithoutDefault2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case -1:
				j = 0;
			case 0:
				i = 0;
			}

			System.out.println(i); // ERROR
			System.out.println(j); // ERROR
			""", """
			i in System.out.println(i)
			j in System.out.println(j)\
			""");
	}

	@Test
	public void testInitializedInSwitchWithoutDefault3() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case -1:
				j = 0;
			case 0:
				i = 0;
				j = 0;
			}

			System.out.println(i); // ERROR
			System.out.println(j); // ERROR
			""", """
			i in System.out.println(i)
			j in System.out.println(j)\
			""");
	}

	@Test
	public void testInitializedInSwitchWithoutDefault4() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case -1:
				j = 0;
				break;
			case 0:
				i = 0;
				j = 0;
				break;
			}

			System.out.println(i); // ERROR
			System.out.println(j); // ERROR
			""", """
			i in System.out.println(i)
			j in System.out.println(j)\
			""");
	}

	@Test
	public void testInitializedInSwitchWithoutDefault5() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			case 0:
				i = 0;
				j = 0;
				break;
			}

			System.out.println(i); // ERROR
			System.out.println(j); // ERROR
			""", """
			i in System.out.println(i)
			j in System.out.println(j)\
			""");
	}

	@Test
	public void testInitializedInSwitchWithOnlyDefault() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;

			switch (key) {
			default:
				i = 0;
				j = 0;
				break;
			}

			System.out.println(i); // OK
			System.out.println(j); // OK
			""", "");
	}

	@Test
	public void testInitializedOnlyInTry() throws Exception {
		assertNotInitializedReferences("""
			int i;
			try {
				i = 0;
			} catch (NullPointerException e) {
			\t
			}
			System.out.println(i);
			""", "i in System.out.println(i)");
	}

	@Test
	public void testInitializedOnlyInCatch() throws Exception {
		assertNotInitializedReferences("""
			int i;
			try {
			\t
			} catch (NullPointerException e) {
				i = 0;
			}
			System.out.println(i);
			""", "i in System.out.println(i)");
	}

	@Test
	public void testInitializedBothInTryAndInCatch() throws Exception {
		assertNotInitializedReferences("""
			int i;
			try {
				i = 0;
			} catch (NullPointerException e) {
				i = 0;
			}
			System.out.println(i);
			""", "");
	}

	@Test
	public void testInitializedBothInTryAndInCatches() throws Exception {
		assertNotInitializedReferences("""
			int i;
			try {
				i = 0;
			} catch (NullPointerException e) {
				i = 0;
			} catch (IllegalArgumentException e) {
				i = 0;
			}
			System.out.println(i);
			""", "");
	}

	@Test
	public void testInitializedOnlyInFinally() throws Exception {
		assertNotInitializedReferences("""
			int i;
			try {
			\t
			} catch (NullPointerException e) {
			\t
			} finally {
				i = 0; // always executed
			}
			System.out.println(i);
			""", "");
	}

	@Test
	public void testInitializedNotInFinally() throws Exception {
		assertNotInitializedReferences("""
			int i;
			int j;
			try {
				j = 0;
			} catch (NullPointerException e) {
				j = 0;
			} finally {
				i = 0;
				i = j; // ERROR
			}
			System.out.println(i); // OK
			System.out.println(j); // OK
			""", "j in i = j");
	}

	@Test
	public void testInitializedInTryWithResources() throws Exception {
		assertNotInitializedReferences("""
			try (String s = "") {
				System.out.println(s); // OK
			}
			""", "");
	}

	@Test
	public void testInitializedInTryWithResourcesCatch() throws Exception {
		assertNotInitializedReferences("""
			int i;
			try (String s = "") {
				i = 0;
				System.out.println(s); // OK
			} catch (NullPointerException e) {
				i = 0;
			}
			System.out.println(i); // OK
			""", "");
	}

	@Test
	public void testInitializedInTryWithResourcesCatch2() throws Exception {
		assertNotInitializedReferences("""
			int i;
			try (String s = "") {
				System.out.println(s); // OK
			} catch (NullPointerException e) {
				i = 0;
			}
			System.out.println(i); // ERROR
			""", "i in System.out.println(i)");
	}

	@Test
	public void testInitializedAfterSynchronized() throws Exception {
		assertNotInitializedReferences("""
			int i, j;
			Object o = null;
			synchronized(o) {
				i = 0;
			}
			System.out.println(i); // OK
			System.out.println(j); // ERROR
			""", "j in System.out.println(j)");
	}

	@Test
	public void testNotInitializedInSynchronized() throws Exception {
		assertNotInitializedReferences("""
			int i, j;
			Object o;
			synchronized(o) { // ERROR
				i = 0;
			}
			System.out.println(i); // OK
			""", "o in synchronized(o) { i = 0; }");
	}

	private void assertNotInitializedReferences(final CharSequence input,
			final CharSequence expected) throws Exception {
		// we record the container of not initialized reference
		// so that it's easier to tell the occurrence in the test input
		final var builder = new StringBuilder();
		final var blockToParse = "{\n" + input + "\n}";
		jbaseInitializedVariableFinder
				.detectNotInitialized((XBlockExpression) parseHelper.parse(blockToParse), ref -> {
					if (builder.length() > 0) {
						builder.append("\n");
					}
					builder.append(ref.toString() + " in "
							+ jbaseNodeModelUtil.getProgramText(ref.eContainer()));
				});
		assertEqualsStrings(expected, builder.toString());
	}
}
