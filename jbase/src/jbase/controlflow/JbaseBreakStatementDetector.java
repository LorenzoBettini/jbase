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
