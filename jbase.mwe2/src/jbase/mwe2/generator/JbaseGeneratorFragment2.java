package jbase.mwe2.generator;

import org.eclipse.xtext.xtext.generator.model.ManifestAccess;
import org.eclipse.xtext.xtext.generator.model.TypeReference;
import org.eclipse.xtext.xtext.generator.xbase.XbaseGeneratorFragment2;
import org.eclipse.xtext.xtext.generator.xbase.XbaseUsageDetector;

import com.google.inject.Inject;

/**
 * A customization of {@link XbaseGeneratorFragment2} where we override the base
 * class for runtime and UI module, so that a DSL using Jbase will inherit our
 * customization. This must be used in the new MWE2 files (starting from Xtext
 * 2.9).
 * 
 * @author Lorenzo Bettini
 */
public class JbaseGeneratorFragment2 extends XbaseGeneratorFragment2 {
	@Inject
	private XbaseUsageDetector xbaseUsageDetector;

	@Override
	public void generate() {
		if (!xbaseUsageDetector.inheritsXbase(this.getGrammar())) {
			return;
		}
		ManifestAccess runtimeManifest = getProjectConfig().getRuntime().getManifest();
		if (runtimeManifest != null) {
			runtimeManifest.getRequiredBundles().add("jbase");
		}
		ManifestAccess uiManifest = getProjectConfig().getEclipsePlugin().getManifest();
		if (uiManifest != null) {
			uiManifest.getRequiredBundles().add("jbase.ui");
		}
		ManifestAccess ideManifest = getProjectConfig().getGenericIde().getManifest();
		if (ideManifest != null) {
			ideManifest.getRequiredBundles().add("jbase");
		}
		super.generate();
	}

	@Override
	protected void contributeRuntimeGuiceBindings() {
		super.contributeRuntimeGuiceBindings();
		getLanguage().getRuntimeGenModule()
			.setSuperClass(TypeReference.typeRef("jbase.DefaultJbaseRuntimeModule"));
	}

	@Override
	protected void contributeEclipsePluginGuiceBindings() {
		super.contributeEclipsePluginGuiceBindings();
		getLanguage().getEclipsePluginGenModule()
			.setSuperClass(TypeReference.typeRef("jbase.ui.DefaultJbaseUiModule"));
	}
}
