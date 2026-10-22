package jbase.typesystem;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.util.Primitives.Primitive;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBinaryOperation;
import org.eclipse.xtext.xbase.scoping.batch.IIdentifiableElementDescription;
import org.eclipse.xtext.xbase.typesystem.computation.IFeatureLinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.internal.AbstractTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionAwareStackedResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.internal.FeatureLinkingCandidate;
import org.eclipse.xtext.xbase.typesystem.internal.ScopeProviderAccess;
import org.eclipse.xtext.xbase.typesystem.internal.StackedResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

/**
 * Resolves array compound operators with the element's assignment type. Java narrows
 * primitive numeric results, boxes matching primitive results, and permits string
 * concatenation into reference elements that can hold a String.
 */
public class JbaseArrayCompoundAssignmentState extends ExpressionTypeComputationState {

	public JbaseArrayCompoundAssignmentState(final XBinaryOperation operation,
			final AbstractTypeComputationState parent) {
		super((StackedResolvedTypes) parent.getResolvedTypes(), parent.getFeatureScopeSession(),
			parent, operation);
	}

	@Override
	protected IFeatureLinkingCandidate createCandidate(final XAbstractFeatureCall call,
			final StackedResolvedTypes demandComputedTypes,
			final IIdentifiableElementDescription description) {
		if (description instanceof ScopeProviderAccess.ErrorDescription) {
			return super.createCandidate(call, demandComputedTypes, description);
		}
		// The parent already contains the element and index types. Keep each candidate's
		// promoted operand expectations in its own fork rather than refining the lvalue.
		final var types = new ExpressionAwareStackedResolvedTypes(getResolvedTypes(), call) { };
		final var state = createExpressionComputationState(call, types);
		return new FeatureLinkingCandidate(call, description, getSingleExpectation(state), state) {
			@Override
			protected LightweightTypeReference getDeclaredType(final JvmIdentifiableElement feature) {
				final var promoted = super.getDeclaredType(feature);
				final var element = getActualType(((XBinaryOperation) call).getLeftOperand());
				if (isNumeric(promoted) && isNumeric(element)
						|| promoted.getIdentifier().equals(
							element.getPrimitiveIfWrapperType().getIdentifier())
						|| promoted.isType(String.class) && element.isAssignableFrom(promoted)) {
					return element;
				}
				return promoted;
			}
		};
	}

	private boolean isNumeric(final LightweightTypeReference type) {
		final var kind = type.getPrimitiveKind();
		// Array elements and operator results cannot have the primitive void type.
		return kind != null && kind != Primitive.Boolean;
	}
}
