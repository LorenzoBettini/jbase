/*
 * generated by Xtext 2.11.0.RC2
 */
package jbase.ide

import com.google.inject.Guice
import jbase.JbaseRuntimeModule
import jbase.JbaseStandaloneSetup
import org.eclipse.xtext.util.Modules2

/**
 * Initialization support for running Xtext languages as language servers.
 */
class JbaseIdeSetup extends JbaseStandaloneSetup {

	override createInjector() {
		Guice.createInjector(Modules2.mixin(new JbaseRuntimeModule, new JbaseIdeModule))
	}
	
}