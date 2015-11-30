package jbase.controlflow

import java.util.Collection
import org.eclipse.xtext.xbase.XSwitchExpression
import org.eclipse.xtext.xbase.controlflow.DefaultEarlyExitComputer
import org.eclipse.xtext.xbase.XExpression

/**
 * Checks whether there is a sure return statement.
 * 
 * @author Lorenzo Bettini
 */
class JbaseSureReturnComputer extends DefaultEarlyExitComputer {

	def boolean isSureReturn(XExpression expression) {
		return isEarlyExit(expression)
	}

	/**
	 * For switch, a sure return must be in the default case.
	 */
	override protected Collection<ExitPoint> _exitPoints(XSwitchExpression expression) {
		return getExitPoints(expression.getDefault())
	}
}