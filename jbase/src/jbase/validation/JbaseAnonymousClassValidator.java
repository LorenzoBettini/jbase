package jbase.validation;

import java.util.HashSet;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.override.OverrideHelper;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.eclipse.xtext.xbase.validation.JvmGenericTypeValidator;

import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;
import jbase.jvmmodel.JbaseAnonymousClassUtil;
import jbase.typesystem.JbaseSealedTypeResolver;

/** Applies the JVM inheritance and member checks only to Jbase anonymous types. */
public class JbaseAnonymousClassValidator extends JvmGenericTypeValidator {
	@Inject
	private IJvmModelAssociations associations;
	@Inject
	private OverrideHelper overrides;
	@Inject
	private JbaseSealedTypeResolver sealedTypes;
	@Inject
	private JbaseAnonymousClassUtil anonymousTypes;
	@Inject
	private IBatchTypeResolver typeResolver;

	@Override
	public void interceptRoot(final EObject object) {
		if (object.eResource().getContents().getFirst() != object) {
			return;
		}
		final var processed = new HashSet<EObject>();
		for (final var anonymous : EcoreUtil2.getAllContentsOfType(object, XJAnonymousClass.class)) {
			for (final var inferred : associations.getJvmElements(anonymous)) {
				if (inferred instanceof JvmGenericType type) {
					checkJvmGenericType(type, processed);
					checkJavaAnonymousClass(anonymous, type);
				}
			}
		}
	}
	@Override
	protected void checkDuplicateAndOverriddenFunctions(final EObject source,
			final JvmGenericType type) {
		final var actual = typeResolver.resolveTypes(source)
			.getActualType((XJAnonymousClass) source);
		final var features = overrides.getResolvedFeatures(actual,
			getGeneratorConfig(source).getJavaSourceVersion());
		final var flagged = new HashSet<EObject>();
		checkDuplicateExecutables(type, features, flagged);
		checkOverriddenMethods(source, type, features, flagged);
		checkFunctionOverrides(features, flagged);
	}

	private void checkJavaAnonymousClass(final XJAnonymousClass source, final JvmGenericType type) {
		final var call = source.getConstructorCall();
		final var superType = anonymousTypes.getSuperType(source);
		if (superType == null) return;
		final var resolved = typeResolver.resolveTypes(source).getActualType(source);
		final var reference = resolved.getOwner().toLightweightTypeReference(superType);
		if (!sealedTypes.permittedTypes(source, reference).isEmpty()) {
			error("An anonymous class cannot extend or implement a sealed type", source, null,
				IssueCodes.INVALID_USE_OF_TYPE);
		}
		if (call.isExplicitTypeArguments() && call.getTypeArguments().isEmpty()) {
			for (final var operation : overrides.getResolvedFeatures(resolved).getDeclaredOperations()) {
				if (operation.getDeclaration().getVisibility()
						!= JvmVisibility.PRIVATE
						&& operation.getOverriddenAndImplementedMethods().isEmpty()) {
					final var member = associations.getPrimarySourceElement(operation.getDeclaration());
					error("A method in an anonymous class using diamond must override a supertype method",
						member, getFeatureForIssue(member),
						IssueCodes.INVALID_USE_OF_TYPE);
				}
			}
		}
	}
}
