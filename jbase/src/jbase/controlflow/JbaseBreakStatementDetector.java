package jbase.controlflow;

import org.eclipse.xtext.xbase.XAbstractWhileExpression;
import org.eclipse.xtext.xbase.XBasicForLoopExpression;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XForLoopExpression;
import org.eclipse.xtext.xbase.XIfExpression;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.EcoreUtil2;

import jbase.jbase.XJAnonymousClassBody;
import jbase.jbase.XJBreakStatement;
import jbase.jbase.XJLabeledStatement;
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
			case XAbstractWhileExpression loop ->
				containsBreakExitingTarget(loop.getBody(), loop);
			case XBasicForLoopExpression loop ->
				containsBreakExitingTarget(loop.getEachExpression(), loop);
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
			case XJLabeledStatement statement ->
				isPossibleBreakStatement(statement.getStatement());
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

	private boolean containsBreakExitingTarget(final XExpression expression,
			final XExpression target) {
		return expression != null
			&& EcoreUtil2.getAllContentsOfType(expression, XJBreakStatement.class).stream()
			.anyMatch(statement -> exitsTarget(statement, target));
	}

	private boolean exitsTarget(final XJBreakStatement statement, final XExpression target) {
		var targetIsBetweenBranchAndLabel = false;
		for (var parent = statement.eContainer(); parent != null; parent = parent.eContainer()) {
			if (parent == target) {
				if (statement.getLabel() == null) {
					return true;
				}
				targetIsBetweenBranchAndLabel = true;
			}
			if (statement.getLabel() == null
					&& (parent instanceof XAbstractWhileExpression
						|| parent instanceof XBasicForLoopExpression
						|| parent instanceof XForLoopExpression
						|| parent instanceof XSwitchExpression)) {
				return false;
			}
			if (parent instanceof XClosure || parent instanceof XJAnonymousClassBody) {
				return false;
			}
			if (statement.getLabel() != null && parent instanceof XJLabeledStatement labeled
					&& statement.getLabel().equals(labeled.getName())) {
				return targetIsBetweenBranchAndLabel;
			}
		}
		return false;
	}
}
