package jbase.controlflow;

import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XIfExpression;

import jbase.jbase.XJBranchingStatement;
import jbase.jbase.XJLabeledStatement;
import jbase.jbase.XJSemicolonStatement;

/**
 * Whether in the passed expression a branching statement like break
 * and continue is surely executed.
 *
 * @author Lorenzo Bettini
 */
public class JbaseBranchingStatementDetector {

	/**
	 * Applies the structural branch-statement check used by control-flow analysis.
	 * Recognizes branching statements through semicolon wrappers, blocks, and conditional
	 * branches. A conditional without an else branch is sure only when its condition is
	 * the literal {@code true}; this helper does not perform a complete reachability analysis.
	 *
	 * @param e the expression to inspect; may be {@code null}
	 * @return whether the expression satisfies the structural check; false for {@code null}
	 */
	public boolean isSureBranchStatement(final XExpression e) {
		return e != null && sureBranch(e);
	}

	protected boolean sureBranch(final XExpression e) {
		return switch (e) {
			case null -> throw new IllegalArgumentException("Unhandled parameter types: [null]");
			case XJBranchingStatement statement -> true;
			case XJLabeledStatement statement ->
				isSureBranchStatement(statement.getStatement());
			case XJSemicolonStatement statement ->
				isSureBranchStatement(statement.getExpression());
			case XBlockExpression block ->
				block.getExpressions().stream().anyMatch(this::isSureBranchStatement);
			case XIfExpression conditional -> isSureBranchStatement(conditional.getThen())
				&& (conditional.getElse() == null
					? conditional.getIf() instanceof XBooleanLiteral literal
						&& literal.isIsTrue()
					: isSureBranchStatement(conditional.getElse()));
			default -> false;
		};
	}
}
