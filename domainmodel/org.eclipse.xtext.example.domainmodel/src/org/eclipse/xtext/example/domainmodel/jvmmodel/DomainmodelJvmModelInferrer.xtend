/*******************************************************************************
 * Copyright (c) 2011, 2021 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.example.domainmodel.jvmmodel

import com.google.inject.Inject
import jbase.jvmmodel.JbaseAnonymousClassInferrer
import org.eclipse.xtext.example.domainmodel.domainmodel.AnonymousClassBody
import org.eclipse.xtext.example.domainmodel.domainmodel.LocalClass
import org.eclipse.xtext.example.domainmodel.domainmodel.Feature
import org.eclipse.xtext.common.types.JvmAnnotationTarget
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.example.domainmodel.domainmodel.Entity
import org.eclipse.xtext.example.domainmodel.domainmodel.Operation
import org.eclipse.xtext.example.domainmodel.domainmodel.Property
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotation
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociator

class DomainmodelJvmModelInferrer extends AbstractModelInferrer {

	@Inject JbaseAnonymousClassInferrer anonymousClasses

	@Inject extension JvmTypesBuilder
	@Inject extension IQualifiedNameProvider
	@Inject extension DomainmodelJvmModelHelper
	@Inject extension IJvmModelAssociations
	@Inject extension IJvmModelAssociator

	def dispatch infer(Entity entity, extension IJvmDeclaredTypeAcceptor acceptor, boolean prelinkingPhase) {
		accept(entity.toClass( entity.fullyQualifiedName )) [
			documentation = entity.documentation
			superClass = entity.superType
			for (implemented : entity.interfaces)
				addSuperInterface(implemented)

			// let's add a default constructor
			members += entity.toConstructor []

			// and one which can be called with a lambda for initialization.
			val procedureType = typeRef(Procedure1, typeRef(it)) /* Procedure<MyEntity> */
			members += entity.toConstructor [
				parameters += entity.toParameter("initializer", procedureType)
				// here we implement the body using black box Java code.
				body = '''
					initializer.apply(this);
				'''
			]

			inferFeatures(entity.features)

			// finally we want to have a nice toString methods.
			members += entity.toToStringMethod(it)
		]
	}

	def private void inferFeatures(JvmDeclaredType it, Iterable<Feature> features) {
		for ( f : features ) {
			switch f {

				// for properties we create a field, a getter and a setter
				Property : {
					val field = f.toField(f.name, f.type)
					members += field
					members += f.toGetter(f.name, f.type)
					members += f.toSetter(f.name, f.type)
				}

				// operations are mapped to methods
				Operation : {
					val method = f.toMethod(f.name, f.type ?: inferredType) [
						documentation = f.documentation
						translateAnnotations(f.annotations)
						for (p : f.params) {
							parameters += p.toParameter(p.name, p.parameterType)
						}
						// Put the expression in the method's scope, including parameters and this.
						body = f.body
					]
					members += method
					// Attach the method before resolving anonymous members for accessor cleanup.
					anonymousClasses.inferLocalClasses(method, f.body, [ source, type |
						type.inferFeatures((source.body as AnonymousClassBody).features)
					], [ source, type |
						val declaration = source as LocalClass
						// Preserve the declared inheritance roles for JVM validation.
						type.superClass = declaration.superType ?: typeRef(Object)
						for (implemented : declaration.interfaces)
							type.addSuperInterface(implemented)
						if (source.body !== null)
							type.inferFeatures((source.body as AnonymousClassBody).features)
					])
				}
			}
		}

		// remove created getters/setters in case they
		// are explicit in the source code
		removeDuplicateGettersSetters
	}

	def private removeDuplicateGettersSetters(JvmDeclaredType inferredType) {
		inferredType.handleDuplicateJvmOperations[jvmOperations|
			// we only remove getters/setters we created automatically
			val getterOrSetter = jvmOperations.filter[primarySourceElement instanceof Property].head
			if (getterOrSetter !== null) {
				removeAllAssociation(getterOrSetter.returnType)
				for (p : getterOrSetter.parameters) {
					removeAllAssociation(p.parameterType)
					removeAllAssociation(p)
				}
				removeAllAssociation(getterOrSetter)
				inferredType.members.remove(getterOrSetter)
			}
			// other duplicated methods will be reported by the validator
		]
	}

	def private void translateAnnotations(JvmAnnotationTarget target, Iterable<XAnnotation> annotations) {
		target.addAnnotations(annotations.filterNull.filter[annotationType !== null])
	}
}
