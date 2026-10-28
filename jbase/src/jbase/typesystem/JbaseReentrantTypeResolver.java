package jbase.typesystem;

import java.util.Map;

import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.common.types.JvmAnnotationType;
import org.eclipse.xtext.common.types.JvmConstructor;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.common.types.util.TypeReferences;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociator;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.InferredTypeIndicator;
import org.eclipse.xtext.xbase.typesystem.internal.LogicalContainerAwareReentrantTypeResolver;
import org.eclipse.xtext.xbase.typesystem.internal.ResolvedTypes;
import org.eclipse.xtext.xtype.XComputedTypeReference;

import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJLocalClass;
import jbase.jvmmodel.JbaseAnonymousClassUtil;

/**
 * Prepares and resolves JVM types introduced by anonymous expressions and local declarations.
 *
 * <p>Xbase's resolver does not know that these expressions introduce local classes. This
 * resolver derives anonymous types from their constructor targets and prepares their
 * constructors. Named local types retain the host's supertypes. Both forms capture the
 * surrounding local scope before their member bodies are resolved.
 *
 * <p>Java permits capturing effectively final locals without an explicit final modifier.
 * Xbase's early writable-local diagnostic is deferred so that
 * {@link jbase.validation.JbaseValidator#checkCapturedLocal(XAbstractFeatureCall)} can inspect
 * all linked writes, including mutually exclusive first assignments to a blank local.
 * This also lets the validator distinguish a bound reference's immediate receiver read
 * from an actual lambda capture.
 */
public class JbaseReentrantTypeResolver extends LogicalContainerAwareReentrantTypeResolver {

	@Inject
	private JbaseAnonymousClassUtil anonymousClassUtil;
	@Inject
	private JvmTypesBuilder typesBuilder;
	@Inject
	private TypeReferences typeReferences;
	@Inject
	private IJvmModelAssociator associator;
	@Inject
	private IJvmModelAssociations associations;

	@Override
	protected void doPrepare(final ResolvedTypes resolvedTypes,
			final IFeatureScopeSession session, final JvmIdentifiableElement element,
			final Map<JvmIdentifiableElement, ResolvedTypes> contexts) {
		super.doPrepare(resolvedTypes, session, element, contexts);
		if (element instanceof JvmFeature feature) {
			prepareAnonymousTypes(resolvedTypes, session, feature, contexts);
		}
	}

	@Override
	protected void _computeTypes(final Map<JvmIdentifiableElement, ResolvedTypes> prepared,
			final ResolvedTypes resolved, final IFeatureScopeSession session,
			final JvmDeclaredType type) {
		if (associations.getPrimarySourceElement(type) instanceof XJAnonymousClass anonymous) {
			final var actual = resolved.getActualType(anonymous.getConstructorCall());
			final var memberTypes = prepared.get(type);
			// Members were prepared against the synthetic type; bind them to the instantiated
			// constructor target so inherited members see the actual base type arguments.
			memberTypes.reassignTypeWithoutMerge(type, actual);
			final var parameters = ((JvmGenericType) type).getTypeParameters();
			for (var i = 0; i < parameters.size(); i++) {
				memberTypes.reassignTypeWithoutMerge(parameters.get(i), actual.getTypeArguments().get(i));
			}
			for (final var superType : actual.getSuperTypes()) {
				memberTypes.reassignTypeWithoutMerge(superType.getType(), superType);
			}
		}
		super._computeTypes(prepared, resolved, session, type);
	}

	private void prepareAnonymousTypes(final ResolvedTypes resolvedTypes,
			final IFeatureScopeSession session, final JvmFeature feature,
			final Map<JvmIdentifiableElement, ResolvedTypes> contexts) {
		for (final var local : feature.getLocalClasses()) {
			if (associations.getPrimarySourceElement(local) instanceof XJLocalClass) {
				// Xbase stores the capture provider on the last supertype. Preserve the
				// host's actual reference rather than inferring it from a constructor call.
				final var index = local.getSuperTypes().size() - 1;
				final var reference = local.getSuperTypes().remove(index);
				requestCapturedLocalVariables(reference, local, resolvedTypes, contexts,
					capturing -> local.getSuperTypes().add(capturing));
			} else {
				prepareAnonymousType(resolvedTypes, session, local, contexts);
			}
		}
	}

	private void prepareAnonymousType(final ResolvedTypes resolvedTypes,
			final IFeatureScopeSession session, final JvmGenericType local,
			final Map<JvmIdentifiableElement, ResolvedTypes> contexts) {
		final var superType = local.getSuperTypes().getFirst();
		if (!InferredTypeIndicator.isInferred(superType)) {
			return;
		}
		final var computed = (XComputedTypeReference) superType;
		final var indicator = (InferredTypeIndicator) computed.getTypeProvider();
		final var anonymous = (XJAnonymousClass) indicator.getExpression();
		final var call = anonymous.getConstructorCall();
		final var scope = session.getScope(call,
			TypesPackage.Literals.JVM_PARAMETERIZED_TYPE_REFERENCE__TYPE, resolvedTypes);
		// Resolve the base type name from scope without recursively linking the constructor call.
		final var type = anonymousClassUtil.getSuperTypeNonResolving(anonymous, scope);
		final var copier = new EcoreUtil.Copier();
		if (type instanceof JvmGenericType generic) {
			// Give the synthetic type corresponding parameters while its base arguments are inferred.
			local.getTypeParameters().addAll(copier.copyAll(generic.getTypeParameters()));
			copier.copyReferences();
		}
		final var reference = createAnonymousTypeReference(call, local, type);
		requestCapturedLocalVariables(reference, local, resolvedTypes, contexts, capturing -> {
			// Captured variables affect the inferred type reference, so create constructors only
			// after the capture-aware reference has been finalized.
			computed.setEquivalent(capturing);
			prepareAnonymousConstructors(anonymous, local, type, reference, copier,
				resolvedTypes, session);
		});
	}

	private JvmTypeReference createAnonymousTypeReference(final XConstructorCall call,
			final JvmGenericType local, final JvmDeclaredType type) {
		if (type == null) {
			return TypesFactory.eINSTANCE.createJvmUnknownTypeReference();
		}
		final var parameterized = TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
		parameterized.setType(type);
		typesBuilder.associate(call, parameterized);
		for (final var parameter : local.getTypeParameters()) {
			parameterized.getArguments().add(typeReferences.createTypeRef(parameter));
		}
		return parameterized;
	}

	private void prepareAnonymousConstructors(final XJAnonymousClass anonymous,
			final JvmGenericType local, final JvmDeclaredType type,
			final JvmTypeReference reference, final EcoreUtil.Copier copier,
			final ResolvedTypes resolvedTypes, final IFeatureScopeSession session) {
		if (requiresDefaultConstructor(type)) {
			if (type != null) {
				// Interfaces and annotations have no constructor to copy; the local type still
				// needs Object as its JVM superclass.
				local.getSuperTypes().add(0,
					typeReferences.getTypeForName(Object.class, local));
			}
			addConstructor(anonymous, local, null, true, copier);
			return;
		}
		final var nested = addThisAndSuper(session, resolvedTypes.getReferenceOwner(),
			local, reference, false);
		for (final var member : type.getMembers()) {
			if (member instanceof JvmConstructor constructor) {
				addConstructor(anonymous, local, constructor, nested.isVisible(constructor), copier);
			}
		}
	}

	private boolean requiresDefaultConstructor(final JvmDeclaredType type) {
		return type == null || type instanceof JvmGenericType generic && generic.isInterface()
				|| type instanceof JvmAnnotationType;
	}

	private void addConstructor(final XJAnonymousClass anonymous, final JvmGenericType type,
			final JvmConstructor original, final boolean visible, final EcoreUtil.Copier copier) {
		final JvmConstructor constructor;
		if (original == null) {
			constructor = TypesFactory.eINSTANCE.createJvmConstructor();
		} else {
			// Resolve lazy parameter names before copying the executable signature.
			original.getParameters().forEach(JvmFormalParameter::getName);
			constructor = (JvmConstructor) copier.copy(original);
			copier.copyReferences();
		}
		type.getMembers().add(constructor);
		associator.associatePrimary(anonymous.getConstructorCall(), constructor);
		constructor.setSimpleName(type.getSimpleName());
		// Preserve whether the source could call the original constructor.
		constructor.setVisibility(visible ? JvmVisibility.DEFAULT : JvmVisibility.PRIVATE);
	}

	/**
	 * Returns no early capture error; this hook does not make mutable captures legal.
	 * For example, {@code int n = 1; IntSupplier f = () -> n; n++;} is still rejected
	 * by Jbase's later capture check. Explicit final-variable writes are checked separately.
	 */
	@Override
	protected String getInvalidWritableVariableAccessMessage(final XVariableDeclaration variable,
			final XAbstractFeatureCall featureCall, final IResolvedTypes resolvedTypes) {
		return null;
	}
}
