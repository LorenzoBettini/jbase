package jbase.compiler;

import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmGenericArrayTypeReference;
import org.eclipse.xtext.xbase.compiler.GeneratorConfig;
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;

import com.google.inject.Inject;

import jbase.util.JbaseModelUtil;

/**
 * All parameters are NOT final by default.
 *
 * @author Lorenzo Bettini
 */
public class JbaseJvmModelGenerator extends JvmModelGenerator {

	@Inject
	private JbaseModelUtil modelUtil;

	/**
	 * Copied from JvmModelGenerator but avoid generating "final" if the original parameter
	 * was not declared as final.
	 */
	@Override
	public void generateParameter(final JvmFormalParameter parameter,
			final ITreeAppendable appendable, final boolean vararg, final GeneratorConfig config) {
		var tracedAppendable = appendable.trace(parameter);
		generateAnnotations(parameter.getAnnotations(), tracedAppendable, false, config);

		// all parameters are NOT final by default
		var originalParam = modelUtil.getOriginalParam(parameter);
		if (originalParam != null && originalParam.isFinal()) {
			tracedAppendable.append("final ");
		}

		var parameterType = parameter.getParameterType();
		if (vararg) {
			if (parameterType instanceof JvmGenericArrayTypeReference arrayType) {
				_errorSafeExtensions.serializeSafely(arrayType.getComponentType(), "Object", tracedAppendable);
			} else {
				tracedAppendable.append(
					"/* Internal Error: Parameter was vararg but not an array type. */");
			}
			tracedAppendable.append("...");
		} else {
			_errorSafeExtensions.serializeSafely(parameterType, "Object", tracedAppendable);
		}
		tracedAppendable.append(" ");
		var name = tracedAppendable.declareVariable(parameter,
			makeJavaIdentifier(parameter.getSimpleName()));
		_treeAppendableUtil.traceSignificant(tracedAppendable, parameter).append(name);
	}
}
