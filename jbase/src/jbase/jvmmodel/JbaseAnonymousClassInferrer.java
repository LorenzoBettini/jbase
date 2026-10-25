package jbase.jvmmodel;

import java.util.function.BiConsumer;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociator;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;

import com.google.inject.Inject;

import jbase.jbase.XJAnonymousClass;

/** Creates local type skeletons; the host language supplies the member inference. */
public class JbaseAnonymousClassInferrer {
	@Inject
	private IJvmModelAssociator associator;
	@Inject
	private JvmTypesBuilder builder;

	public void inferLocalClasses(final JvmFeature container, final XExpression expression) {
		inferLocalClasses(container, expression, (source, type) -> { });
	}

	/** Call after assigning the feature body, during the inferrer's initialization phase. */
	public void inferLocalClasses(final JvmFeature container, final XExpression expression,
			final BiConsumer<XJAnonymousClass, JvmGenericType> initializeMembers) {
		if (expression == null) {
			return;
		}
		final var contents = EcoreUtil2.getAllNonDerivedContents(expression, true);
		while (contents.hasNext()) {
			if (contents.next() instanceof XJAnonymousClass anonymous) {
				final var type = TypesFactory.eINSTANCE.createJvmGenericType();
				type.setSimpleName("__Anonymous" + container.getLocalClasses().size());
				type.setAnonymous(true);
				type.setFinal(true);
				type.setVisibility(JvmVisibility.DEFAULT);
				type.getSuperTypes().add(builder.inferredType(anonymous));
				container.getLocalClasses().add(type);
				associator.associatePrimary(anonymous, type);
				initializeMembers.accept(anonymous, type);
				for (final var argument : anonymous.getConstructorCall().getArguments()) {
					associator.associateLogicalContainer(argument, container);
					inferLocalClasses(container, argument, initializeMembers);
				}
				contents.prune();
			}
		}
	}
}
