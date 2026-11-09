package jbase.compiler;

import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.generator.trace.ILocationData;
import org.eclipse.xtext.generator.trace.ITraceURIConverter;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.xbase.compiler.ImportManager;
import org.eclipse.xtext.xbase.compiler.output.SharedAppendableState;
import org.eclipse.xtext.xbase.compiler.output.TreeAppendable;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;

import jbase.jbase.XJAnonymousClass;
import jbase.jbase.XJLocalClass;

/**
 * Carries the generator's class-body callback through traced appendables. Compilation can
 * request class emission without injecting the generator back into the compiler.
 */
final class JbaseClassAwareTreeAppendable extends TreeAppendable {
	private final JbaseClassBodyEmitter classBodies;

	JbaseClassAwareTreeAppendable(final JbaseClassBodyEmitter classBodies, // NOSONAR: we need so many parameters to pass through the superclass constructor
			final ImportManager importManager, final ITraceURIConverter converter,
			final ILocationInFileProvider locationProvider,
			final IJvmModelAssociations associations, final EObject source,
			final String indentation, final String lineSeparator) {
		super(importManager, converter, locationProvider, associations, source,
			indentation, lineSeparator);
		this.classBodies = classBodies;
	}

	private JbaseClassAwareTreeAppendable(final JbaseClassBodyEmitter classBodies,
			final SharedAppendableState state, final ITraceURIConverter converter,
			final ILocationInFileProvider locationProvider,
			final IJvmModelAssociations associations, final Set<ILocationData> locations,
			final boolean useForDebugging) {
		super(state, converter, locationProvider, associations, locations, useForDebugging);
		this.classBodies = classBodies;
	}

	/** Delegates a named declaration to the generator using this traced appendable. */
	void generateLocalClass(final XJLocalClass declaration) {
		classBodies.generateLocalClass(declaration, this);
	}

	/** Delegates anonymous members to the generator using this traced appendable. */
	void generateAnonymousClassBody(final XJAnonymousClass anonymous,
			final JvmDeclaredType type) {
		classBodies.generateAnonymousClassBody(anonymous, type, this);
	}

	/** Retains the callback when Xbase creates an appendable for a trace region. */
	@Override
	protected TreeAppendable createChild(final SharedAppendableState state,
			final ITraceURIConverter converter,
			final ILocationInFileProvider locationProvider,
			final IJvmModelAssociations associations, final Set<ILocationData> locations,
			final boolean useForDebugging) {
		return new JbaseClassAwareTreeAppendable(classBodies, state, converter,
			locationProvider, associations, locations, useForDebugging);
	}
}
