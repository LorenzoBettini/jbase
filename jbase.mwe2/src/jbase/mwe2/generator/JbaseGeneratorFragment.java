/**
 * 
 */
package jbase.mwe2.generator;

import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.generator.xbase.XbaseGeneratorFragment;

/**
 * A customization of {@link XbaseGeneratorFragment} where we override the
 * base class for runtime and UI module, so that a DSL using Jbase will
 * inherit our customization.
 * 
 * @author Lorenzo Bettini
 *
 */
@SuppressWarnings("restriction")
public class JbaseGeneratorFragment extends XbaseGeneratorFragment {

	/**
	 * If you use this fragment then you really want Xbase
	 */
	@Override
	protected boolean usesXbaseGrammar(Grammar grammar) {
		return true;
	}

	@Override
	protected String getTemplate() {
		// we must point to the original XbaseGeneratorFragment template
		return getClass().getSuperclass().getName().replaceAll("\\.", "::");
	}

	@Override
	public String getDefaultRuntimeModuleClassName(Grammar grammar) {
		return "jbase.DefaultJbaseRuntimeModule";
	}

	@Override
	public String getDefaultUiModuleClassName(Grammar grammar) {
		return "jbase.ui.JbaseUiModule";
	}
}
