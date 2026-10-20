package jbase.jbase.impl;

import jbase.jbase.XJTryWithResourcesVariableDeclaration;
import jbase.jbase.XJVariableDeclaration;

/**
 * Local variables are writable by default; resources are implicitly final.
 *
 * @author Lorenzo Bettini
 */
public class XJVariableDeclarationImplCustom extends XJVariableDeclarationImpl
		implements XJVariableDeclaration {

	@Override
	public boolean isWriteable() {
		return !isFinal() && !(this instanceof XJTryWithResourcesVariableDeclaration);
	}
}
