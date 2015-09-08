/**
 * 
 */
package jbase.jbase.impl;

import jbase.jbase.XJVariableDeclaration;
import jbase.jbase.impl.XJVariableDeclarationImpl;

/**
 * Custom implementation so that isWritable is always true.
 * 
 * @author Lorenzo Bettini
 *
 */
public class XJVariableDeclarationImplCustom extends XJVariableDeclarationImpl implements XJVariableDeclaration  {

	@Override
	public boolean isWriteable() {
		// in our language all variable declarations are considered writable
		// by default.
		return !isFinal();
	}

}
