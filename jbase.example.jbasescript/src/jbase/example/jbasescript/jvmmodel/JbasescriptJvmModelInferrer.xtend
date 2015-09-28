package jbase.example.jbasescript.jvmmodel

import com.google.inject.Inject
import jbase.example.jbasescript.jbasescript.JbasescriptLanguageModel
import jbase.example.jbasescript.jbasescript.Operation
import jbase.jbase.XJJvmFormalParameter
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder

/**
 * <p>Infers a JVM model from the source model.</p> 
 *
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>     
 */
class JbasescriptJvmModelInferrer extends AbstractModelInferrer {

    /**
     * convenience API to build and initialize JVM types and their members.
     */
	@Inject extension JvmTypesBuilder

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
	 *            indexing phase using the closure you pass as the last argument.
	 * @param isPreIndexingPhase
	 *            whether the method is called in a pre-indexing phase, i.e.
	 *            when the global index is not yet fully updated. You must not
	 *            rely on linking using the index if isPreIndexingPhase is
	 *            <code>true</code>.
	 */
   	def dispatch void infer(JbasescriptLanguageModel m, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
   		val e = m.block
		acceptor.accept(e.toClass("jbasescript." + e.eResource.name)) [
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
	
	private def inferJavaMethod(Operation o, JbasescriptLanguageModel m) {
		o.toMethod(o.name, o.type ?: inferredType) [
			documentation = m.documentation
		
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
		return s.substring(0, s.length - '.jbasescript'.length)
	}
}

