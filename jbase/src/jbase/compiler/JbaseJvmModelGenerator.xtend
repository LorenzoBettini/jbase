package jbase.compiler

import com.google.inject.Inject
import jbase.util.JbaseModelUtil
import org.eclipse.xtext.common.types.JvmFormalParameter
import org.eclipse.xtext.common.types.JvmGenericArrayTypeReference
import org.eclipse.xtext.xbase.compiler.GeneratorConfig
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable

/**
 * All parameters are NOT final by default.
 * 
 * @author Lorenzo Bettini
 */
class JbaseJvmModelGenerator extends JvmModelGenerator {
	
	@Inject extension JbaseModelUtil
	
	/**
	 * Copied from JvmModelGenerator but avoid generating "final" if the original parameter
	 * was not declared as final.
	 */
	override void generateParameter(JvmFormalParameter it, ITreeAppendable appendable, boolean vararg, GeneratorConfig config) {
		val tracedAppendable = appendable.trace(it)
		annotations.generateAnnotations(tracedAppendable, false, config)
		
		// all parameters are NOT final by default
		val originalParam = it.originalParam
		if (originalParam != null && originalParam.isFinal()) {
			tracedAppendable.append("final ")	
		}

		if (vararg) {
			if (! (parameterType instanceof JvmGenericArrayTypeReference)) {
				tracedAppendable.append("/* Internal Error: Parameter was vararg but not an array type. */");
			} else {
				(parameterType as JvmGenericArrayTypeReference).componentType.serializeSafely("Object", tracedAppendable)
			}
			tracedAppendable.append("...")
		} else {
			parameterType.serializeSafely("Object", tracedAppendable)
		}
		tracedAppendable.append(" ")
		val name = tracedAppendable.declareVariable(it, makeJavaIdentifier(simpleName))
		tracedAppendable.traceSignificant(it).append(name)
	}
}