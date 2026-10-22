package jbase.controlflow;

import org.eclipse.xtext.xbase.XAbstractWhileExpression;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XIfExpression;

import jbase.jbase.XJBreakStatement;
import jbase.jbase.XJSemicolonStatement;

/**
 * Whether in the passed expression a break is possibly executed.
 *
 * @author Lorenzo Bettini
 */
public class JbaseBreakStatementDetector {

	/**
	 * Checks for a possible break in a while or basic-for loop's body.
	 * Semicolon wrappers are unwrapped. Nested loops are not traversed when checking
	 * whether a break may exit the inspected loop.
	 *
	 * @param e the loop or semicolon-wrapped loop to inspect; may be {@code null}
	 * @return whether its body contains a possible break; false for other expressions
	 */
	public boolean containsPossibleBreakStatement(final XExpression e) {
		return e != null && containsPossibleBreak(e);
	}

	protected boolean containsPossibleBreak(final XExpression e) {
		return switch (e) {
			case null -> throw new IllegalArgumentException("Unhandled parameter types: [null]");
			case XJSemicolonStatement statement ->
				containsPossibleBreakStatement(statement.getExpression());
			case XAbstractWhileExpression loop -> isPossibleBreakStatement(loop.getBody());
			case XBasicForLoopExpression loop -> isPossibleBreakStatement(loop.getEachExpression());
			default -> false;
		};
	}

	/**
	 * Searches an expression for a break through wrappers, blocks, and either if branch.
	 * Other constructs, including nested loops, are not traversed, so their breaks do
	 * not count as exits from the enclosing loop being analyzed.
	 *
	 * @param e the expression or loop body to inspect; may be {@code null}
	 * @return whether a break is structurally reachable; false for {@code null}
	 */
	public boolean isPossibleBreakStatement(final XExpression e) {
		return e != null && possibleBreak(e);
	}

	protected boolean possibleBreak(final XExpression e) {
		return switch (e) {
			case null -> throw new IllegalArgumentException("Unhandled parameter types: [null]");
			case XJBreakStatement statement -> true;
			case XJSemicolonStatement statement ->
				isPossibleBreakStatement(statement.getExpression());
			case XBlockExpression block ->
				block.getExpressions().stream().anyMatch(this::isPossibleBreakStatement);
			case XIfExpression conditional -> isPossibleBreakStatement(conditional.getThen())
				|| (conditional.getElse() != null
					&& isPossibleBreakStatement(conditional.getElse()));
			default -> false;
		};
	}
}
