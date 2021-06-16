package jbase.controlflow;

import java.util.Collection;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.controlflow.DefaultEarlyExitComputer;

import jbase.jbase.XJSemicolonStatement;

/**
 * Handles semicolon statement containers.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseSemicolonStatementAwareEarlyExitComputer extends DefaultEarlyExitComputer {
	/**
	 * A semicolon statement wraps an expression, so we delegate to the contained
	 * expression.
	 * 
	 * @return
	 */
	@Override
	protected Collection<ExitPoint> exitPoints(final XExpression e) {
		if (e instanceof XJSemicolonStatement) {
			return this.getExitPoints(((XJSemicolonStatement) e).getExpression());
		}
		return super.exitPoints(e);
	}
}
