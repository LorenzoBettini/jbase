package jbase.testlanguage.jvmmodel

import com.google.inject.Inject
import jbase.jbase.XJJvmFormalParameter
import jbase.testlanguage.jbaseTestlanguage.AbstractOperation
import jbase.testlanguage.jbaseTestlanguage.JbaseTestLanguageModel
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.common.types.JvmAnnotationTarget
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotation
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import jbase.testlanguage.jbaseTestlanguage.OpMethod

/**
 * <p>Infers a JVM model from the source model.</p> 
 * 
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>     
 */
class JbaseTestlanguageJvmModelInferrer extends AbstractModelInferrer {

	/**
	 * convenience API to build and initialize JVM types and their members.
	 */
	@Inject extension JvmTypesBuilder jvmTypesBuilder

	/**
	 * The dispatch method {@code infer} is called for each instance of the
	 * given element's type that is contained in a resource.
	 * 
	 * @param element
	 *            the model to create one or more
	 *            {@link JvmDeclaredType declared
	 *            types} from.
	 * @param acceptor
	 *            each created
	 *            {@link JvmDeclaredType type}
	 *            without a container should be passed to the acceptor in order
	 *            get attached to the current resource. The acceptor's
	 *            {@link IJvmDeclaredTypeAcceptor#accept(org.eclipse.xtext.common.types.JvmDeclaredType)
	 *            accept(..)} method takes the constructed empty type for the
	 *            pre-indexing phase. This one is further initialized in the
	 *            indexing phase using the lambda you pass as the last argument.
	 * @param isPreIndexingPhase
	 *            whether the method is called in a pre-indexing phase, i.e.
	 *            when the global index is not yet fully updated. You must not
	 *            rely on linking using the index if isPreIndexingPhase is
	 *            <code>true</code>.
	 */
	def dispatch void infer(JbaseTestLanguageModel m, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		val e = m.block
		acceptor.accept(e.toClass("jbasetestlanguage." + e.eResource.name)) [
			for (p : m.properties) {
				members += p.toField(p.name, p.type) => [
					translateAnnotations(p.annotations)
				]
			}

			for (o : m.methods) {
				members += inferJavaMethod(o, m)
			}

			for (o : m.operations) {
				members += inferJavaMethod(o, m) => [ static=true ]
			}

			members += e.toMethod('main', typeRef(Void.TYPE)) [
				exceptions += typeRef(Throwable)
				parameters += e.toParameter("args", typeRef(String).addArrayTypeDimension)
				static = true
				// Associate the script as the body of the main method
				body = e
			]
		]
	}
	
	private def inferJavaMethod(AbstractOperation o, JbaseTestLanguageModel m) {
		o.toMethod(o.name, o.type ?: inferredType) [
			documentation = m.documentation
			if (o instanceof OpMethod)
				translateAnnotations(o.annotations)
			for (p : o.params) {
				inferParameter(it, p)
			}
		
			body = o.body
		]
	}

	protected def inferParameter(JvmOperation it, XJJvmFormalParameter p) {
		var parameterType = p.parameterType
		if (p.varArgs) {
			// varArgs is a property of JvmExecutable
			varArgs = p.varArgs
			parameterType = parameterType.addArrayTypeDimension
		}
		parameters += p.toParameter(p.name, parameterType)
	}

	def name(Resource res) {
		val s = res.URI.lastSegment
		return s.substring(0, s.length - '.jbasetestlanguage'.length)
	}

	def private void translateAnnotations(JvmAnnotationTarget target, Iterable<XAnnotation> annotations) {
		target.addAnnotations(annotations.filterNull.filter[annotationType !== null])
	}

	/**
	 * For testing purposes.
	 */
	def void setJvmTypesBuilder(JvmTypesBuilder jvmTypesBuilder) {
		this.jvmTypesBuilder = jvmTypesBuilder
	}
}

