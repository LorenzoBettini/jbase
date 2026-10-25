package jbase.tests.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.util.TypeReferences;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelInferrer;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import com.google.inject.Inject;

import jbase.jvmmodel.JbaseAnonymousClassInferrer;

public class SimpleJvmModelInferrer implements IJvmModelInferrer {

	@Inject
	private JvmTypesBuilder jvmTypesBuilder;

	@Inject
	private TypeReferences references;

	@Inject
	private JbaseAnonymousClassInferrer anonymousClasses;

	@Override
	public void infer(final EObject object, final IJvmDeclaredTypeAcceptor acceptor,
			final boolean prelinkingPhase) {
		acceptor.accept(jvmTypesBuilder.toClass(object, "Test"), type -> {
			final var method = jvmTypesBuilder.toMethod(object, "doStuff",
					references.getTypeForName(String.class, object), operation -> {
						operation.getParameters().add(jvmTypesBuilder.toParameter(object, "s",
								references.getTypeForName(String.class, object)));
						jvmTypesBuilder.setBody(operation, (XExpression) object);
						anonymousClasses.inferLocalClasses(operation, (XExpression) object);
					});
			type.getMembers().add(method);
		});
	}
}
