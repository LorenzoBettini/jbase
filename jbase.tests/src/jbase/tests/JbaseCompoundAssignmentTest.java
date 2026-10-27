package jbase.tests;

import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseCompoundAssignmentTest extends JbaseAbstractCompilerTest {

	@Test
	public void narrowingOfOrdinaryCompoundAssignments() throws Exception {
		final var source = """
			method kind(byte value): String { return "byte"; }
			method kind(int value): String { return "int"; }
			method testMe() {
				byte b = 127;
				b += 1;
				b -= 1;
				b *= 2;
				b /= 2;
				b %= 2;
				b |= 2;
				b &= 3;
				b ^= 1;
				byte assigned = b += 1;
				System.out.println(assigned);
				System.out.println(kind(b += 1));
				short s = 32767;
				s += 1;
				System.out.println(s);
				char c = 65535;
				c += 1;
				System.out.println((int) c);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "3\nbyte\n-32768\n0\n");
	}

	@Test
	public void otherOrdinaryCompoundAssignmentsKeepJavaBehavior() throws Exception {
		final var source = """
			method testMe() {
				boolean enabled = true;
				enabled &= false;
				enabled |= true;
				enabled ^= true;
				System.out.println(enabled);
				String text = "value";
				text += 2;
				System.out.println(text);
				double decimal = 1.5;
				decimal += 1;
				System.out.println(decimal);
			}
			""";
		checkCompilation(source, null);
		assertExecuteMain(source, "false\nvalue2\n2.5\n");
	}
}
