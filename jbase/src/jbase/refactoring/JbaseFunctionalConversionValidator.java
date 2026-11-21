package jbase.refactoring;

import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XMemberFeatureCall;
import org.eclipse.xtext.xbase.XReturnExpression;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;

import com.google.inject.Inject;

import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJMethodReference;
import jbase.jbase.XJSemicolonStatement;

/** Checks that a direct conversion retains its target and selected executable. */
public class JbaseFunctionalConversionValidator {
	@Inject private ILocationInFileProvider locations;
	@Inject private IResourceValidator validator;
	@Inject private IBatchTypeResolver typeResolver;

	/** Reparse in a separate resource supplied by the caller, leaving the original untouched. */
	public boolean preservesBinding(final XClosure original, final XtextResource copy,
			final String source, final String replacement) {
		final var region = locations.getFullTextRegion(original);
		final var oldTarget = typeResolver.resolveTypes(original).getActualType(original);
		final var oldFeature = executable(original);
		final var arrayCreation = isArrayCreation(original);
		if (oldFeature == null && !arrayCreation) {
			return false;
		}
		final var updated = source.substring(0, region.getOffset()) + replacement
			+ source.substring(region.getOffset() + region.getLength());
		try {
			copy.reparse(updated);
			if (validator.validate(copy, CheckMode.ALL, CancelIndicator.NullImpl).stream()
					.anyMatch(issue -> issue.getSeverity() == Severity.ERROR)) {
				return false;
			}
			for (final var iterator = copy.getAllContents(); iterator.hasNext();) {
				if (iterator.next() instanceof XClosure candidate) {
					final var candidateRegion = locations.getFullTextRegion(candidate);
					if (candidateRegion.getOffset() == region.getOffset()) {
						final var newFeature = executable(candidate);
						final var newTarget = typeResolver.resolveTypes(candidate)
							.getActualType(candidate);
						return oldTarget.getJavaIdentifier().equals(newTarget.getJavaIdentifier())
							&& (arrayCreation ? isArrayCreation(candidate)
								: newFeature != null && oldFeature.getIdentifier()
									.equals(newFeature.getIdentifier()));
					}
				}
			}
		} catch (Exception exception) {
			return false;
		}
		return false;
	}

	private JvmIdentifiableElement executable(final XClosure expression) {
		if (expression instanceof XJMethodReference reference) {
			return reference.getExecutable();
		}
		var body = expression.getExpression();
		if (body instanceof XBlockExpression block && block.getExpressions().size() == 1) {
			body = block.getExpressions().getFirst();
			if (body instanceof XJSemicolonStatement statement) {
				body = statement.getExpression();
			}
			if (body instanceof XReturnExpression returned) {
				body = returned.getExpression();
			}
		}
		if (body instanceof XMemberFeatureCall call) {
			return call.getFeature();
		}
		if (body instanceof XConstructorCall call) {
			return call.getConstructor();
		}
		return null;
	}

	private boolean isArrayCreation(final XClosure expression) {
		return expression instanceof XJMethodReference reference
			? "new".equals(reference.getMember())
				&& reference.getType() != null
				&& reference.getType().getIdentifier().endsWith("[]")
			: expression.getExpression() instanceof XJArrayConstructorCall;
	}
}
