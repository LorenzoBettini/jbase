package jbase.controlflow;

import java.util.Collection;

import org.eclipse.xtext.xbase.controlflow.DefaultEarlyExitComputer;

import jbase.jbase.XJSemicolonStatement;

/**
 * Handles semicolon statement containers.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseSemicolonStatementAwareEarlyExitComputer extends DefaultEarlyExitComputer {

	/**
	 * A semicolon statement wraps an expression, so we delegate to the contained expression.
	 */
	def dispatch protected Collection<ExitPoint> exitPoints(XJSemicolonStatement st) {
		return getExitPoints(st.expression)
	}
}
