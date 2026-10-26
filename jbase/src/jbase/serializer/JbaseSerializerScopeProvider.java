/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package jbase.serializer;

import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.serializer.SerializerScopeProvider;

import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;
import jbase.jvmmodel.JbaseAnonymousClassUtil;

/**
 * Restores the correct constructor scope when serializing anonymous classes.
 *
 * <p>The anonymous class is represented by a synthetic JVM type, but the source constructor
 * call names the constructor of its base type. Serialization therefore needs the same
 * anonymous-class-specific scope used when the call was originally linked.
 * 
 * Inspired by org.eclipse.xtend.core.serializer.XtendSerializerScopeProvider
 */
public class JbaseSerializerScopeProvider extends SerializerScopeProvider {

	@Inject
	private JbaseAnonymousClassUtil anonymousClassUtil;

	@Override
	protected IScope doCreateConstructorCallSerializationScope(XConstructorCall context) {
		if (context.eContainer() instanceof XJAnonymousClass) {
			final XJAnonymousClass anonymousClass = (XJAnonymousClass) context.eContainer();
			final JvmType superType = anonymousClassUtil.getSuperType(anonymousClass);
			if(superType != null) {
				// Serialize against the source base type, not the inferred synthetic subclass.
				return createAnonymousClassConstructorScope(context, superType);
			}
		}
		return super.doCreateConstructorCallSerializationScope(context);
	}

}
