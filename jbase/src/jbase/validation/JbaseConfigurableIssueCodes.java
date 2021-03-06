package jbase.validation;

import org.eclipse.xtext.preferences.PreferenceKey;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.validation.IssueCodes;
import org.eclipse.xtext.validation.SeverityConverter;
import org.eclipse.xtext.xbase.validation.XbaseConfigurableIssueCodes;

/**
 * Retrieve possible errors in the generated Java code.
 * 
 * @author Lorenzo Bettini
 *
 */
public class JbaseConfigurableIssueCodes extends XbaseConfigurableIssueCodes {

	@Override
	protected void initialize(IAcceptor<PreferenceKey> iAcceptor) {
		super.initialize(iAcceptor);
		// overwrite xbase default
		iAcceptor.accept(create(IssueCodes.COPY_JAVA_PROBLEMS,
				SeverityConverter.SEVERITY_ERROR));
	}
}
