package jbase.conversion;

import org.eclipse.xtext.conversion.impl.IDValueConverter;
import org.eclipse.xtext.nodemodel.INode;

/** Validates Unicode identifier code points according to Java's identifier rules. */
public class JbaseIdentifierValueConverter extends IDValueConverter {

	@Override
	public String toValue(final String string, final INode node) {
		return JavaIdentifierValidator.validate(super.toValue(string, node), node);
	}
}
