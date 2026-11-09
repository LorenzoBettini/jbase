package jbase.compiler;

import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;

import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJLocalClass;

/** Emits inferred class bodies at their positions in a compiled expression. */
interface JbaseClassBodyEmitter {

	/** Emits a named local class declaration at its source position. */
	void generateLocalClass(XJLocalClass declaration, ITreeAppendable appendable);

	/** Emits the members of an anonymous class after its constructor arguments. */
	void generateAnonymousClassBody(XJAnonymousClass anonymous, JvmDeclaredType type,
			ITreeAppendable appendable);
}
