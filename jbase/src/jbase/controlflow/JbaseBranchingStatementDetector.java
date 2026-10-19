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
			case XJSemicolonStatement statement -> _sureBranch(statement);
			case XBlockExpression block -> _sureBranch(block);
			case XIfExpression conditional -> _sureBranch(conditional);
			default -> false;
		};
	}

	protected boolean _sureBranch(final XIfExpression e) {
		return isSureBranchStatement(e.getThen())
			&& (e.getElse() == null || isSureBranchStatement(e.getElse()));
	}

	protected boolean _sureBranch(final XBlockExpression e) {
		return e.getExpressions().stream().anyMatch(this::isSureBranchStatement);
	}

	protected boolean _sureBranch(final XJSemicolonStatement e) {
		return isSureBranchStatement(e.getExpression());
	}
}
