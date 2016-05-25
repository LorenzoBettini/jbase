/** 
 */
package jbase.mwe2.generator

import com.google.inject.Inject
import org.eclipse.xtext.xtext.generator.xbase.XbaseGeneratorFragment2
import org.eclipse.xtext.xtext.generator.xbase.XbaseUsageDetector

import static extension org.eclipse.xtext.xtext.generator.model.TypeReference.*

/** 
 * A customization of {@link XbaseGeneratorFragment2} where we override the base
 * class for runtime and UI module, so that a DSL using Jbase will inherit our
 * customization. This must be used in the new MWE2 files (starting from Xtext
 * 2.9).
 * 
 * @author Lorenzo Bettini
 */
class JbaseGeneratorFragment2 extends XbaseGeneratorFragment2 {

	@Inject extension XbaseUsageDetector

	override generate() {
		if (!grammar.inheritsXbase)
			return;

		if (projectConfig.runtime.manifest !== null) {
			projectConfig.runtime.manifest.requiredBundles.addAll(#[
				'jbase'
			])
		}
		if (projectConfig.eclipsePlugin.manifest !== null) {
			projectConfig.eclipsePlugin.manifest.requiredBundles.addAll(#[
				'jbase.ui'
			])
		}

		super.generate()
	}

	override protected contributeRuntimeGuiceBindings() {
		super.contributeRuntimeGuiceBindings()
		language.runtimeGenModule.superClass = 'jbase.DefaultJbaseRuntimeModule'.typeRef
	}

	override protected contributeEclipsePluginGuiceBindings() {
		super.contributeEclipsePluginGuiceBindings()
		language.eclipsePluginGenModule.superClass = 'jbase.ui.JbaseUiModule'.typeRef
	}

}
