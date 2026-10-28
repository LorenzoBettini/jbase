package jbase.scoping;

import java.util.ArrayList;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.FilteringScope;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider;

import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJLocalClass;

/**
 * Scopes named local classes lexically and hides synthetic anonymous-class type parameters.
 *
 * <p>Those parameters exist only to model the inferred JVM type. The parent scope already
 * contains the enclosing lexical declarations, including the source's actual type parameters.
 */
public class JbaseImportSectionNamespaceScopeProvider extends XImportSectionNamespaceScopeProvider {
	@Inject
	private IJvmModelAssociations associations;

	/** Adds named local types from enclosing blocks, starting at their declaration. */
	@Override
	public IScope getScope(final EObject context, final EReference reference) {
		final var global = new FilteringScope(super.getScope(context, reference), description ->
			!(description.getEObjectOrProxy() instanceof JvmGenericType type && type.isLocal()
				&& associations.getPrimarySourceElement(type) instanceof XJLocalClass));
		return localClasses(context, global);
	}

	/** Layers enclosing blocks from outermost to innermost, including the current declaration. */
	private IScope localClasses(final EObject context, final IScope parent) {
		if (context == null) return parent;
		final var outer = localClasses(context.eContainer(), parent);
		final var descriptions = new ArrayList<IEObjectDescription>();
		if (context.eContainer() instanceof XBlockExpression block) {
			final var visible = block.getExpressions().subList(0,
				block.getExpressions().indexOf(context) + 1);
			for (final var expression : visible) {
				if (expression instanceof XJLocalClass declaration) {
					for (final var inferred : associations.getJvmElements(declaration)) {
						if (inferred instanceof JvmGenericType type && type.getSimpleName() != null) {
							descriptions.add(EObjectDescription.create(
								QualifiedName.create(type.getSimpleName()), type));
						}
					}
				}
			}
		}
		return new SimpleScope(outer, descriptions);
	}

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
