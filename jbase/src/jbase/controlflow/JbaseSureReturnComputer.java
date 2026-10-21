package jbase.controlflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XSwitchExpression;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;

import com.google.inject.Inject;

import jbase.jbase.XJCasePart;
import jbase.util.JbaseSwitches;

/**
 * Checks whether there is a sure return statement.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseSureReturnComputer extends JbaseSemicolonStatementAwareEarlyExitComputer {
	@Inject
	private IBatchTypeResolver typeResolver;

	public boolean isSureReturn(final XExpression expression) {
		return this.isEarlyExit(expression);
	}

	/**
	 * Arrow rules must all exit; a total pattern or complete enum may replace default.
	 * Legacy colon statements retain their fall-through behavior.
	 */
	@Override
	protected Collection<ExitPoint> _exitPoints(final XSwitchExpression expression) {
		if (JbaseSwitches.isExpression(expression)) {
			return Collections.emptyList();
		}
		final var result = new ArrayList<ExitPoint>();
		for (final var part : expression.getCases()) {
			if (((XJCasePart) part).isArrow()
					&& getExitPoints(part.getThen()).isEmpty()) {
				return Collections.emptyList();
			}
			result.addAll(getExitPoints(part.getThen()));
		}
		if (expression.getDefault() == null && JbaseSwitches.isEnhanced(expression)
				&& JbaseSwitches.isExhaustive(expression, typeResolver.resolveTypes(expression))) {
			return result;
		}
		return this.getExitPoints(expression.getDefault());
	}
}
