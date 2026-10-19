package jbase.controlflow;

import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XIfExpression;

import jbase.jbase.XJBranchingStatement;
import jbase.jbase.XJSemicolonStatement;

/**
 * Whether in the passed expression a branching statement like break
 * and continue is surely executed.
 *
 * @author Lorenzo Bettini
 */
public class JbaseBranchingStatementDetector {

	public boolean isSureBranchStatement(final XExpression e) {
		return e != null && sureBranch(e);
	}

	protected boolean sureBranch(final XExpression e) {
		return switch (e) {
			case null -> throw new IllegalArgumentException("Unhandled parameter types: [null]");
			case XJBranchingStatement statement -> true;
			case XJSemicolonStatement statement ->
				isSureBranchStatement(statement.getExpression());
			case XBlockExpression block ->
				block.getExpressions().stream().anyMatch(this::isSureBranchStatement);
			case XIfExpression conditional -> isSureBranchStatement(conditional.getThen())
				&& (conditional.getElse() == null
					|| isSureBranchStatement(conditional.getElse()));
			default -> false;
		};
	}
}
