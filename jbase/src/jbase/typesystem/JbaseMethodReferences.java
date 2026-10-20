package jbase.typesystem;

import static jbase.validation.JbaseIssueCodes.INVALID_METHOD_REFERENCE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmExecutable;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.xbase.typesystem.util.IVisibilityHelper;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.validation.EObjectDiagnosticImpl;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.jvmmodel.ILogicalContainerProvider;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.computation.ClosureWithExpectationHelper;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeExpectation;
import org.eclipse.xtext.xbase.typesystem.conformance.ConformanceFlags;
import org.eclipse.xtext.xbase.typesystem.conformance.TypeConformanceComputationArgument;
import org.eclipse.xtext.xbase.typesystem.internal.AbstractTypeComputationState;
import org.eclipse.xtext.xbase.typesystem.override.IResolvedExecutable;
import org.eclipse.xtext.xbase.typesystem.override.IResolvedOperation;
import org.eclipse.xtext.xbase.typesystem.override.OverrideHelper;
import org.eclipse.xtext.xbase.typesystem.references.LightweightMergedBoundTypeArgument;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.UnboundTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.WildcardTypeReference;
import org.eclipse.xtext.xbase.typesystem.util.BoundTypeArgumentSource;
import org.eclipse.xtext.xbase.typesystem.util.ContextualVisibilityHelper;
import org.eclipse.xtext.xbase.typesystem.util.DeferredTypeParameterHintCollector;
import org.eclipse.xtext.xbase.typesystem.util.PublicVisibilityHelper;
import org.eclipse.xtext.xbase.typesystem.util.TypeParameterByUnboundSubstitutor;
import org.eclipse.xtext.xbase.typesystem.util.VarianceInfo;

import com.google.inject.Inject;

import jbase.jbase.JbasePackage;
import jbase.jbase.XJMethodReference;

/**
 * Target typing for Java references. ClosureWithExpectationHelper supplies Xbase's
 * deferred functional interface constraints; resolved features supply inherited
 * signatures with the receiver's type arguments substituted.
 */
public class JbaseMethodReferences {
	private static final TypeConformanceComputationArgument STRICT =
		new TypeConformanceComputationArgument(false, false, false, true, false, false);
	private static final TypeConformanceComputationArgument LOOSE =
		new TypeConformanceComputationArgument(false, false, true, true, false, false);



	@Inject
	private OverrideHelper overrideHelper;

	@Inject
	private ILogicalContainerProvider logicalContainers;

	public void computeTypes(final XJMethodReference reference,
			final ITypeComputationState state) {
		for (final var expectation : state.getExpectations()) {
			final var target = expectation.getExpectedType();
			final var operation = target == null ? null
				: state.getReferenceOwner().getServices().getFunctionTypes()
					.findImplementingOperation(target);
			if (operation == null || !target.isInterfaceType()) {
				receiverType(reference, state);
				error(reference, state, "A method reference requires a functional interface target type");
				state.acceptActualType(state.getReferenceOwner().newAnyTypeReference());
			} else {
				final var helper = new ReferenceTypeHelper(reference, operation, expectation, state);
				helper.computeTypes();
				state.acceptCandidate(reference, helper);
			}
		}
	}

	private LightweightTypeReference receiverType(final XJMethodReference reference,
			final ITypeComputationState state) {
		if (reference.getType() != null) {
			return state.getReferenceOwner().toLightweightTypeReference(reference.getType());
		}
		return state.withoutExpectation().computeTypes(reference.getReceiver())
			.getActualExpressionType();
	}

	public IVisibilityHelper visibilityHelper(final XJMethodReference reference) {
		final var container = logicalContainers.getLogicalContainer(reference);
		final var context = EcoreUtil2.getContainerOfType(container, JvmDeclaredType.class);
		return new ContextualVisibilityHelper(new PublicVisibilityHelper(), context);
	}

	private boolean isTypeReceiver(final XJMethodReference reference,
			final ITypeComputationState state) {
		return reference.getType() != null
			|| reference.getReceiver() instanceof XAbstractFeatureCall call
				&& ((AbstractTypeComputationState) state).getResolvedTypes()
					.getLinkingCandidate(call).isTypeLiteral();
	}

	private void error(final XJMethodReference reference, final ITypeComputationState state,
			final String message) {
		state.addDiagnostic(new EObjectDiagnosticImpl(Severity.ERROR, INVALID_METHOD_REFERENCE,
			message, reference, JbasePackage.Literals.XJ_METHOD_REFERENCE__MEMBER, -1, null));
	}

	private boolean isReifiable(final LightweightTypeReference type) {
		if (type.isArray()) {
			return isReifiable(type.getComponentType());
		}
		return type.getTypeArguments().stream().allMatch(argument -> argument instanceof WildcardTypeReference wildcard
			&& wildcard.getLowerBound() == null
			&& wildcard.getUpperBoundSubstitute().isType(Object.class));
	}

	private boolean invocationCompatible(final LightweightTypeReference expected,
			final LightweightTypeReference actual, final boolean boxing) {
		if (boxing && actual.isPrimitive() && !expected.isPrimitive()) {
			// Java permits boxing followed by reference widening, but not
			// primitive widening followed by boxing (int cannot become Long).
			return expected.isAssignableFrom(actual.getWrapperTypeIfPrimitive(), STRICT);
		}
		return expected.isAssignableFrom(actual, boxing ? LOOSE : STRICT);
	}

	private record Match(JvmExecutable executable, LightweightTypeReference result,
			List<LightweightTypeReference> parameters,
			List<LightweightTypeReference> exceptions, int phase) { }

	private class ReferenceTypeHelper extends ClosureWithExpectationHelper {
		private final XJMethodReference reference;
		private JvmExecutable executable;
		private boolean valid = true;

		ReferenceTypeHelper(final XJMethodReference reference, final JvmOperation operation,
				final ITypeExpectation expectation, final ITypeComputationState state) {
			super(reference, operation, expectation, state);
			this.reference = reference;
		}

		@Override
		protected void computeTypes() {
			final var state = getState();
			var receiver = receiverType(reference, state);
			final var typeReceiver = isTypeReceiver(reference, state);
			if (typeReceiver && reference.getType() == null) {
				receiver = receiver.getTypeArguments().getFirst();
			}
			if ("new".equals(reference.getMember()) && receiver != null && receiver.isRawType()) {
				final var type = (JvmGenericType) receiver.getType();
				final var inferred = state.getReferenceOwner().newParameterizedTypeReference(type);
				for (final var parameter : type.getTypeParameters()) {
					inferred.addTypeArgument(state.createUnboundTypeReference(reference, parameter));
				}
				receiver = inferred;
			}
			final var function = getExpectedClosureType();
			final var arguments = function.getParameterTypes();
			final var matches = new ArrayList<Match>();
			if (receiver != null && receiver.isArray() && "new".equals(reference.getMember())) {
				if (typeReceiver && isReifiable(receiver) && reference.getTypeArguments().isEmpty()
						&& arguments.size() == 1
						&& state.getReferenceOwner().newReferenceTo(int.class)
							.isAssignableFrom(arguments.getFirst())) {
					matches.add(new Match(null, receiver, List.copyOf(arguments), List.of(), 0));
				}
			} else if (receiver != null && receiver.isArray()
					&& "clone".equals(reference.getMember())
					&& ((!typeReceiver && arguments.isEmpty())
						|| (typeReceiver && arguments.size() == 1
							&& receiver.isAssignableFrom(arguments.getFirst(), STRICT)))) {
				matches.add(new Match(null, receiver, List.copyOf(arguments), List.of(), 0));
			} else if (receiver != null && !receiver.isPrimitive()) {
				collectMatches(receiver, typeReceiver, arguments, matches);
			}
			final var best = matches.stream().filter(candidate -> matches.stream()
				.noneMatch(other -> other != candidate && moreSpecific(other, candidate))).toList();
			if (best.size() == 1) {
				final var match = best.getFirst();
				executable = match.executable();
				final var expectedReturn = function.getReturnType();
				if (!expectedReturn.isPrimitiveVoid()) {
					if (!match.result().isPrimitive()) {
						if (expectedReturn instanceof UnboundTypeReference unbound) {
							unbound.tryResolve(false);
						}
						final var projected = match.result().getSuperType(expectedReturn.getType());
						new DeferredTypeParameterHintCollector(state.getReferenceOwner())
							.processPairedReferences(projected == null ? match.result() : projected,
								expectedReturn.getWrapperTypeIfPrimitive());
					}
					deferredBindTypeArgument(expectedReturn, match.result(),
						BoundTypeArgumentSource.INFERRED);
					final var targetReturn = getExpectation().getExpectedType()
						.tryConvertToFunctionTypeReference(false).getReturnType();
					deferredBindTypeArgument(targetReturn, match.result(),
						BoundTypeArgumentSource.INFERRED);
					if (targetReturn instanceof UnboundTypeReference unbound) {
						unbound.tryResolve(false);
					}
					if (!invocationCompatible(targetReturn, match.result(), true)) {
						error(reference, state, "Incompatible method reference return type");
					}
				}
				checkExceptions(match);
			} else {
				error(reference, state, best.isEmpty() ? "No applicable method reference"
					: "Ambiguous method reference");
			}
			getExpectation().acceptActualType(function,
				(valid ? ConformanceFlags.UNCHECKED
					: ConformanceFlags.CHECKED | ConformanceFlags.INCOMPATIBLE | ConformanceFlags.SEALED)
					| ConformanceFlags.LAMBDA_WITH_EXPECTATION);
		}

		private void error(final XJMethodReference expression,
				final ITypeComputationState state, final String message) {
			valid = false;
			JbaseMethodReferences.this.error(expression, state, message);
		}

		private void collectMatches(final LightweightTypeReference receiver,
				final boolean typeReceiver, final List<LightweightTypeReference> arguments,
				final List<Match> matches) {
			final var features = overrideHelper.getResolvedFeatures(receiver.isArray()
				? getState().getReferenceOwner().newReferenceToObject() : receiver);
			if ("new".equals(reference.getMember())) {
				if (typeReceiver && receiver.getType() instanceof JvmGenericType type
						&& !type.isAbstract()
						&& receiver.getTypeArguments().stream().noneMatch(LightweightTypeReference::isWildcard)) {
					for (final var constructor : features.getDeclaredConstructors()) {
						addMatch(constructor, receiver, arguments, matches);
					}
				}
				return;
			}
			for (final var method : features.getAllOperations().stream()
				.filter(method -> method.getDeclaration().getSimpleName()
					.equals(reference.getMember())).toList()) {
				if (method.getDeclaration().isStatic()) {
					if (typeReceiver && reference.getType() == null) {
						addMatch(method, referenceReturnType(method, receiver), arguments, matches);
					}
				} else if (!typeReceiver) {
					addMatch(method, referenceReturnType(method, receiver), arguments, matches);
				} else if (!arguments.isEmpty()) {
					// Constrain the original invocation variable before resolving the
					// closure's corresponding parameter (e.g. comparing(String::length)).
					final var targetFunction = getExpectation().getExpectedType()
						.tryConvertToFunctionTypeReference(false);
					deferredBindTypeArgument(targetFunction.getParameterTypes().getFirst()
						.getLowerBoundSubstitute(), receiver, BoundTypeArgumentSource.INFERRED);
					if (arguments.getFirst() instanceof UnboundTypeReference unbound) {
						unbound.tryResolve(false);
					}
					if (!receiver.isAssignableFrom(arguments.getFirst(), STRICT)) {
						continue;
					}
					// A raw type name can acquire type arguments from the receiver parameter.
					final var actualReceiver = arguments.getFirst().getSuperType(receiver.getType());
					final var resolved = overrideHelper
						.getResolvedFeatures(actualReceiver)
						.getResolvedOperation(method.getDeclaration());
					addMatch(resolved, referenceReturnType(resolved, receiver),
						arguments.subList(1, arguments.size()), matches);
				}
			}
		}

		private LightweightTypeReference referenceReturnType(final IResolvedOperation method,
				final LightweightTypeReference receiver) {
			if ("getClass".equals(method.getDeclaration().getSimpleName())
					&& method.getDeclaration().getParameters().isEmpty()) {
				// Like an invocation, a reference to getClass has the specialized
				// return type Class<? extends |T|>, where T is the qualifying type.
				final var owner = getState().getReferenceOwner();
				final var result = owner.newParameterizedTypeReference(
					method.getResolvedReturnType().getType());
				final var argument = owner.newWildcardTypeReference();
				argument.addUpperBound(receiver.getRawTypeReference());
				result.addTypeArgument(argument);
				return result;
			}
			return method.getResolvedReturnType();
		}

		private void addMatch(final IResolvedExecutable resolved,
				final LightweightTypeReference result,
				final List<LightweightTypeReference> arguments, final List<Match> matches) {
			final var declaration = resolved.getDeclaration();
			if (!visibilityHelper(reference).isVisible(declaration)) {
				return;
			}
			if (!reference.getTypeArguments().isEmpty()
					&& !declaration.getTypeParameters().isEmpty()
					&& reference.getTypeArguments().size() != declaration.getTypeParameters().size()) {
				return;
			}
			final var owner = getState().getReferenceOwner();
			final var mapping = new HashMap<JvmTypeParameter, LightweightMergedBoundTypeArgument>();
			for (var i = 0; i < Math.min(reference.getTypeArguments().size(),
				declaration.getTypeParameters().size()); i++) {
				mapping.put(declaration.getTypeParameters().get(i), new LightweightMergedBoundTypeArgument(
					owner.toLightweightTypeReference(reference.getTypeArguments().get(i)),
					VarianceInfo.INVARIANT));
			}
			final var unbound = new HashMap<JvmTypeParameter, UnboundTypeReference>();
			final var substitutor = new TypeParameterByUnboundSubstitutor(mapping, owner) {
				@Override
				protected UnboundTypeReference createUnboundTypeReference(final JvmTypeParameter type) {
					return unbound.computeIfAbsent(type,
						parameter -> getState().createUnboundTypeReference(reference, parameter));
				}
			};
			for (final var parameter : declaration.getTypeParameters()) {
				if (mapping.containsKey(parameter) && parameter.getConstraints().stream()
						.anyMatch(constraint -> !substitutor.substitute(constraint.getTypeReference())
							.isAssignableFrom(mapping.get(parameter).getTypeReference()))) {
					return;
				}
			}
			final var parameters = resolved.getResolvedParameterTypes().stream()
				.map(substitutor::substitute).toList();
			final var returnType = substitutor.substitute(result);
			for (var phase = 0; phase < 3; phase++) {
				final var expanded = phase == 2 && declaration.isVarArgs();
				if ((!expanded && arguments.size() != parameters.size())
						|| (expanded && arguments.size() < parameters.size() - 1)) {
					continue;
				}
				final var formal = new ArrayList<LightweightTypeReference>();
				var applicable = true;
				for (var i = 0; i < arguments.size(); i++) {
					var parameter = parameters.get(Math.min(i, parameters.size() - 1));
					if (expanded && i >= parameters.size() - 1) {
						parameter = parameter.getComponentType();
					}
					formal.add(parameter);
					final var argument = arguments.get(i);
					if (argument instanceof UnboundTypeReference inferredArgument) {
						inferredArgument.tryResolve(false);
					}
					new DeferredTypeParameterHintCollector(owner)
						.processPairedReferences(parameter, argument.getWrapperTypeIfPrimitive());
					if (!invocationCompatible(parameter, argument, phase != 0)) {
						applicable = false;
						break;
					}
				}
				if (applicable) {
					// An empty varargs tail still participates in most-specific selection.
					if (expanded && arguments.size() == parameters.size() - 1) {
						formal.add(parameters.getLast().getComponentType());
					}
					matches.add(new Match(declaration, returnType, formal,
						resolved.getResolvedExceptions().stream().map(substitutor::substitute).toList(),
						phase));
					return;
				}
			}
		}

		private boolean moreSpecific(final Match first, final Match second) {
			// Java performs separate searches for static and unbound instance methods.
			// A result from each search is ambiguous even if their invocation phases differ.
			if (first.executable() instanceof JvmOperation a
					&& a.isStatic() != ((JvmOperation) second.executable()).isStatic()) {
				return false;
			}
			if (first.phase() != second.phase()) {
				return first.phase() < second.phase();
			}
			var strict = false;
			for (var i = 0; i < Math.max(first.parameters().size(), second.parameters().size()); i++) {
				final var a = first.parameters().get(Math.min(i, first.parameters().size() - 1));
				final var b = second.parameters().get(Math.min(i, second.parameters().size() - 1));
				if (!b.isAssignableFrom(a, STRICT)) {
					return false;
				}
				strict |= !a.isAssignableFrom(b, STRICT);
			}
			return strict || first.executable().getTypeParameters().isEmpty()
				&& !second.executable().getTypeParameters().isEmpty();
		}

		private void checkExceptions(final Match match) {
			final var target = overrideHelper.getResolvedFeatures(getExpectation().getExpectedType())
				.getResolvedOperation(getOperation()).getResolvedExceptions();
			for (final var exception : match.exceptions()) {
				if (!exception.isSubtypeOf(RuntimeException.class) && !exception.isSubtypeOf(Error.class)
						&& target.stream().noneMatch(allowed -> allowed.isAssignableFrom(exception))) {
					error(reference, getState(), "Unhandled exception in method reference: "
						+ exception.getHumanReadableName());
				}
			}
		}

		@Override
		public void applyToModel(final IResolvedTypes resolvedTypes) {
			reference.setExecutable(executable);
		}
	}
}
