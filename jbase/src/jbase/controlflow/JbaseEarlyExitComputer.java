package jbase.controlflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XSwitchExpression;

import com.google.inject.Inject;

/**
 * Customization to take into account that in Java, switch's cases automatically fall through
 * without an explicit break; it also takes into consideration break statements in loops.
 *
 * @author Lorenzo Bettini
 */
public class JbaseEarlyExitComputer extends JbaseSemicolonStatementAwareEarlyExitComputer {

	@Inject
	private JbaseBranchingStatementDetector branchingStatementDetector;
	@Inject
	private JbaseBreakStatementDetector breakStatementDetector;

	@Override
	public Collection<ExitPoint> getExitPoints(final XExpression expression) {
		var exitPoints = super.getExitPoints(expression);
		var head = exitPoints.isEmpty() ? null : exitPoints.iterator().next();
		if (head != null && head.getExpression() == expression
				&& breakStatementDetector.containsPossibleBreakStatement(expression)) {
			return Collections.emptyList();
		}
		return exitPoints;
	}

	@Override
	protected Collection<ExitPoint> _exitPoints(final XSwitchExpression expression) {
		var result = new ArrayList<ExitPoint>();
		for (var casePart : expression.getCases()) {
			var then = casePart.getThen();
			var caseExit = getExitPoints(then);
			// if there is not a break then in Java it is an automatic fall through
			// so we must not consider this case
			if (branchingStatementDetector.isSureBranchStatement(then) && !isNotEmpty(caseExit)) {
				return Collections.emptyList();
			}
			result.addAll(caseExit);
		}
		var defaultExit = getExitPoints(expression.getDefault());
		if (!isNotEmpty(defaultExit)) {
			return Collections.emptyList();
		}
		result.addAll(defaultExit);
		return result;
	}
}
