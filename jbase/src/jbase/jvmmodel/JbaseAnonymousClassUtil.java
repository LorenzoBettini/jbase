/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jbase.jvmmodel;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.linking.lazy.LazyURIEncoder;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.xbase.XbasePackage;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;

/**
 * Resolves the declared base type of Jbase anonymous classes.
 *
 * <p>The same lookup is needed before constructor linking during type inference and after
 * linking during code generation and serialization; keeping it here avoids those phases
 * deriving the base type differently.
 * 
 * Inspired by org.eclipse.xtend.core.jvmmodel.AnonymousClassUtil
 */
public class JbaseAnonymousClassUtil {

	@Inject
	private IQualifiedNameConverter qualifiedNameConverter;

	@Inject
	private LazyURIEncoder uriEncoder;

	@Inject
	private LinkingHelper linkingHelper;

	/** Resolves the written superclass name without linking the constructor recursively. */
	public JvmDeclaredType getSuperTypeNonResolving(final XJAnonymousClass anonymousClass,
			final IScope typeScope) {
		final var call = anonymousClass.getConstructorCall();
		final var constructor = (EObject) call.eGet(
			XbasePackage.Literals.XCONSTRUCTOR_CALL__CONSTRUCTOR, false);
		IEObjectDescription description = null;
		if (constructor != null) {
			if (!constructor.eIsProxy()) {
				// A linked constructor already identifies its declaring type's base type.
				return getSuperType(anonymousClass);
			}
			// Read the written type name from the constructor cross-reference without resolving
			// the constructor: resolving it here would re-enter the inference that needs this type.
			final var fragment = EcoreUtil.getURI(constructor).fragment();
			final var node = uriEncoder.getNode(call, fragment);
			final var name = linkingHelper.getCrossRefNodeAsString(node, true);
			final var superTypeName = qualifiedNameConverter.toQualifiedName(name);
			description = typeScope.getSingleElement(superTypeName);
		}
		if (description == null || !EcoreUtil2.isAssignableFrom(
				TypesPackage.Literals.JVM_DECLARED_TYPE, description.getEClass())) {
			// An unresolved or invalid base type still gives the anonymous type Java's Object base.
			description = typeScope.getSingleElement(QualifiedName.create("java", "lang", "Object"));
		}
		if (description != null && EcoreUtil2.isAssignableFrom(
				TypesPackage.Literals.JVM_DECLARED_TYPE, description.getEClass())) {
			final var type = (JvmDeclaredType) description.getEObjectOrProxy();
			if (!type.eIsProxy()) {
				return type;
			}
			return (JvmDeclaredType) EcoreUtil.resolve(type, anonymousClass);
		}
		return null;
	}

	/** Returns the superclass or implemented interface of a linked anonymous constructor. */
	public JvmGenericType getSuperType(final XJAnonymousClass anonymousClass) {
		final var constructor = anonymousClass.getConstructorCall().getConstructor();
		if (constructor != null && !constructor.eIsProxy()) {
			final var declaringType = constructor.getDeclaringType();
			final var superType = Iterables.getLast(declaringType.getSuperTypes()).getType();
			if (superType instanceof JvmGenericType generic) {
				return generic;
			}
		}
		return null;
	}
}
