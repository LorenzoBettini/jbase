/*
 * generated by Xtext 2.25.0
 */
package jbase.testlanguage;


/**
 * Initialization support for running Xtext languages without Equinox extension registry.
 */
public class JbaseTestlanguageStandaloneSetup extends JbaseTestlanguageStandaloneSetupGenerated {

	public static void doSetup() {
		new JbaseTestlanguageStandaloneSetup().createInjectorAndDoEMFRegistration();
	}
}