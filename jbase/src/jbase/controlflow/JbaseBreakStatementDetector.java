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
			case XJSemicolonStatement statement -> _containsPossibleBreak(statement);
			case XAbstractWhileExpression loop -> _containsPossibleBreak(loop);
			case XBasicForLoopExpression loop -> _containsPossibleBreak(loop);
			default -> _containsPossibleBreak(e);
		};
	}

	protected boolean _containsPossibleBreak(final XExpression e) {
		return false;
	}

	protected boolean _containsPossibleBreak(final XJSemicolonStatement e) {
		return containsPossibleBreakStatement(e.getExpression());
	}

	protected boolean _containsPossibleBreak(final XAbstractWhileExpression e) {
		return isPossibleBreakStatement(e.getBody());
	}

	protected boolean _containsPossibleBreak(final XBasicForLoopExpression e) {
		return isPossibleBreakStatement(e.getEachExpression());
	}

	public boolean isPossibleBreakStatement(final XExpression e) {
		return e != null && possibleBreak(e);
	}

	protected boolean possibleBreak(final XExpression e) {
		return switch (e) {
			case null -> throw new IllegalArgumentException("Unhandled parameter types: [null]");
			case XJBreakStatement statement -> _possibleBreak(statement);
			case XJSemicolonStatement statement -> _possibleBreak(statement);
			case XBlockExpression block -> _possibleBreak(block);
			case XIfExpression conditional -> _possibleBreak(conditional);
			default -> _possibleBreak(e);
		};
	}

	protected boolean _possibleBreak(final XExpression e) {
		return false;
	}

	protected boolean _possibleBreak(final XJBreakStatement e) {
		return true;
	}

	protected boolean _possibleBreak(final XIfExpression e) {
		return isPossibleBreakStatement(e.getThen())
			|| (e.getElse() != null && isPossibleBreakStatement(e.getElse()));
	}

	protected boolean _possibleBreak(final XBlockExpression e) {
		return e.getExpressions().stream().anyMatch(this::isPossibleBreakStatement);
	}

	protected boolean _possibleBreak(final XJSemicolonStatement e) {
		return isPossibleBreakStatement(e.getExpression());
	}
}
