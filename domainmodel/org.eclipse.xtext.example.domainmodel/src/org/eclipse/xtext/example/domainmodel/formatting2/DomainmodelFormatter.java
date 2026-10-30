/**
 * Copyright (c) 2014, 2022 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.xtext.example.domainmodel.formatting2;

import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.example.domainmodel.domainmodel.AbstractElement;
import org.eclipse.xtext.example.domainmodel.domainmodel.AnonymousClassBody;
import org.eclipse.xtext.example.domainmodel.domainmodel.LocalClass;
import org.eclipse.xtext.example.domainmodel.domainmodel.DomainModel;
import org.eclipse.xtext.example.domainmodel.domainmodel.DomainmodelPackage;
import org.eclipse.xtext.example.domainmodel.domainmodel.Entity;
import org.eclipse.xtext.example.domainmodel.domainmodel.Feature;
import org.eclipse.xtext.example.domainmodel.domainmodel.Operation;
import org.eclipse.xtext.example.domainmodel.domainmodel.PackageDeclaration;
import org.eclipse.xtext.example.domainmodel.domainmodel.Property;
import org.eclipse.xtext.formatting2.IFormattableDocument;
import org.eclipse.xtext.formatting2.regionaccess.ISemanticRegion;
import jbase.formatting2.JbaseFormatter;

/**
 * @author Moritz Eysholdt - Initial implementation and API
 */
public class DomainmodelFormatter extends JbaseFormatter {
	protected void _format(DomainModel domainmodel, IFormattableDocument document) {
		document.append(document.prepend(domainmodel, it -> {
			it.setNewLines(0, 0, 1);
			it.noSpace();
		}), this::newLine);
		format(domainmodel.getImportSection(), document);
		for (AbstractElement element : domainmodel.getElements()) {
			format(element, document);
		}
	}

	protected void _format(PackageDeclaration pkg, IFormattableDocument document) {
		ISemanticRegion open = regionFor(pkg).keyword("{");
		ISemanticRegion close = regionFor(pkg).keyword("}");
		document.surround(regionFor(pkg).feature(DomainmodelPackage.Literals.ABSTRACT_ELEMENT__NAME), this::oneSpace);
		document.append(open, this::newLine);
		document.interior(open, close, this::indent);
		for (AbstractElement element : pkg.getElements()) {
			document.format(element);
			document.append(element, it -> it.setNewLines(1, 1, 2));
		}
	}

	protected void _format(Entity entity, IFormattableDocument document) {
		ISemanticRegion open = regionFor(entity).keyword("{");
		ISemanticRegion close = regionFor(entity).keyword("}");
		document.surround(regionFor(entity).feature(DomainmodelPackage.Literals.ABSTRACT_ELEMENT__NAME), this::oneSpace);
		document.surround(entity.getSuperType(), this::oneSpace);
		document.surround(regionFor(entity).keyword("implements"), this::oneSpace);
		for (final var comma : regionFor(entity).keywords(",")) {
			document.append(document.prepend(comma, this::noSpace), this::oneSpace);
		}
		for (final var implemented : entity.getInterfaces()) {
			format(implemented, document);
		}
		document.prepend(open, this::oneSpace);
		document.append(open, this::newLine);
		document.interior(open, close, this::indent);
		format(entity.getSuperType(), document);
		for (Feature feature : entity.getFeatures()) {
			document.format(feature);
			document.append(feature, it -> it.setNewLines(1, 1, 2));
		}
	}

	/** Formats the host-owned local declaration header and reuses its member formatter. */
	protected void _format(final LocalClass declaration, final IFormattableDocument document) {
		document.append(regionFor(declaration).keyword("local"), this::oneSpace);
		document.surround(regionFor(declaration).keyword("extends"), this::oneSpace);
		document.surround(regionFor(declaration).keyword("implements"), this::oneSpace);
		for (final var comma : regionFor(declaration).keywords(",")) {
			document.append(document.prepend(comma, this::noSpace), this::oneSpace);
		}
		format(declaration.getSuperType(), document);
		for (final var implemented : declaration.getInterfaces()) {
			format(implemented, document);
		}
		final var open = regionFor(declaration).keyword("{");
		final var close = regionFor(declaration).keyword("}");
		document.append(document.prepend(open, this::oneSpace), this::newLine);
		document.prepend(close, this::newLine);
		document.interior(open, close, this::indent);
		format(declaration.getBody(), document);
	}

	protected void _format(AnonymousClassBody body, IFormattableDocument document) {
		for (final var feature : body.getFeatures()) {
			document.format(feature);
			document.append(feature, this::newLine);
		}
	}

	protected void _format(Property property, IFormattableDocument document) {
		document.surround(regionFor(property).keyword(":"), this::noSpace);
		document.format(property.getType());
	}

	protected void _format(Operation operation, IFormattableDocument document) {
		document.append(regionFor(operation).keyword("op"), this::oneSpace);
		document.surround(regionFor(operation).keyword("("), this::noSpace);
		if (!operation.getParams().isEmpty()) {
			for (ISemanticRegion comma : regionFor(operation).keywords(",")) {
				document.append(document.prepend(comma, this::noSpace), this::oneSpace);
			}
			for (JvmFormalParameter params : operation.getParams()) {
				document.format(params);
			}
			document.prepend(regionFor(operation).keyword(")"), this::noSpace);
		}
		if (operation.getType() != null) {
			document.append(regionFor(operation).keyword(")"), this::noSpace);
			document.append(document.prepend(operation.getType(), this::noSpace), this::oneSpace);
			document.format(operation.getType());
		} else {
			document.append(regionFor(operation).keyword(")"), this::oneSpace);
		}
		document.format(operation.getBody());
	}

	@Override
	public void format(Object expr, IFormattableDocument format) {
		formatUsingPolymorphicDispatcher(expr, format);
	}
}
