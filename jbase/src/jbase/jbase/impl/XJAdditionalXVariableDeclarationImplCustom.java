/**
 * 
 */
package jbase.jbase.impl;

import org.eclipse.xtext.common.types.JvmTypeReference;

import jbase.jbase.XJAdditionalXVariableDeclaration;
import jbase.jbase.XJVariableDeclaration;

/**
 * Custom implementation so that isWritable is always true, so that
 * the declared type is implicitly the one of the containing variable declaration.
 * 
 * @author Lorenzo Bettini
 *
 */
public class XJAdditionalXVariableDeclarationImplCustom extends XJAdditionalXVariableDeclarationImpl implements XJAdditionalXVariableDeclaration  {

	@Override
	public boolean isWriteable() {
		// in our language all variable declarations are considered NOT final by default
		return getContainingVariableDeclaration().isWriteable();
	}

	/**
	 * Custom implementation that returns the type of the containing variable declaration.
	 * 
	 * @see org.eclipse.xtext.xbase.impl.XVariableDeclarationImpl#getType()
	 */
	@Override
	public JvmTypeReference getType() {
		return getContainingVariableDeclaration().getType() ;
	}

	protected XJVariableDeclaration getContainingVariableDeclaration() {
		return (XJVariableDeclaration)eContainer();
	}
}
