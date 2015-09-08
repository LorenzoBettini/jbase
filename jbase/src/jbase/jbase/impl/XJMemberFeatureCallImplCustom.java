package jbase.jbase.impl;

import org.eclipse.emf.common.util.EList;
import org.eclipse.xtext.xbase.XExpression;

import jbase.jbase.XJArrayAccessExpression;
import jbase.jbase.XJMemberFeatureCall;
import jbase.jbase.impl.XJArrayAccessExpressionImpl;
import jbase.jbase.impl.XJMemberFeatureCallImpl;

/**
 * A manual implementation that embeds the original member call target XExpression into
 * a {@link XJArrayAccessExpression}; "indexes" are also delegated to the embedded
 * {@link XJArrayAccessExpression} instance.
 * 
 * @author Lorenzo Bettini
 *
 */
public class XJMemberFeatureCallImplCustom extends XJMemberFeatureCallImpl implements XJMemberFeatureCall {

	/**
	 * Delegated to the embedded {@link XJArrayAccessExpression}; when indexes
	 * are requested or updated we can safely assume that the {@link XJArrayAccessExpression}
	 * has already been created (see the grammar rule).
	 */
	@Override
	public EList<XExpression> getIndexes()
	{
		return getArrayAccessExpression().getIndexes();
	}

	/**
	 * Custom implementation that intercepts the set of the {@link XExpression} from the parser,
	 * creates a new {@link XJArrayAccessExpression} and embeds the original {@link XExpression}.
	 * 
	 * @see org.eclipse.xtext.xbase.impl.XMemberFeatureCallImpl#setMemberCallTarget(org.eclipse.xtext.xbase.XExpression)
	 */
	@Override
	public void setMemberCallTarget(XExpression newMemberCallTarget) {
		XJArrayAccessExpression arrayAccessExpression = new XJArrayAccessExpressionImpl();
		
		setArrayAccessExpression(arrayAccessExpression);
		
		getArrayAccessExpression().setArray(newMemberCallTarget);
	}
	
	/**
	 * Customized in order to return the embedded {@link XJArrayAccessExpression}
	 * 
	 * @see org.eclipse.xtext.xbase.impl.XMemberFeatureCallImpl#getMemberCallTarget()
	 */
	@Override
	public XExpression getMemberCallTarget() {
		return getArrayAccessExpression();
	}
}
