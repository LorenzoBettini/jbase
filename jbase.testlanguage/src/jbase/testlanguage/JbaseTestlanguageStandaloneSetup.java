/*
 * generated by Xtext
 */
package jbase.testlanguage;

/**
 * Initialization support for running Xtext languages 
 * without equinox extension registry
 */
public class JbaseTestlanguageStandaloneSetup extends JbaseTestlanguageStandaloneSetupGenerated{

	public static void doSetup() {
		new JbaseTestlanguageStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

}

