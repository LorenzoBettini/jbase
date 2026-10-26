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
				return createAnonymousClassConstructorScope(context, superType);
			}
		}
		return super.doCreateConstructorCallSerializationScope(context);
	}

}
