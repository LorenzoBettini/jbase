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
import jbase.jbase.XJLocalClass;

/**
 * Creates JVM model types for anonymous expressions and host-defined local classes.
 *
 * <p>Xbase does not infer local JVM types for Jbase's class expressions and declarations. The
 * skeleton created here lets linking and generation treat the class body as a real type,
 * while the host language's inferrer remains responsible for its members.
 */
public class JbaseAnonymousClassInferrer {
	@Inject
	private IJvmModelAssociator associator;
	@Inject
	private JvmTypesBuilder builder;

	/** Registers classes whose bodies require no host-specific member initialization. */
	public void inferLocalClasses(final JvmFeature container, final XExpression expression) {
		inferLocalClasses(container, expression, (source, type) -> { });
	}

	/** Call after assigning the feature body, during the inferrer's initialization phase. */
	public void inferLocalClasses(final JvmFeature container, final XExpression expression,
			final BiConsumer<XJAnonymousClass, JvmGenericType> initializeMembers) {
		inferLocalClasses(container, expression, initializeMembers, (source, type) -> { });
	}

	/**
	 * Registers anonymous and host-defined named classes in lexical traversal order.
	 * The named callback supplies supertypes, modifiers and members using the host model.
	 * A default Object superclass and constructor are supplied when omitted. Member bodies
	 * must call this helper recursively, as they belong to their own JVM features.
	 */
	public void inferLocalClasses(final JvmFeature container, final XExpression expression,
			final BiConsumer<XJAnonymousClass, JvmGenericType> initializeMembers,
			final BiConsumer<XJLocalClass, JvmGenericType> initializeNamedMembers) {
		if (expression == null) {
			return;
		}
		final var contents = EcoreUtil2.getAllNonDerivedContents(expression, true);
		while (contents.hasNext()) {
			final var source = contents.next();
			if (source instanceof XJLocalClass declaration) {
				final var type = TypesFactory.eINSTANCE.createJvmGenericType();
				type.setSimpleName(declaration.getName());
				type.setVisibility(JvmVisibility.DEFAULT);
				container.getLocalClasses().add(type);
				associator.associatePrimary(declaration, type);
				initializeNamedMembers.accept(declaration, type);
				if (type.getSuperTypes().isEmpty()) {
					type.getSuperTypes().add(builder.newTypeRef(declaration, Object.class));
				}
				if (!type.getDeclaredConstructors().iterator().hasNext()) {
					type.getMembers().add(builder.toConstructor(declaration, constructor -> {
						constructor.setSimpleName(type.getSimpleName());
						constructor.setVisibility(JvmVisibility.DEFAULT);
					}));
				}
				contents.prune();
			} else if (source instanceof XJAnonymousClass anonymous) {
				final var type = TypesFactory.eINSTANCE.createJvmGenericType();
				type.setSimpleName("__Anonymous" + container.getLocalClasses().size());
				type.setAnonymous(true);
				type.setFinal(true);
				type.setVisibility(JvmVisibility.DEFAULT);
				type.getSuperTypes().add(builder.inferredType(anonymous));
				container.getLocalClasses().add(type);
				associator.associatePrimary(anonymous, type);
				initializeMembers.accept(anonymous, type);
				// The class body belongs to the new local type, but its constructor arguments
				// are evaluated in the enclosing feature and may contain nested anonymous classes.
				for (final var argument : anonymous.getConstructorCall().getArguments()) {
					associator.associateLogicalContainer(argument, container);
					inferLocalClasses(container, argument, initializeMembers, initializeNamedMembers);
				}
				contents.prune();
			}
		}
	}
}
