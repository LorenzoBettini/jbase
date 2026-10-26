package jbase.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider;

import jbase.jbase.XJAnonymousClass;

/**
 * Keeps synthetic anonymous-class type parameters out of source-level name lookup.
 *
 * <p>Those parameters exist only to model the inferred JVM type. The parent scope already
 * contains the enclosing lexical declarations, including the source's actual type parameters.
 */
public class JbaseImportSectionNamespaceScopeProvider extends XImportSectionNamespaceScopeProvider {
	@Override
	protected IScope getLocalElementsScope(final IScope parent, final IScope globalScope,
			final EObject context, final EReference reference) {
		if (context instanceof XJAnonymousClass) {
			// Do not expose the synthetic parameters as if they had been declared in the source.
			return parent;
		}
		return super.getLocalElementsScope(parent, globalScope, context, reference);
	}
}
