package jbase.ui.typesystem;

import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.access.jdt.IJavaProjectProvider;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import com.google.inject.Inject;

import jbase.typesystem.JbaseSealedTypeResolver;
import jbase.ui.jdt.JbaseJdtHelper;

/** Reads permitted subclasses from the JDT model used by the IDE type provider. */
public class JbaseUiSealedTypeResolver extends JbaseSealedTypeResolver {
	@Inject
	private IJavaProjectProvider javaProjectProvider;

	@Override
	protected List<String> permittedTypeNames(final EObject context,
			final LightweightTypeReference type) {
		final var resource = type.getType().eResource();
		final var javaProject = javaProjectProvider.getJavaProject(resource.getResourceSet());
		return JbaseJdtHelper.permittedTypeNames(context, type, javaProject, super::permittedTypeNames);
	}

}
