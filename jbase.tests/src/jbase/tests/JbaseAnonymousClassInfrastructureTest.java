package jbase.tests;

import static org.junit.Assert.*;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.common.types.util.TypeReferences;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.SimpleScope;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.typesystem.internal.ResolvedTypes;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import jbase.jbase.JbaseFactory;
import jbase.jbase.XJAnonymousClass;
import jbase.jvmmodel.JbaseAnonymousClassUtil;
import jbase.scoping.JbaseAnonymousClassConstructorScopes;
import jbase.tests.util.SimpleJvmModelTestInjectorProvider;
import jbase.typesystem.JbaseReentrantTypeResolver;

/** Recovery and programmatic-model boundaries, independent of a host member grammar. */
@RunWith(XtextRunner.class)
@InjectWith(SimpleJvmModelTestInjectorProvider.class)
public class JbaseAnonymousClassInfrastructureTest extends JbaseAbstractTest {
	@Inject
	private JbaseAnonymousClassUtil anonymousTypes;
	@Inject
	private jbase.jvmmodel.JbaseAnonymousClassInferrer inferrer;
	@Inject
	private JbaseAnonymousClassConstructorScopes scopes;
	@Inject
	private IBatchTypeResolver resolver;
	@Inject
	private TypeReferences references;

	@Test
	public void defaultBodyWorksWithMinimalInferrer() throws Exception {
		final var model = parseHelper.parse("{ var value = new Object() {}; return value.toString(); }");
		validationTestHelper.assertNoErrors(model);
		final var anonymous = EcoreUtil2.getAllContentsOfType(model, XJAnonymousClass.class).getFirst();
		assertTrue(resolver.resolveTypes(model).getActualType(anonymous).getType()
			instanceof JvmGenericType type && type.isAnonymous());
		assertNotNull(anonymousTypes.getSuperTypeNonResolving(anonymous, IScope.NULLSCOPE));
	}

	@Test
	public void missingTypeScopesAndIncompleteModelsRecover() {
		final var anonymous = JbaseFactory.eINSTANCE.createXJAnonymousClass();
		anonymous.setConstructorCall(JbaseFactory.eINSTANCE.createXJConstructorCall());
		assertNull(anonymousTypes.getSuperType(anonymous));
		assertNull(anonymousTypes.getSuperTypeNonResolving(anonymous, IScope.NULLSCOPE));
		assertSame(IScope.NULLSCOPE, scopes.createConstructorScope(anonymous, null, null, null));
		final var constructor = TypesFactory.eINSTANCE.createJvmConstructor();
		((InternalEObject) constructor).eSetProxyURI(URI.createURI("missing:/Constructor"));
		anonymous.getConstructorCall().setConstructor(constructor);
		assertNull(anonymousTypes.getSuperType(anonymous));
	}

	@Test
	public void unresolvedNamesUseObjectFallbackAndResolveTypeProxies() throws Exception {
		final var model = parseHelper.parse("var value = new Object() {};");
		final var anonymous = EcoreUtil2.getAllContentsOfType(model, XJAnonymousClass.class).getFirst();
		final var object = TypesFactory.eINSTANCE.createJvmGenericType();
		object.setSimpleName("Object");
		final var fallback = new SimpleScope(List.of(EObjectDescription.create(QualifiedName.create("java", "lang", "Object"), object)));
		assertSame(object, anonymousTypes.getSuperTypeNonResolving(anonymous, fallback));
		final var primitive = TypesFactory.eINSTANCE.createJvmPrimitiveType();
		primitive.setSimpleName("int");
		final var invalid = new SimpleScope(List.of(EObjectDescription.create("Object", primitive),
			EObjectDescription.create(QualifiedName.create("java", "lang", "Object"), primitive)));
		assertNull(anonymousTypes.getSuperTypeNonResolving(anonymous, invalid));

		final var actual = references.findDeclaredType(Object.class, model);
		final var proxy = TypesFactory.eINSTANCE.createJvmGenericType();
		((InternalEObject) proxy).eSetProxyURI(EcoreUtil.getURI(actual));
		final var scope = new SimpleScope(List.of(EObjectDescription.create(QualifiedName.create("Object"), proxy)));
		assertSame(actual, anonymousTypes.getSuperTypeNonResolving(anonymous, scope));
	}

	@Test
	public void nongenericSuperclassIsNotAClassType() {
		final var anonymous = JbaseFactory.eINSTANCE.createXJAnonymousClass();
		final var call = JbaseFactory.eINSTANCE.createXJConstructorCall();
		anonymous.setConstructorCall(call);
		final var local = TypesFactory.eINSTANCE.createJvmGenericType();
		final var constructor = TypesFactory.eINSTANCE.createJvmConstructor();
		local.getMembers().add(constructor);
		call.setConstructor(constructor);
		final var superType = TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
		superType.setType(TypesFactory.eINSTANCE.createJvmEnumerationType());
		local.getSuperTypes().add(superType);
		assertNull(anonymousTypes.getSuperType(anonymous));
	}
	@Test
	public void otherLocalTypeIsNotRewrittenByAnonymousPreparation() throws Exception {
		final var local = TypesFactory.eINSTANCE.createJvmGenericType();
		final var superType = TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
		superType.setType(TypesFactory.eINSTANCE.createJvmGenericType());
		local.getSuperTypes().add(superType);
		final var operation = TypesFactory.eINSTANCE.createJvmOperation();
		operation.getLocalClasses().add(local);
		final var prepare = JbaseReentrantTypeResolver.class.getDeclaredMethod(
			"prepareAnonymousTypes", ResolvedTypes.class,
			IFeatureScopeSession.class,
			JvmFeature.class, java.util.Map.class);
		prepare.setAccessible(true);
		prepare.invoke(new JbaseReentrantTypeResolver(), null, null, operation, null);
		assertSame(superType, local.getSuperTypes().getFirst());
		assertTrue(local.getMembers().isEmpty());
	}

	@Test
	public void missingFeatureBodyDoesNotCreateLocalTypes() {
		final var feature = TypesFactory.eINSTANCE.createJvmOperation();
		inferrer.inferLocalClasses(feature, null);
		assertTrue(feature.getLocalClasses().isEmpty());
	}

}
