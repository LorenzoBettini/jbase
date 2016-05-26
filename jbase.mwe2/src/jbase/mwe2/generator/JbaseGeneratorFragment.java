/**
 * 
 */
package jbase.mwe2.generator;

import java.util.ArrayList;

import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.generator.xbase.XbaseGeneratorFragment;

import com.google.common.collect.Lists;

/**
 * A customization of {@link XbaseGeneratorFragment} where we override the base
 * class for runtime and UI module, so that a DSL using Jbase will inherit our
 * customization. This must be used in the old MWE2 files (pre Xtext 2.9).
 * 
 * @author Lorenzo Bettini
 *
 */
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
		return "jbase.ui.DefaultJbaseUiModule";
	}

	@Override
	public String[] getRequiredBundlesUi(Grammar grammar) {
		ArrayList<String> parentRequiredBundles = Lists.newArrayList(super.getRequiredBundlesUi(grammar));
		parentRequiredBundles.add("jbase.ui");
		String[] result = new String[parentRequiredBundles.size()];
		return parentRequiredBundles.toArray(result);
	}
}
