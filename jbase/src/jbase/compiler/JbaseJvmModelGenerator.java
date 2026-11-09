package jbase.compiler;

import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmGenericArrayTypeReference;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.xbase.compiler.GeneratorConfig;
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;

import com.google.inject.Inject;

import jbase.jbase.XJJvmFormalParameter;
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
	 * Mirrors Xtext's generic type generation, ending local class declarations without
	 * indentation on the following blank line. The enclosing block emits its own newline
	 * before the next statement.
	 */
	@Override
	protected ITreeAppendable _generateBody(final JvmGenericType type,
			final ITreeAppendable appendable, final GeneratorConfig config) {
		generateJavaDoc(type, appendable, config);
		final var child = appendable.trace(type);
		generateAnnotations(type.getAnnotations(), child, true, config);
		generateModifier(type, child, config);
		child.append(type.isInterface() ? "interface " : "class ");
		_treeAppendableUtil.traceSignificant(child, type)
			.append(makeJavaIdentifier(type.getSimpleName()));
		generateTypeParameterDeclaration(type, child, config);
		if (type.getTypeParameters().isEmpty()) {
			child.append(" ");
		}
		generateExtendsClause(type, child, config);
		generateMembersInBody(type, child, config);
		if (!type.isAnonymous() && !(type.eContainer() instanceof JvmType)) {
			if (type.eContainer() instanceof JvmFeature) {
				appendable.blankLine();
			} else {
				appendable.newLine();
			}
		}
		return appendable;
	}

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
		if (originalParam instanceof XJJvmFormalParameter sourceParameter) {
			final var size = sourceParameter.getArrayDimensions().size();
			for (var dimension = 0; dimension < size;
					dimension++) {
				tracedAppendable.append("[]");
			}
		}
	}
}
