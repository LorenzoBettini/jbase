package jbase.annotations.validation;

import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.annotations.validation.AnnotationValueValidator;

import jbase.jbase.XJClassObject;

/**
 * To deal with type literals and class objects.
 * 
 * @author Lorenzo Bettini
 */
public class JbaseAnnotationValueValidator extends AnnotationValueValidator {
	@Override
	protected boolean isValidAnnotationValue(final XExpression expression) {
		if (expression instanceof XJClassObject) {
			return true;
		}
		return super.isValidAnnotationValue(expression);
	}

	@Override
	protected boolean _isValidAnnotationValue(final XAbstractFeatureCall expression) {
		// type literals are not valid in Jbase: you must use class object
		if (expression.isTypeLiteral()) {
			return false;
		}
		return super._isValidAnnotationValue(expression);
	}
}
