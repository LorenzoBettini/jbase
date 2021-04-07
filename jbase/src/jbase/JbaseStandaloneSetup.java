/**
 * generated by Xtext
 */
package jbase;

import com.google.inject.Injector;
import jbase.jbase.JbasePackage;
import org.eclipse.emf.ecore.EPackage;

/**
 * Initialization support for running Xtext languages without equinox extension
 * registry
 */
public class JbaseStandaloneSetup extends JbaseStandaloneSetupGenerated {
	public static void doSetup() {
		new JbaseStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

	@Override
	public void register(final Injector injector) {
		if (!EPackage.Registry.INSTANCE.containsKey("http://www.Jbase.jbase")) {
			EPackage.Registry.INSTANCE.put("http://www.Jbase.jbase", JbasePackage.eINSTANCE);
		}
		super.register(injector);
	}
}