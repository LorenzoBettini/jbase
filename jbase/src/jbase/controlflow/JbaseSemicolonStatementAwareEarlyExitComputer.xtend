package jbase.controlflow;

import java.util.Collection;

import org.eclipse.xtext.xbase.controlflow.DefaultEarlyExitComputer;

import jbase.jbase.XJSemicolonStatement;
import org.eclipse.xtext.xbase.XExpression

/**
 * Handles semicolon statement containers.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseSemicolonStatementAwareEarlyExitComputer extends DefaultEarlyExitComputer {

	/**
	 * A semicolon statement wraps an expression, so we delegate to the contained expression.
	 */
	override protected Collection<ExitPoint> exitPoints(XExpression e) {
		if (e instanceof XJSemicolonStatement) {
			return getExitPoints(e.expression)
		}
		return super.exitPoints(e)
	}
}
