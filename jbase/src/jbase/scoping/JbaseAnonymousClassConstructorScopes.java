/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jbase.scoping;

import java.util.ArrayList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.common.types.JvmConstructor;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XbasePackage;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.scoping.batch.ConstructorScopes;
import org.eclipse.xtext.xbase.scoping.batch.ConstructorDescription;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJConstructorCall;

/**
 * Supplies constructor scopes for anonymous classes and qualified inner-class creation.
 *
 * <p>The constructor call in an anonymous expression also determines the synthetic type's
 * base class. Its body needs the anonymous-class-specific {@code this} and {@code super}
 * scope, rather than the ordinary constructor-call scope.
 * 
 * Inspired by org.eclipse.xtend.core.scoping.AnonymousClassConstructorScopes
 */
public class JbaseAnonymousClassConstructorScopes extends ConstructorScopes {

	@Inject
	private IJvmModelAssociations associations;

	@Override
	public IScope createConstructorScope(final EObject context, final EReference reference,
			final IFeatureScopeSession session, final IResolvedTypes resolvedTypes) {
		if (context instanceof XJConstructorCall qualified
				&& qualified.getQualifier() != null) {
			final var scope = createQualifiedConstructorScope(qualified, session, resolvedTypes);
			if (scope != null) {
				return scope;
			}
		}
		if (context instanceof XConstructorCall) {
			final var container = context.eContainer();
			if (container instanceof XJAnonymousClass) {
				// The inferred JVM type supplies this/super for the anonymous class body.
				final var inferredType = Iterables.getFirst(Iterables.filter( // NOSONAR wrong interpretation of the nullability
					associations.getJvmElements(container), JvmGenericType.class), null);
				if (inferredType != null) { // NOSONAR wrong interpretation of the nullability
					return createAnonymousClassConstructorScope(inferredType, context, session);
				}
			}
		}
		return super.createConstructorScope(context, reference, session, resolvedTypes);
	}

	/**
	 * Resolves the unqualified member-class name against the qualifier's declared type.
	 */
	private IScope createQualifiedConstructorScope(final XJConstructorCall call,
			final IFeatureScopeSession session, final IResolvedTypes resolvedTypes) {
		// JbaseTypeComputer computes the qualifier before constructor scope lookup.
		final var type = resolvedTypes.getActualType(call.getQualifier()).getType();
		if (!(type instanceof JvmDeclaredType declaredType)) {
			return null;
		}
		final var nodes = NodeModelUtils.findNodesForFeature(call,
				XbasePackage.Literals.XCONSTRUCTOR_CALL__CONSTRUCTOR);
		// Cross-reference node text may include surrounding hidden whitespace. When serializing
		// without a node model, use the already-linked member type's source-level name.
		final var innerTypeName = nodes.isEmpty()
				? call.getConstructor().getDeclaringType().getSimpleName()
				: nodes.getFirst().getText().trim();
		for (final JvmMember member : declaredType.getMembers()) {
			if (member instanceof JvmGenericType innerType
					&& innerType.getSimpleName().equals(innerTypeName)
					&& (!innerType.isStatic() || innerType.isInterface())) {
				final var descriptions = new ArrayList<IEObjectDescription>();
				final var name = QualifiedName.create(innerTypeName);
				for (final JvmConstructor constructor : innerType.getDeclaredConstructors()) {
					descriptions.add(new ConstructorDescription(name, constructor,
							ConstructorScopes.CONSTRUCTOR_BUCKET, session.isVisible(constructor)));
				}
				return new SimpleScope(IScope.NULLSCOPE, descriptions);
			}
		}
		return null;
	}
}
