package jbase.typesystem;

import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.internal.LogicalContainerAwareReentrantTypeResolver;

/**
 * Java permits capturing effectively final locals without an explicit final modifier.
 * Capture validity is checked by the Jbase validator after linking all variable uses.
 */
public class JbaseReentrantTypeResolver extends LogicalContainerAwareReentrantTypeResolver {

	@Override
	protected String getInvalidWritableVariableAccessMessage(final XVariableDeclaration variable,
			final XAbstractFeatureCall featureCall, final IResolvedTypes resolvedTypes) {
		return null;
	}
}
