package jbase.compiler;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmGenericArrayTypeReference;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.generator.trace.ITraceURIConverter;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.xbase.compiler.GeneratorConfig;
import org.eclipse.xtext.xbase.compiler.IGeneratorConfigProvider;
import org.eclipse.xtext.xbase.compiler.ImportManager;
import org.eclipse.xtext.xbase.compiler.JvmModelGenerator;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;
import org.eclipse.xtext.xbase.compiler.output.TreeAppendable;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;

import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJLocalClass;
import jbase.jbase.XJJvmFormalParameter;
import jbase.util.JbaseModelUtil;

/**
 * Generates Jbase JVM types, including local and anonymous class bodies, and preserves
 * source-declared parameter modifiers.
 *
 * @author Lorenzo Bettini
 */
public class JbaseJvmModelGenerator extends JvmModelGenerator implements JbaseClassBodyEmitter {

	@Inject
	private JbaseModelUtil modelUtil;

	@Inject
	private IJvmModelAssociations associations;

	@Inject
	private IGeneratorConfigProvider generatorConfigs;

	/** Creates an appendable whose trace children retain access to class-body emission. */
	@Override
	protected TreeAppendable createAppendable(final ImportManager importManager,
			final ITraceURIConverter converter,
			final ILocationInFileProvider locationProvider,
			final IJvmModelAssociations jvmModelAssociations, final EObject source,
			final String indentation, final String lineSeparator) {
		return new JbaseClassAwareTreeAppendable(this, importManager, converter,
			locationProvider, jvmModelAssociations, source, indentation, lineSeparator);
	}

	/**
	 * Generates a named local class in its enclosing block. Its scope gives member bodies
	 * their own {@code this} and {@code super} without changing the enclosing expression.
	 */
	@Override
	public void generateLocalClass(final XJLocalClass declaration,
			final ITreeAppendable appendable) {
		final var type = (JvmGenericType) associations.getPrimaryJvmElement(declaration);
		final var config = generatorConfigs.get(declaration);
		appendable.newLine().openScope();
		assignThisAndSuper(appendable, type, config);
		generateBody(type, appendable, config);
		appendable.closeScope();
	}

	/**
	 * Generates the body of an anonymous creation after the compiler emits its constructor
	 * call. Members use the inferred JVM type and the base type's {@code this}/{@code super}.
	 */
	@Override
	public void generateAnonymousClassBody(final XJAnonymousClass anonymous,
			final JvmDeclaredType type, final ITreeAppendable appendable) {
		appendable.append(" ").openScope();
		final var config = generatorConfigs.get(anonymous);
		assignThisAndSuper(appendable, type, config);
		generateMembersInBody(type, appendable, config);
		appendable.closeScope();
	}

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
