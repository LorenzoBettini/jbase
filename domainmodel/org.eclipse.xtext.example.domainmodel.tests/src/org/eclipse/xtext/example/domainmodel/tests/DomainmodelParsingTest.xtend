/*******************************************************************************
 * Copyright (c) 2016, 2019 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.example.domainmodel.tests

import com.google.inject.Inject
import jbase.jbase.JbasePackage
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.common.types.JvmOperation
import org.eclipse.xtext.common.types.TypesPackage
import org.eclipse.xtext.example.domainmodel.DomainmodelStandaloneSetup
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.example.domainmodel.domainmodel.DomainmodelFactory
import org.eclipse.xtext.example.domainmodel.domainmodel.DomainModel
import org.eclipse.xtext.example.domainmodel.domainmodel.Entity
import org.eclipse.xtext.example.domainmodel.domainmodel.Operation
import org.eclipse.xtext.example.domainmodel.domainmodel.PackageDeclaration
import org.eclipse.xtext.example.domainmodel.domainmodel.Property
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.xbase.annotations.xAnnotations.XAnnotationsFactory
import org.eclipse.xtext.serializer.ISerializer
import org.eclipse.xtext.common.types.TypesFactory
import org.eclipse.xtext.common.types.util.TypeReferences
import org.eclipse.xtext.testing.InjectWith
import org.eclipse.xtext.testing.XtextRunner
import org.eclipse.xtext.testing.util.ParseHelper
import org.eclipse.xtext.testing.validation.ValidationTestHelper
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import org.eclipse.xtext.xbase.validation.IssueCodes
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(XtextRunner)
@InjectWith(DomainmodelInjectorProvider)
class DomainmodelParsingTest{

	@Inject extension ParseHelper<DomainModel>
	@Inject extension ValidationTestHelper
	@Inject extension IJvmModelAssociations
	@Inject ISerializer serializer
	@Inject TypeReferences typeReferences

	@Test def void testAnonymousClassSerialization() {
		val source = '''
			entity Foo {
				op create():Object {
					return new Object() {
						value : int
						op getValue():int { return this.value; }
					};
				}
			}
		'''
		val model = source.parse
		model.assertNoErrors
		serializer.serialize(model).parse.assertNoErrors
		EcoreUtil.resolveAll(model)
		val copy = EcoreUtil.copy(model)
		val resource = model.eResource
		resource.contents.clear
		resource.contents.add(copy)
		serializer.serialize(copy).parse.assertNoErrors
	}

	@Test def void testAnonymousClassRequiresInterfaceImplementation() {
		"entity Foo { op create():Runnable { return new Runnable() {}; } }".parse.assertError(
			JbasePackage.Literals.XJ_ANONYMOUS_CLASS,
			IssueCodes.ANONYMOUS_CLASS_MISSING_MEMBERS
		)
	}

	@Test def void testParsing() throws Exception {
		val model = '''
			package example {
			  entity MyEntity {
			    property : String
			  }
			}
		'''.parse

		val pack = model.elements.head as PackageDeclaration
		Assert.assertEquals("example", pack.name)

		val entity = pack.elements.head as Entity
		Assert.assertEquals("MyEntity", entity.name)

		val property = entity.features.head as Property
		Assert.assertEquals("property", property.name)
		Assert.assertEquals("java.lang.String", property.type.identifier)
	}

	@Test def void testJvmTypeReferencesValidator() throws Exception {
		'''
			import java.util.List;
			package example {
			  entity MyEntity {
			    p : List<int>
			  }
			}
		'''.parse.assertError(
			TypesPackage.Literals.JVM_TYPE_REFERENCE,
			IssueCodes.INVALID_USE_OF_TYPE,
			"The primitive 'int' cannot be a type argument"
		)
	}

	@Test def void testParsingAndLinking() throws Exception {
		'''
			package example {
			  entity MyEntity {
			    property : String
			    op foo(String s) : String {
			    	this.property = s;
			    	return s.toUpperCase();
			    }
			  }
			}
		'''.parse.assertNoErrors
	}

	@Test def void testParsingAndLinkingWithImports() throws Exception {
		'''
			import java.util.List;
			package example {
			  entity MyEntity {
			    p : List<String>
			  }
			}
		'''.parse.assertNoErrors
	}

	@Test def void testReturnTypeInference() throws Exception {
		val model = '''
			package example {
			  entity MyEntity {
			    property : String
			    op foo(String s) {
			    	return property.toUpperCase() + s;
			    }
			  }
			}
		'''.parse
		val pack = model.elements.head as PackageDeclaration
		val entity = pack.elements.head as Entity
		val op = entity.features.lastOrNull as Operation
		val method = op.jvmElements.head as JvmOperation
		Assert.assertEquals("String", method.returnType.simpleName)
	}

	@Test def void testSerializeParsedArrayTypeUsesCompactBrackets() {
		val source = "entity E { names : String[ ] }"
		val model = source.parse
		model.assertNoErrors
		Assert.assertEquals("entity E { names : String[] }", serializer.serialize(model).trim)
	}

	@Test def void testSerializeNewArrayTypeUsesCompactBrackets() {
		val model = "entity E { names : String }".parse
		val entity = model.elements.head as Entity
		val property = entity.features.head as Property
		val arrayType = TypesFactory.eINSTANCE.createJvmGenericArrayTypeReference
		arrayType.componentType = typeReferences.getTypeForName(String, model)
		property.type = arrayType
		val serialized = serializer.serialize(model)
		Assert.assertTrue(serialized.contains("[]"))
		serialized.parse.assertNoErrors
	}

	@Test def void testIncompleteAnnotationIsIgnoredDuringInference() {
		val model = "entity E { op m() {} }".parse
		val resource = model.eResource as DerivedStateAwareResource
		resource.discardDerivedState
		val entity = model.elements.head as Entity
		val operation = entity.features.head as Operation
		operation.annotations += XAnnotationsFactory.eINSTANCE.createXAnnotation
		resource.installDerivedState(false)
		val method = operation.jvmElements.filter(JvmOperation).head
		Assert.assertTrue(method.annotations.empty)
	}

	@Test def void testUnspecializedFeatureIsIgnoredDuringInference() {
		val model = "entity E {}".parse
		val resource = model.eResource as DerivedStateAwareResource
		resource.discardDerivedState
		val entity = model.elements.head as Entity
		val feature = DomainmodelFactory.eINSTANCE.createFeature
		feature.name = "incomplete"
		entity.features += feature
		resource.installDerivedState(false)
		Assert.assertFalse(entity.jvmElements.empty)
		Assert.assertTrue(feature.jvmElements.empty)
	}

	@Test def void testStandaloneSetupRegistersDomainmodelResources() {
		DomainmodelStandaloneSetup.doSetup
		Assert.assertNotNull(Resource.Factory.Registry.INSTANCE.extensionToFactoryMap.get("dmodel"))
	}

}
