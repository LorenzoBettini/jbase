/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jbase.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.scoping.batch.ConstructorScopes;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class JbaseAnonymousClassConstructorScopes extends ConstructorScopes {

	@Inject
	private IJvmModelAssociations associations;

	@Override
	public IScope createConstructorScope(final EObject context, final EReference reference,
			final IFeatureScopeSession session, final IResolvedTypes resolvedTypes) {
		if (context instanceof XConstructorCall) {
			final var container = context.eContainer();
			if (container instanceof XJAnonymousClass) {
				final var inferredType = Iterables.getFirst(Iterables.filter(
					associations.getJvmElements(container), JvmGenericType.class), null);
				if (inferredType != null) {
					return createAnonymousClassConstructorScope(inferredType, context, session);
				}
			}
		}
		return super.createConstructorScope(context, reference, session, resolvedTypes);
	}
}
