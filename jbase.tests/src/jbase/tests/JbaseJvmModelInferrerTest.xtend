package jbase.tests

import com.google.inject.Inject
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelInferrer
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseInjectorProvider))
class JbaseJvmModelInferrerTest {

	@Inject IJvmModelInferrer inferrer
	
	@Test def void testWithANonXExpression() {
		inferrer.infer(EcoreFactory.eINSTANCE.createEClass, null, false)
	}

	@Test(expected=IllegalArgumentException)
	def void testWithNull() {
		inferrer.infer(null, null, false)
	}
}
