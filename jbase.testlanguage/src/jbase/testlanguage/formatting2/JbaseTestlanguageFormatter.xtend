/*
 * generated by Xtext
 */
package jbase.testlanguage.formatting2;

import org.eclipse.xtext.formatting2.IFormattableDocument
import jbase.formatting2.JbaseFormatter
import jbase.testlanguage.jbaseTestlanguage.AbstractOperation
import jbase.testlanguage.jbaseTestlanguage.OpMethod
import jbase.testlanguage.jbaseTestlanguage.Operation
import jbase.testlanguage.jbaseTestlanguage.Property
import jbase.testlanguage.jbaseTestlanguage.JbaseTestLanguageModel

class JbaseTestlanguageFormatter extends JbaseFormatter {

	override void format(Object expr, extension IFormattableDocument document) {
		// you could use dispatch methods, but that will generate many other
		// if cases for inherited dispatch methods that will never be executed during the
		// tests and I prefer to have full control on code coverage.
		if (expr instanceof JbaseTestLanguageModel) {
			_format(expr, document);
		} else if (expr instanceof Property) {
			_format(expr, document);
		} else if (expr instanceof OpMethod) {
			_format(expr, document);
		} else if (expr instanceof Operation) {
			_format(expr, document);
		} else {
			super.format(expr, document)
		}
	}	

	def void _format(JbaseTestLanguageModel model, extension IFormattableDocument document) {
		val importSection = model.getImportSection()
		if (importSection != null) {
			// to avoid a useless newline at the beginning of the program
			model.prepend[setNewLines(0, 0, 0); noSpace]
			format(importSection, document);
		} else {
			model.prepend[setNewLines(0, 0, 1); noSpace]
		}
		for (p : model.properties) {
			format(p, document)
		}
		for (m : model.methods) {
			format(m, document);
			m.append[setNewLines(2, 2, 2)]
		}
		for (m : model.operations) {
			format(m, document);
			if (m != model.methods.last)
				m.append[setNewLines(2, 2, 2)]
			else
				m.append[setNewLines(1, 1, 2)]
		}
		formatExpressions(model.block.expressions, document, false);
	}

	def void _format(Property property, extension IFormattableDocument document) {
		property.regionForKeyword(":").surround[noSpace]
		format(property.type, document);
	}

	def void _format(OpMethod operation, extension IFormattableDocument document) {
		operation.regionForKeyword("method").append[oneSpace]
		operation.formatAbstractOperation(document)
	}

	def void _format(Operation operation, extension IFormattableDocument document) {
		operation.regionForKeyword("op").append[oneSpace]
		operation.formatAbstractOperation(document)
	}

	def void formatAbstractOperation(AbstractOperation operation, extension IFormattableDocument document) {
		operation.regionForKeyword("(").surround[noSpace]
		if (!operation.params.isEmpty) {
			for (comma : operation.regionsForKeywords(","))
				comma.prepend[noSpace].append[oneSpace]
			for (params : operation.params)
				format(params, document);
			operation.regionForKeyword(")").prepend[noSpace]
		}
		if (operation.type != null) {
			operation.regionForKeyword(":").prepend[oneSpace]
			operation.type.surround[oneSpace]
			format(operation.type, document);
		} else {
			operation.regionForKeyword(")").append[oneSpace]
		}
		format(operation.body, document);
	}
}
