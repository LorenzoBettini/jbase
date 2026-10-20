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
 * Computes Java method and constructor references within Xbase's closure inference protocol.
 * {@link XJMethodReference} extends {@code XClosure} so references remain deferred arguments
 * of generic and overloaded calls. {@link ClosureWithExpectationHelper} prepares the functional
 * interface constraints; this class matches members instead of computing a synthetic lambda body.
 *
 * <p>Resolved features substitute the receiver's type arguments into inherited signatures.
 * The selected executable is applied to the model only after candidate computation, while the
 * compiler emits native reference syntax to preserve receiver evaluation and {@code super}.
 *
 * @see JbaseTypeComputer
 * @see jbase.compiler.JbaseXbaseCompiler
 */
public class JbaseMethodReferences {
	// Both modes allow primitive widening and disable Xbase synonym conversions.
	// Only LOOSE allows boxing/unboxing; the invocation phases choose between them.
	private static final TypeConformanceComputationArgument STRICT =
		new TypeConformanceComputationArgument(false, false, false, true, false, false);
	private static final TypeConformanceComputationArgument LOOSE =
		new TypeConformanceComputationArgument(false, false, true, true, false, false);



	@Inject
	private OverrideHelper overrideHelper;

	@Inject
	private ILogicalContainerProvider logicalContainers;

	/**
	 * Computes a separate closure candidate for each functional interface expectation.
	 * For example, {@code Stream.of("a").map(String::length)} must contribute constraints
	 * to the enclosing generic call rather than receive a standalone type during parsing.
	 * Even a missing target still triggers receiver computation for linking diagnostics.
	 */
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

	/**
	 * Computes the qualifier without imposing the reference's functional interface type on it.
	 * An expression denoting a type still has Xbase's {@code Class<T>} type here; the caller
	 * unwraps {@code T} after distinguishing that expression from an instance receiver.
	 */
	private LightweightTypeReference receiverType(final XJMethodReference reference,
			final ITypeComputationState state) {
		if (reference.getType() != null) {
			return state.getReferenceOwner().toLightweightTypeReference(reference.getType());
		}
		return state.withoutExpectation().computeTypes(reference.getReceiver())
			.getActualExpressionType();
	}

	/**
	 * Shares contextual member visibility between inference and Eclipse completion.
	 * The context is the generated JVM declaring type, obtained through the logical container;
	 * public visibility alone would incorrectly hide accessible private or protected methods.
	 */
	public IVisibilityHelper visibilityHelper(final XJMethodReference reference) {
		final var container = logicalContainers.getLogicalContainer(reference);
		final var context = EcoreUtil2.getContainerOfType(container, JvmDeclaredType.class);
		return new ContextualVisibilityHelper(new PublicVisibilityHelper(), context);
	}

	/**
	 * Uses the linking candidate to distinguish {@code String::length} from {@code text::trim}.
	 * Testing only whether the linked feature is a {@code JvmType} would also classify
	 * {@code this} and {@code super} as type literals and lose their bound-receiver semantics.
	 */
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

	/**
	 * Checks array constructor element types recursively: {@code List<?>[]::new} is valid,
	 * but {@code List<String>[]::new} and {@code List<? super String>[]::new} are not.
	 * Raw and non-parameterized element types have no arguments to reject.
	 */
	private boolean isReifiable(final LightweightTypeReference type) {
		if (type.isArray()) {
			return isReifiable(type.getComponentType());
		}
		return type.getTypeArguments().stream().allMatch(argument -> argument instanceof WildcardTypeReference wildcard
			&& wildcard.getLowerBound() == null
			&& wildcard.getUpperBoundSubstitute().isType(Object.class));
	}

	/**
	 * Tests Java invocation conversion for parameters and reference results.
	 * In particular, {@code int -> Integer -> Number} is allowed in a boxing phase,
	 * whereas {@code int -> long -> Long} is not. Unboxing followed by primitive widening
	 * is handled by the loose conformance mode. Receiver checks use STRICT directly.
	 */
	private boolean invocationCompatible(final LightweightTypeReference expected,
			final LightweightTypeReference actual, final boolean boxing) {
		if (boxing && actual.isPrimitive() && !expected.isPrimitive()) {
			// Java permits boxing followed by reference widening, but not
			// primitive widening followed by boxing (int cannot become Long).
			return expected.isAssignableFrom(actual.getWrapperTypeIfPrimitive(), STRICT);
		}
		return expected.isAssignableFrom(actual, boxing ? LOOSE : STRICT);
	}

	/**
	 * An applicable invocation with substituted types and effective comparison parameters.
	 * Phases 0, 1 and 2 mean strict fixed arity, loose fixed arity and expanded varargs.
	 * Array creation and array clone have synthetic signatures, so their executable is null.
	 */
	private record Match(JvmExecutable executable, LightweightTypeReference result,
			List<LightweightTypeReference> parameters,
			List<LightweightTypeReference> exceptions, int phase) { }

	/**
	 * Reuses Xbase's functional descriptor and deferred bindings for one target expectation.
	 * It replaces the inherited lambda-body computation and model update: a reference has
	 * neither a synthesized body nor implicit lambda parameters to install.
	 */
	private class ReferenceTypeHelper extends ClosureWithExpectationHelper {
		private final XJMethodReference reference;
		private JvmExecutable executable;
		private boolean valid = true;

		ReferenceTypeHelper(final XJMethodReference reference, final JvmOperation operation,
				final ITypeExpectation expectation, final ITypeComputationState state) {
			super(reference, operation, expectation, state);
			this.reference = reference;
		}

		/**
		 * Adapts the qualifier to the descriptor, selects an invocation, then contributes
		 * return constraints and checks exceptions. Raw constructor qualifiers receive fresh
		 * class arguments, allowing {@code Supplier<List<String>> f = ArrayList::new} to infer
		 * the element type. Invalid candidates must also report incompatibility to Xbase's
		 * enclosing overload selection, rather than merely attaching an error message.
		 */
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
					// Reverse result constraints infer, for example, emptyList's T from List<String>.
					// Resolve only available hints: forcing an unconstrained invocation variable
					// to its bound here would damage inference for comparing(String::length).
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
					// Check the original descriptor as well as the helper's synthetic return variable.
					// Otherwise competing hints can conceal int-to-Long in Function<String, Long>.
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

		/** Marks this expectation incompatible as well as locating the diagnostic on the member. */
		private void error(final XJMethodReference expression,
				final ITypeComputationState state, final String message) {
			valid = false;
			JbaseMethodReferences.this.error(expression, state, message);
		}

		/**
		 * Builds invocation shapes from inherited, generically resolved features.
		 * Bound references consume no descriptor parameter for their receiver; unbound ones
		 * consume the first. Static references require an unparameterized type qualifier,
		 * and class constructors require a type qualifier without wildcard arguments.
		 * Arrays use Object for ordinary member lookup; creation and clone are handled earlier.
		 */
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

		/**
		 * Preserves Java's specialized getClass result instead of reflection's {@code Class<?>}.
		 * {@code String::getClass} returns {@code Class<? extends String>}; changing the
		 * functional receiver parameter to String does not give {@code Object::getClass}
		 * that same result type. The qualifying type, erased to its raw form, is decisive.
		 */
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

		/**
		 * Checks visibility and explicit type bounds, substitutes executable variables, and
		 * tries Java's three invocation phases. Explicit arguments constrain a generic
		 * declaration, but may be ignored by a non-generic one ({@code String::<Integer>length}).
		 * Unspecified variables retain deferred hints so argument and target result constraints
		 * can cooperate; they must not all be eagerly replaced by their upper bounds.
		 */
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

		/**
		 * Orders applicable invocations within the static or instance search, first by phase
		 * and then by effective parameter specificity. Repeating the final comparison type
		 * accounts for varargs lists with different declared arities. A non-generic declaration
		 * wins an otherwise equal parameter comparison against a generic declaration.
		 */
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

		/**
		 * Checks against the functional operation's throws clause, not the enclosing method's.
		 * Here the inherited getOperation() denotes the functional descriptor; the referenced
		 * executable is in Match. RuntimeException and Error subtypes need no declared allowance.
		 */
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

		/**
		 * Commits only the executable link after inference. The inherited implementation would
		 * install implicit lambda parameters, which have no source or execution role in a reference.
		 * Synthetic array signatures intentionally leave this transient link null.
		 */
		@Override
		public void applyToModel(final IResolvedTypes resolvedTypes) {
			reference.setExecutable(executable);
		}
	}
}
