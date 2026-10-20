package jbase.typesystem;

import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.internal.LogicalContainerAwareReentrantTypeResolver;

/**
 * Java permits capturing effectively final locals without an explicit final modifier.
 * Xbase's early writable-local diagnostic is deferred so that
 * {@link jbase.validation.JbaseValidator#checkCapturedLocal(XAbstractFeatureCall)} can inspect
 * all linked writes, including mutually exclusive first assignments to a blank local.
 * This also lets the validator distinguish a bound reference's immediate receiver read
 * from an actual lambda capture.
 */
public class JbaseReentrantTypeResolver extends LogicalContainerAwareReentrantTypeResolver {

	/**
	 * Returns no early capture error; this hook does not make mutable captures legal.
	 * For example, {@code int n = 1; IntSupplier f = () -> n; n++;} is still rejected
	 * by Jbase's later capture check. Explicit final-variable writes are checked separately.
	 */
	@Override
	protected String getInvalidWritableVariableAccessMessage(final XVariableDeclaration variable,
			final XAbstractFeatureCall featureCall, final IResolvedTypes resolvedTypes) {
		return null;
	}
}
