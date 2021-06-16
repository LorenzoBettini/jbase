package jbase.controlflow;

import java.util.Collection;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XSwitchExpression;

/**
 * Checks whether there is a sure return statement.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseSureReturnComputer extends JbaseSemicolonStatementAwareEarlyExitComputer {
	public boolean isSureReturn(final XExpression expression) {
		return this.isEarlyExit(expression);
	}

	/**
	 * For switch, a sure return must be in the default case.
	 */
	@Override
	protected Collection<ExitPoint> _exitPoints(final XSwitchExpression expression) {
		return this.getExitPoints(expression.getDefault());
	}
}
