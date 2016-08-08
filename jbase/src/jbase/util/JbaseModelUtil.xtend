package jbase.util

import com.google.inject.Inject
import com.google.inject.Singleton
import jbase.jbase.XJArrayConstructorCall
import jbase.jbase.XJArrayDimension
import jbase.jbase.XJJvmFormalParameter
import org.eclipse.xtext.common.types.JvmFormalParameter
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations
import jbase.jbase.XJConstructorCall
import org.eclipse.xtext.common.types.JvmGenericType

/**
 * Utility methods for accessing the Jbase model.
 * 
 * @author Lorenzo Bettini
 * 
 */
@Singleton
class JbaseModelUtil {

	@Inject extension JbaseNodeModelUtil
	@Inject extension IJvmModelAssociations

	/**
	 * The returned list contains XExpressions corresponding to
	 * dimension specification in a XJArrayConstructorCall;
	 * if the n-th element in the list is null then it means that no
	 * dimension expression has been specified for that dimension.
	 * For example
	 * 
	 * <pre>new int[][0][][0]</pre>
	 * 
	 * will correspond to the list [null, XNumberLiteral, null, XNumberLiteral]
	 */
	def arrayDimensionIndexAssociations(XJArrayConstructorCall c) {
		val sortedByOffset = (c.dimensions + c.indexes).sortBy[elementOffsetInProgram]

		// there's at least one dimension [ if we parsed a XJArrayConstructorCall
		val associations = <XExpression>newArrayList()

		val last = sortedByOffset.reduce [ p1, p2 |
			if (p1 instanceof XJArrayDimension) {
				if (p2 instanceof XExpression) {
					// case "[ exp"
					associations.add(p2)
				} else {
					// case "[ ["
					associations.add(null)
				}
			}
			// else "exp [ " skip and go to the next state
			p2
		]
		if (last instanceof XJArrayDimension) {
			associations.add(null)
		}

		return associations
	}

	/**
	 * This also takes into consideration Jvm model methods inferred from Jbase
	 * methods; in such case the parameter is not a XJJvmFormalParameter, but
	 * its original source is, and we return the original one.
	 */
	def XJJvmFormalParameter getOriginalParam(JvmFormalParameter p) {
		if (p instanceof XJJvmFormalParameter) {
			return p
		}

		val orig = p.sourceElements.head
		if (orig instanceof XJJvmFormalParameter) {
			return orig
		} else {
			return null
		}
	}

	def isRawType(XJConstructorCall e) {
		val type = e.constructor.eContainer
		if (type instanceof JvmGenericType)
			return
				e.typeArguments.empty &&
				!e.isExplicitTypeArguments &&
				!type.typeParameters.empty
		return false
	}
}