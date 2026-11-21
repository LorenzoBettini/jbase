package jbase.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XFeatureCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XReturnExpression;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.references.FunctionTypeReference;
import org.eclipse.xtext.common.types.JvmOperation;

import com.google.inject.Inject;

import jbase.jbase.XJMethodReference;
import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJSemicolonStatement;

/** Builds direct Java functional-expression conversions from linked Jbase expressions. */
public class JbaseFunctionalConversion {
	@Inject private IBatchTypeResolver typeResolver;
	@Inject private ILocationInFileProvider locations;

	/** Returns replacement source, or {@code null} when a direct conversion is unsafe. */
	public String convert(final XClosure closure, final String source) {
		final var region = locations.getFullTextRegion(closure);
		final var original = source.substring(region.getOffset(), region.getOffset() + region.getLength());
		if (original.contains("/*") || original.contains("//")) {
			return null;
		}
		final var types = typeResolver.resolveTypes(closure);
		final var expected = types.getActualType(closure);
		final FunctionTypeReference function = expected.getAsFunctionTypeReference();
		if (function == null) {
			return null;
		}
		return closure instanceof XJMethodReference reference
			? referenceToLambda(reference, original, function, types)
			: lambdaToReference(closure, source, types);
	}

	private String referenceToLambda(final XJMethodReference reference, final String original,
			final FunctionTypeReference function, final IResolvedTypes types) {
		final var separator = original.indexOf("::");
		final var qualifier = original.substring(0, separator);
		final var member = original.substring(separator + 2);
		final var parameters = new ArrayList<String>();
		for (var i = 0; i < function.getParameterTypes().size(); i++) {
			parameters.add("arg" + i);
		}
		final var typeQualifier = reference.getType() != null
			|| reference.getReceiver() instanceof XAbstractFeatureCall call
				&& types.getLinkingCandidate(call).isTypeLiteral();
		final String body;
		if ("new".equals(reference.getMember())) {
			if (!reference.getTypeArguments().isEmpty()) {
				return null;
			}
			body = constructorBody(qualifier, parameters);
		} else if (reference.getExecutable() instanceof JvmOperation operation) {
			body = operationBody(qualifier, member, parameters, typeQualifier, operation);
		} else {
			return null;
		}
		return body == null ? null : "(" + String.join(", ", parameters) + ") -> " + body;
	}

	/** Builds a constructor invocation, including the single-length array form. */
	private String constructorBody(final String qualifier, final List<String> parameters) {
		if (qualifier.endsWith("[]")) {
			if (parameters.size() != 1) {
				return null;
			}
			return "new " + qualifier.substring(0, qualifier.length() - 2)
				+ "[" + parameters.getFirst() + "]";
		}
		return "new " + qualifier + "(" + String.join(", ", parameters) + ")";
	}

	/** Builds a call only for a qualifier whose evaluation remains safe after conversion. */
	private String operationBody(final String qualifier, final String member,
			final List<String> parameters, final boolean typeQualifier,
			final JvmOperation operation) {
		if (typeQualifier && !operation.isStatic()) {
			return parameters.getFirst() + "." + member + "("
				+ String.join(", ", parameters.subList(1, parameters.size())) + ")";
		}
		if (typeQualifier || "this".equals(qualifier) || "super".equals(qualifier)) {
			return qualifier + "." + member + "(" + String.join(", ", parameters) + ")";
		}
		return null;
	}

	private String lambdaToReference(final XClosure closure, final String source,
			final IResolvedTypes types) {
		final XExpression body = lambdaBody(closure);
		if (body == null || closure.getDeclaredFormalParameters().stream().anyMatch(
			parameter -> parameter.getParameterType() != null)) {
			return null;
		}
		if (body instanceof XMemberFeatureCall call && call.getFeature() instanceof JvmOperation operation) {
			return callToReference(call, operation, closure, source, types);
		}
		if (body instanceof XConstructorCall call
			&& forwarded(call.getArguments(), closure, 0)) {
			final var expression = text(call, source);
			return expression.substring(4, expression.indexOf('(')).trim() + "::new";
		}
		if (body instanceof XJArrayConstructorCall call && call.getIndexes().size() == 1
				&& call.getDimensions().size() == 1
				&& forwarded(call.getIndexes(), closure, 0)) {
			final var expression = text(call, source);
			final var start = expression.indexOf('[');
			return expression.substring(4, start).trim() + "[]::new";
		}
		return null;
	}

	/** Unwraps the single statement allowed in a direct conversion. */
	private XExpression lambdaBody(final XClosure closure) {
		XExpression body = closure.getExpression();
		if (body instanceof XBlockExpression block) {
			if (block.getExpressions().size() != 1) {
				return null;
			}
			body = block.getExpressions().getFirst();
			if (body instanceof XJSemicolonStatement statement) {
				body = statement.getExpression();
			}
			if (body instanceof XReturnExpression returned) {
				body = returned.getExpression();
			}
		}
		return body;
	}

	/** Identifies a type or forwarded receiver without changing the selected operation. */
	private String callToReference(final XMemberFeatureCall call, final JvmOperation operation,
			final XClosure closure, final String source, final IResolvedTypes types) {
		if (!call.getTypeArguments().isEmpty()) {
			return null;
		}
		final var arguments = call.getMemberCallArguments();
		final var receiver = call.getMemberCallTarget();
		final String qualifier;
		if (receiver instanceof XFeatureCall feature && types.getLinkingCandidate(feature).isTypeLiteral()
				&& forwarded(arguments, closure, 0)) {
			qualifier = text(receiver, source);
			} else if (receiver instanceof XFeatureCall feature
				&& !closure.getDeclaredFormalParameters().isEmpty()
				&& feature.getFeature() == closure.getDeclaredFormalParameters().getFirst()
				&& !operation.isStatic()) {
			if (!forwarded(arguments, closure, 1)) {
				return null;
			}
			qualifier = types.getActualType(receiver).getJavaIdentifier();
			} else {
			return null;
		}
		return qualifier + "::" + operation.getSimpleName();
	}

	private boolean forwarded(final List<XExpression> arguments, final XClosure closure,
			final int firstParameter) {
		if (arguments.size() != closure.getDeclaredFormalParameters().size() - firstParameter) {
			return false;
		}
		for (var i = 0; i < arguments.size(); i++) {
			if (!(arguments.get(i) instanceof XFeatureCall call)
					|| call.getFeature() != closure.getDeclaredFormalParameters().get(i + firstParameter)) {
				return false;
			}
		}
		return true;
	}

	private String text(final XExpression expression, final String source) {
		final var region = locations.getFullTextRegion(expression);
		return source.substring(region.getOffset(), region.getOffset() + region.getLength());
	}
}
