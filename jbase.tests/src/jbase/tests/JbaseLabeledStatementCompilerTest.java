package jbase.tests;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.google.inject.Inject;
import jbase.compiler.JbaseXbaseCompiler;
import jbase.jbase.XJContinueStatement;
import jbase.jbase.XJLabeledStatement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests Java generation and continue-target resolution for labeled statements.
 */
@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseLabeledStatementCompilerTest extends JbaseAbstractCompilerTest {

	/**
	 * Exposes the compiler's protected continue-target resolver to focused tests.
	 */
	public static class CompilerWithContinueTargetAccess extends JbaseXbaseCompiler {

		/**
		 * Resolves the loop updated by the given continue, if it is a basic-for target.
		 *
		 * @param statement the continue statement to resolve
		 * @return the targeted basic-for loop, or {@code null} otherwise
		 */
		public XBasicForLoopExpression continueTarget(final XJContinueStatement statement) {
			return getContinueTarget(statement);
		}
	}

	private final CompilerWithContinueTargetAccess compiler =
		new CompilerWithContinueTargetAccess();

	@Inject
	private org.eclipse.xtext.testing.util.ParseHelper<EObject> objectParseHelper;

	@Test
	public void continueTargetFindsEnclosingLabeledForThroughOtherLabels() throws Exception {
		final var model = objectParseHelper.parse("""
			outer: for (int i = 0; i < 1; i++) {
				inner: {
					continue outer;
				}
			}
			""");
		final var statement = EcoreUtil2.getAllContentsOfType(model, XJContinueStatement.class)
			.getFirst();
		final var label = EcoreUtil2.getAllContentsOfType(model, XJLabeledStatement.class)
			.getFirst();
		assertSame(label.getStatement(),
			compiler.continueTarget(statement));
	}

	@Test
	public void continueTargetReturnsNullForNonBasicLoopLabel() throws Exception {
		final var model = objectParseHelper.parse("""
			outer: while (true) continue outer;
			""");
		final var statement = EcoreUtil2.getAllContentsOfType(model, XJContinueStatement.class)
			.getFirst();
		assertNull(compiler.continueTarget(statement));
	}

	@Test
	public void continueTargetStopsAtLambdaBoundary() throws Exception {
		final var model = objectParseHelper.parse("""
			outer: for (int i = 0; i < 1; i++) {
				java.lang.Runnable action = () -> {
					continue outer;
				};
			}
			""");
		final var statement = EcoreUtil2.getAllContentsOfType(model, XJContinueStatement.class)
			.getFirst();
		assertNull(compiler.continueTarget(statement));
	}

	@Test
	public void continueTargetStopsAtAnonymousClassBoundary() throws Exception {
		final var model = objectParseHelper.parse("""
			outer: for (int i = 0; i < 1; i++) {
				var action = new Object() {
					method run() {
						continue outer;
					}
				};
			}
			""");
		final var statement = EcoreUtil2.getAllContentsOfType(model, XJContinueStatement.class)
			.getFirst();
		assertNull(compiler.continueTarget(statement));
	}

	@Test
	public void continueTargetReturnsNullForMissingLabel() throws Exception {
		final var model = objectParseHelper.parse("continue missing;");
		final var statement = EcoreUtil2.getAllContentsOfType(model, XJContinueStatement.class)
			.getFirst();
		assertNull(compiler.continueTarget(statement));
	}

	@Test
	public void labeledContinueTargetsEnclosingWhileLoop() throws Exception {
		assertExecuteMain("""
			op testMe() : void {
				int count = 0;
				outer: while (count < 3) {
					count++;
					continue outer;
				}
				System.out.println(count);
			}
			""", "3\n");
	}

	@Test
	public void labeledForThatStaysAForLoopCompiles() throws Exception {
		assertExecuteMain("""
			op testMe() : void {
				outer: for (int i = 0; i < 3; i++) {
					if (i == 1) continue outer;
				}
				System.out.println("done");
			}
			""", "done\n");
	}

	@Test
	public void labeledBlockDoesNotLabelNestedTranslatedForLoop() throws Exception {
		assertExecuteMain("""
			op testMe() : void {
				int count = 0;
				outer: {
					for (int i = 0; i < 3; i += 1) {
						count++;
						if (i == 1) break;
					}
				}
				System.out.println(count);
			}
			""", "2\n");
	}

	@Test
	public void labeledBreakExitsBlockAndLoop() throws Exception {
		assertExecuteMain("""
			op testMe() : void {
				int count = 0;
				exit: {
					count++;
					break exit;
				}
				outer: for (int i = 0; i < 5; i += 1) {
					count++;
					break outer;
				}
				System.out.println(count);
			}
			""", "2\n");
	}

	@Test
	public void labeledContinueUpdatesTranslatedForLoop() throws Exception {
		assertExecuteMain("""
			op testMe() : void {
				int count = 0;
				outer: for (int i = 0; i < 3; i += 1) {
					while (i < 3) {
						count++;
						continue outer;
					}
				}
				System.out.println(count);
			}
			""", "3\n");
	}
}
