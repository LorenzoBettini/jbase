package jbase.annotations.validation

import org.eclipse.xtext.xbase.annotations.validation.AnnotationValueValidator
import org.eclipse.xtext.xbase.XAbstractFeatureCall
import org.eclipse.xtext.xbase.XExpression
import jbase.jbase.XJClassObject

/**
 * To deal with type literals and class objects.
 * 
 * @author Lorenzo Bettini
 */
class JbaseAnnotationValueValidator extends AnnotationValueValidator {

	override protected isValidAnnotationValue(XExpression expression) {
		if (expression instanceof XJClassObject)
			return true
		super.isValidAnnotationValue(expression)
	}

	override protected _isValidAnnotationValue(XAbstractFeatureCall expression) {
		// type literals are not valid in Jbase: you must use class object
		if (expression.isTypeLiteral)
			return false
		super._isValidAnnotationValue(expression)
	}

}
