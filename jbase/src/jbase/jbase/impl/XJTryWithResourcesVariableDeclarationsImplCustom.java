package jbase.jbase.impl;

import org.eclipse.xtext.xbase.lib.IterableExtensions;

import jbase.jbase.XJTryWithResourcesVariableDeclaration;

/**
 * Implements the EMF operation
 * {@link XJTryWithResourcesVariableDeclarationsImplCustom#getResourceDeclarations()}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class XJTryWithResourcesVariableDeclarationsImplCustom extends XJTryWithResourcesVariableDeclarationsImpl {
	@Override
	public Iterable<XJTryWithResourcesVariableDeclaration> getResourceDeclarations() {
		return IterableExtensions.filter(getExpressions(), XJTryWithResourcesVariableDeclaration.class);
	}
}
