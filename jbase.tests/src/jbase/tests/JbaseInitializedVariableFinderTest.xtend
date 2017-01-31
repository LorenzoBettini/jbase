package jbase.tests

import com.google.inject.Inject
import jbase.util.JbaseNodeModelUtil
import jbase.validation.JbaseInitializedVariableFinder
import jbase.validation.JbaseInitializedVariableFinder.InitializedVariables
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.xbase.XBlockExpression
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseInitializedVariableFinderTest extends JbaseAbstractTest {

	@Inject extension JbaseInitializedVariableFinder
	@Inject extension JbaseNodeModelUtil

	@Test def void testNotInitializedNull() {
		// test it does not throw
		null.detectNotInitialized[]
	}

	@Test(expected=IllegalArgumentException)
	def void testNotInitializedInternalNull() {
		null.detectNotInitialized(new InitializedVariables)[]
	}

	@Test def void testNotInitializedWithNotVariableReference() {
		'''
		foo;
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testNotInitializedInCall() {
		'''
		int i;
		int j = 0;
		System.out.println(i);
		System.out.println(j);
		'''.
		assertNotInitializedReferences("i in System.out.println(i)")
	}

	@Test def void testNotInitializedInCall2() {
		'''
		int i;
		int j = 0;
		System.out.println( i );
		i = 1; // now initialized
		System.out.println(i);
		System.out.println(j);
		'''.
		assertNotInitializedReferences("i in System.out.println( i )")
	}

	@Test def void testNotInitializedInCall3() {
		'''
		int i;
		int j = 0;
		System.out.println( j < i );
		'''.
		assertNotInitializedReferences("i in j < i")
	}

	@Test def void testInitializedInCall() {
		'''
		int i;
		int j = 0;
		System.out.println( (i = j) == (j = i) );
		System.out.println(i); // now initialized
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testNotInitializedInCall4() {
		'''
		int i;
		int j = 0;
		System.out.println( (j = i) == (i = j) );
		'''.
		assertNotInitializedReferences("i in (j = i)")
	}

	@Test def void testNotInitializedInUnaryOperation() {
		'''
		int i;
		int j = -i;
		'''.
		assertNotInitializedReferences("i in -i")
	}

	@Test def void testNotInitializedInAssignment() {
		'''
		int i;
		int j;
		j = i;
		'''.
		assertNotInitializedReferences("i in j = i")
	}

	@Test def void testNotInitializedInAssignment2() {
		'''
		int i;
		foo = i;
		'''.
		assertNotInitializedReferences("i in foo = i")
		// even if foo is unresolved we still inspect the contents
	}

	@Test def void testNotInitializedInVariableDeclaration() {
		'''
		int i;
		int j = i;
		i = j; // OK
		'''.
		assertNotInitializedReferences("i in int j = i")
	}

	@Test def void testNotInitializedInSeveralVariableDeclarations() {
		'''
		int i;
		int z = 0;
		int j = i, k = i, w = z;
		i = j; // OK
		'''.
		assertNotInitializedReferences(
		'''
		i in int j = i, k = i, w = z
		i in k = i'''
		)
	}

	@Test def void testNotInitializedInSeveralVariableDeclarations2() {
		'''
		int i;
		int z = 0;
		int j, k = i, w = z;
		i = k; // OK
		'''.
		assertNotInitializedReferences("i in k = i")
	}

	@Test def void testNotInitializedInArrayAccess() {
		'''
		int i;
		int[] a = null;
		a[i] = 0;
		'''.
		assertNotInitializedReferences("i in a[i] = 0")
	}

	@Test def void testNotInitializedInArrayAccess2() {
		'''
		int i;
		int[] a = null;
		i = a[i];
		'''.
		assertNotInitializedReferences("i in a[i]")
	}

	@Test def void testInitializedInArrayAccess() {
		'''
		int i;
		int[] a = null;
		a[i=1] = 0;
		System.out.println(i);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInArrayAccess2() {
		'''
		int i;
		int[] a = null;
		System.out.println(a[i=1]);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testNotInitializedInArrayConstructor() {
		'''
		int i;
		int[] a = new int[i];
		'''.
		assertNotInitializedReferences("i in new int[i]")
	}

	@Test def void testInitializedInArrayConstructor() {
		'''
		int i;
		int[] a = new int[i=1];
		System.out.println(i);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testNotInitializedAfterBlock() {
		'''
		int i;
		int j;
		int k;
		{
			j = 0;
		}
		k = i; // ERROR
		k = j; // OK
		'''.
		assertNotInitializedReferences("i in k = i")
		// even if foo is unresolved we still inspect the contents
	}

	@Test def void testNotInitializedInIf() {
		'''
		int i;
		if (i) {
			
		}
		'''.
		assertNotInitializedReferences("i in if (i) { }")
	}

	@Test def void testNotInitializedInIf2() {
		'''
		int j=0;
		int i;
		if (j) {
			j = i;
		}
		'''.
		assertNotInitializedReferences("i in j = i")
	}

	@Test def void testNotInitializedInIf3() {
		'''
		int j=0;
		int i;
		if (j) {
			j = 0;
		} else {
			int k = i;
		}
		'''.
		assertNotInitializedReferences("i in int k = i")
	}

	@Test def void testNotInitializedInIf4() {
		'''
		int j=0;
		int i;
		if (j) {
			i = 0;
		}
		System.out.println(i); // ERROR
		'''.
		assertNotInitializedReferences("i in System.out.println(i)")
	}

	@Test def void testInitializedInIfBothBranches() {
		'''
		int j=0;
		int i;
		if (j) {
			i = 0;
		} else {
			i = 1;
		}
		System.out.println(i); // OK
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInConditional() {
		'''
		int i;
		int j;
		int k;
		int z;
		boolean b = (i = 0) < (j = i) ? (k = 1) > 0 : (k = 1) > (z = 1);
		System.out.println(i); // OK
		System.out.println(j); // OK
		System.out.println(k); // OK
		System.out.println(z); // ERROR
		'''.
		assertNotInitializedReferences("z in System.out.println(z)")
	}

	@Test def void testInitializedInFor() {
		'''
		int j=0;
		int i;
		for (i = 0; i < 0; i++) {
			j = i;
		}
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInFor2() {
		'''
		int j=0;
		for (int i = 0; i < 0; i++) {
			j = i;
		}
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInFor3() {
		'''
		int j=0;
		int i;
		for (i = 0; i < 0; i++) {
			j = i;
		}
		System.out.println(i);  // OK
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testNotInitializedInFor() {
		'''
		int j=0;
		int i;
		for (; i < 0; i++) {
			j = i;
		}
		'''.
		assertNotInitializedReferences(
		'''
		i in i < 0
		i in i++
		i in j = i'''
		)
	}

	@Test def void testNotInitializedInFor2() {
		'''
		int j;
		int i = 0;
		int k;
		for (k = 0; k < 0; j = i + 1) {
			j = i;
		}
		System.out.println(j); // ERROR
		System.out.println(k); // OK
		'''.
		assertNotInitializedReferences("j in System.out.println(j)")
	}

	@Test def void testNotInitializedInForEach() {
		'''
		int i;
		int j;
		for (int ii : integers) {
			j = i;
		}
		'''.
		assertNotInitializedReferences("i in j = i")
	}

	@Test def void testNotInitializedInForEach2() {
		'''
		int i;
		int j;
		for (int ii : integers) {
			i = 0;
		}
		j = i;
		'''.
		assertNotInitializedReferences("i in j = i")
	}

	@Test def void testNotInitializedInWhile() {
		'''
		int i;
		int j = 0;
		while (j < 0) {
			i = 0;
		}
		j = i; // ERROR
		'''.
		assertNotInitializedReferences("i in j = i")
	}

	@Test def void testNotInitializedInWhile2() {
		'''
		int i;
		int j = 0;
		while (j < 0) {
			j = i; // ERROR
		}
		'''.
		assertNotInitializedReferences("i in j = i")
	}

	@Test def void testNotInitializedInWhile3() {
		'''
		int i;
		int j = 0;
		while ((i = j) < 0) {
			System.out.println(i); // OK
		}
		System.out.println(i); // OK
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testNotInitializedInDoWhile() {
		'''
		int i;
		int j = 0;
		do {
			j = i;
		} while (i < 0);
		'''.
		assertNotInitializedReferences(
		'''
		i in j = i
		i in i < 0'''
		)
	}

	@Test def void testInitializedInDoWhile() {
		'''
		int i;
		do {
			i = 0;
		} while (i < 0);
		System.out.println(i);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInDoWhile2() {
		'''
		int i;
		int j;
		do {
			j = 0;
		} while ((i = j) < 0);
		System.out.println(i);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInSwitchCase() {
		'''
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
		'''.
		assertNotInitializedReferences('''
		i in z = i
		i in k = i
		i in w = i'''
		)
		// cases without break are inspected before the cases
		// with break, that's why i in z = i; is detected before
	}

	@Test def void testInitializedInSwitch() {
		'''
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
		'''.
		assertNotInitializedReferences("j in System.out.println(j)")
	}

	@Test def void testInitializedInSwitch2() {
		'''
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
		'''.
		assertNotInitializedReferences(
		'''
		i in System.out.println(i)
		j in System.out.println(j)'''
		)
	}

	@Test def void testInitializedInSwitch3() {
		'''
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
		'''.
		assertNotInitializedReferences("j in System.out.println(j)")
	}

	@Test def void testInitializedInSwitch4() {
		'''
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
		'''.
		assertNotInitializedReferences("i in System.out.println(i)")
	}

	@Test def void testInitializedInSwitchWithoutDefault() {
		'''
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
		'''.
		assertNotInitializedReferences('''
		i in System.out.println(i)
		j in System.out.println(j)'''
		)
	}

	@Test def void testInitializedInSwitchWithoutDefault2() {
		'''
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
		'''.
		assertNotInitializedReferences('''
		i in System.out.println(i)
		j in System.out.println(j)'''
		)
	}

	@Test def void testInitializedInSwitchWithoutDefault3() {
		'''
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
		'''.
		assertNotInitializedReferences('''
		i in System.out.println(i)
		j in System.out.println(j)'''
		)
	}

	@Test def void testInitializedInSwitchWithoutDefault4() {
		'''
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
		'''.
		assertNotInitializedReferences('''
		i in System.out.println(i)
		j in System.out.println(j)'''
		)
	}

	@Test def void testInitializedInSwitchWithoutDefault5() {
		'''
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
		'''.
		assertNotInitializedReferences('''
		i in System.out.println(i)
		j in System.out.println(j)'''
		)
	}

	@Test def void testInitializedInSwitchWithOnlyDefault() {
		'''
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
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedOnlyInTry() {
		'''
		int i;
		try {
			i = 0;
		} catch (NullPointerException e) {
			
		}
		System.out.println(i);
		'''.
		assertNotInitializedReferences("i in System.out.println(i)")
	}

	@Test def void testInitializedOnlyInCatch() {
		'''
		int i;
		try {
			
		} catch (NullPointerException e) {
			i = 0;
		}
		System.out.println(i);
		'''.
		assertNotInitializedReferences("i in System.out.println(i)")
	}

	@Test def void testInitializedBothInTryAndInCatch() {
		'''
		int i;
		try {
			i = 0;
		} catch (NullPointerException e) {
			i = 0;
		}
		System.out.println(i);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedBothInTryAndInCatches() {
		'''
		int i;
		try {
			i = 0;
		} catch (NullPointerException e) {
			i = 0;
		} catch (IllegalArgumentException e) {
			i = 0;
		}
		System.out.println(i);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedOnlyInFinally() {
		'''
		int i;
		try {
			
		} catch (NullPointerException e) {
			
		} finally {
			i = 0; // always executed
		}
		System.out.println(i);
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedNotInFinally() {
		'''
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
		'''.
		assertNotInitializedReferences("j in i = j")
	}

	@Test def void testInitializedInTryWithResources() {
		'''
		try (String s = "") {
			System.out.println(s); // OK
		}
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInTryWithResourcesCatch() {
		'''
		int i;
		try (String s = "") {
			i = 0;
			System.out.println(s); // OK
		} catch (NullPointerException e) {
			i = 0;
		}
		System.out.println(i); // OK
		'''.
		assertNotInitializedReferences("")
	}

	@Test def void testInitializedInTryWithResourcesCatch2() {
		'''
		int i;
		try (String s = "") {
			System.out.println(s); // OK
		} catch (NullPointerException e) {
			i = 0;
		}
		System.out.println(i); // ERROR
		'''.
		assertNotInitializedReferences("i in System.out.println(i)")
	}

	@Test def void testInitializedAfterSynchronized() {
		'''
		int i, j;
		Object o = null;
		synchronized(o) {
			i = 0;
		}
		System.out.println(i); // OK
		System.out.println(j); // ERROR
		'''.
		assertNotInitializedReferences("j in System.out.println(j)")
	}

	@Test def void testNotInitializedInSynchronized() {
		'''
		int i, j;
		Object o;
		synchronized(o) { // ERROR
			i = 0;
		}
		System.out.println(i); // OK
		'''.
		assertNotInitializedReferences("o in synchronized(o) { i = 0; }")
	}

	private def assertNotInitializedReferences(CharSequence input, CharSequence expected) {
		val builder = new StringBuilder()
		val blockToParse = '''{
			«input»
		}'''
		// we record the container of not initialized reference
		// so that it's easier to tell the occurrence in the test input
		(blockToParse.parse as XBlockExpression).detectNotInitialized[
			ref |
			if (builder.length > 0) {
				builder.append("\n")
			}
			builder.append(ref.toString + " in " + ref.eContainer.programText)
		]
		assertEqualsStrings(
			expected,
			builder.toString
		)
	}
}