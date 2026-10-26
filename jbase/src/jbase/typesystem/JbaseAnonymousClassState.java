package jbase.typesystem;

import java.util.List;

import org.eclipse.xtext.common.types.JvmConstructor;
import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.scoping.batch.IIdentifiableElementDescription;
import org.eclipse.xtext.xbase.typesystem.computation.IConstructorLinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.internal.AbstractTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.ConstructorLinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionAwareStackedResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.FeatureLinkHelper;
import org.eclipse.xtext.xbase.typesystem.internal.ScopeProviderAccess.ErrorDescription;
import org.eclipse.xtext.xbase.typesystem.internal.StackedResolvedTypes;

/**
 * Computes constructor calls used to create anonymous classes.
 *
 * <p>The synthetic anonymous type has local copies of the base type's parameters. Constructor
 * inference must instead use the constructor's declared parameters so diamond inference
 * resolves the base type arguments, not the synthetic type's copies.
 */
public class JbaseAnonymousClassState extends ExpressionTypeComputationState {
	public JbaseAnonymousClassState(final XConstructorCall call,
			final AbstractTypeComputationState parent) {
		super((StackedResolvedTypes) parent.getResolvedTypes(), parent.getFeatureScopeSession(),
			parent, call);
	}

	@Override
	protected IConstructorLinkingCandidate createCandidate(final XConstructorCall call,
			final IIdentifiableElementDescription description) {
		if (description instanceof ErrorDescription
				|| !(description.getElementOrProxy() instanceof JvmConstructor)) {
			return super.createCandidate(call, description);
		}
		final var types = new ExpressionAwareStackedResolvedTypes(getResolvedTypes(), call) { };
		final var state = createExpressionComputationState(call, types);
		return new ConstructorLinkingCandidate(call, description, getSingleExpectation(state), state) {
			@Override
			protected List<JvmTypeParameter> getDeclaredTypeParameters() {
				// The anonymous type's copied parameters are not the constructor's type variables.
				return new FeatureLinkHelper().getDeclaredTypeParameters(getConstructor());
			}
		};
	}
}
