package jbase.validation;

import java.util.HashSet;
import java.util.Objects;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.override.OverrideHelper;
import org.eclipse.xtext.xbase.validation.IssueCodes;
import org.eclipse.xtext.xbase.validation.JvmGenericTypeValidator;

import com.google.inject.Inject;

import jbase.jbase.JbasePackage;
import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJAnonymousClassBody;
import jbase.jbase.XJLocalClass;
import jbase.jvmmodel.JbaseAnonymousClassUtil;
import jbase.typesystem.JbaseSealedTypeResolver;

/**
 * Applies JVM inheritance, member and lexical checks to anonymous and named local types.
 *
 * <p>These classes are inferred local JVM types, not top-level source types, so the
 * ordinary declared-type validation pass does not check them. This validator finds their
 * associations and runs the applicable JVM checks together with Java-specific restrictions.
 */
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
		// Visit from the resource root so each inferred local type is validated once.
		final var processed = new HashSet<EObject>();
		for (final var anonymous : EcoreUtil2.getAllContentsOfType(object, XJAnonymousClass.class)) {
			for (final var inferred : associations.getJvmElements(anonymous)) {
				if (inferred instanceof JvmGenericType type) {
					checkJvmGenericType(type, processed);
					checkJavaAnonymousClass(anonymous);
				}
			}
		}
		for (final var declaration : EcoreUtil2.getAllContentsOfType(object, XJLocalClass.class)) {
			for (final var inferred : associations.getJvmElements(declaration)) {
				if (inferred instanceof JvmGenericType type) {
					checkJvmGenericType(type, processed);
					checkJavaLocalClass(declaration, type);
				}
			}
		}
	}

	/** Checks the lexical restrictions that JVM member validation cannot see. */
	private void checkJavaLocalClass(final XJLocalClass source, final JvmGenericType type) {
		if (!(source.eContainer() instanceof XBlockExpression)) {
			error("A local class must be declared directly in a block", source, null,
				IssueCodes.INVALID_USE_OF_TYPE);
		}
		for (var child = (EObject) source; child != null
				&& !(child instanceof XJAnonymousClassBody); child = child.eContainer()) {
			if (child.eContainer() instanceof XBlockExpression block) {
				final var preceding = block.getExpressions().subList(0,
					block.getExpressions().indexOf(child));
				for (final var previous : preceding) {
					if (previous instanceof XJLocalClass declaration
							&& Objects.equals(declaration.getName(), source.getName())) {
						error("Duplicate local type " + source.getName(), source,
							JbasePackage.Literals.XJ_LOCAL_CLASS__NAME, IssueCodes.DUPLICATE_TYPE);
					}
				}
			}
		}
		for (var enclosing = type.eContainer(); enclosing != null; enclosing = enclosing.eContainer()) {
			if (enclosing instanceof JvmGenericType enclosingType
					&& Objects.equals(enclosingType.getSimpleName(), source.getName())) {
				error("A local class cannot have the same name as an enclosing type", source,
					JbasePackage.Literals.XJ_LOCAL_CLASS__NAME, IssueCodes.DUPLICATE_TYPE);
			}
		}
		final var owner = typeResolver.resolveTypes(source).getActualType(source).getOwner();
		for (final var superType : type.getSuperTypes()) {
			if (!sealedTypes.permittedTypes(source,
					owner.toLightweightTypeReference(superType)).isEmpty()) {
				error("A local class cannot extend or implement a sealed type", source, null,
					IssueCodes.INVALID_USE_OF_TYPE);
			}
		}
	}

	@Override
	protected void checkDuplicateAndOverriddenFunctions(final EObject source,
			final JvmGenericType type) {
		if (source instanceof XJLocalClass) {
			super.checkDuplicateAndOverriddenFunctions(source, type);
			return;
		}
		// The source is an anonymous-class expression; use its instantiated base type when
		// resolving inherited operations for duplicate and override checks.
		final var actual = typeResolver.resolveTypes(source)
			.getActualType((XJAnonymousClass) source);
		final var features = overrides.getResolvedFeatures(actual,
			getGeneratorConfig(source).getJavaSourceVersion());
		final var flagged = new HashSet<EObject>();
		checkDuplicateExecutables(type, features, flagged);
		checkOverriddenMethods(source, type, features, flagged);
		checkFunctionOverrides(features, flagged);
	}

	private void checkJavaAnonymousClass(final XJAnonymousClass source) {
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
			// Java's diamond form permits only members that override or implement a base method.
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
