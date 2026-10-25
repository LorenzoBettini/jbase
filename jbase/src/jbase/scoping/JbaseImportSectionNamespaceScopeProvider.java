package jbase.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider;

import jbase.jbase.XJAnonymousClass;

/** Anonymous inference parameters are not declarations in the source language. */
public class JbaseImportSectionNamespaceScopeProvider extends XImportSectionNamespaceScopeProvider {
	@Override
	protected IScope getLocalElementsScope(final IScope parent, final IScope globalScope,
			final EObject context, final EReference reference) {
		if (context instanceof XJAnonymousClass) {
			// Keep the enclosing lexical type scope, including its actual type parameters.
			return parent;
		}
		return super.getLocalElementsScope(parent, globalScope, context, reference);
	}
}
