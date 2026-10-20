package jbase.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import jbase.jbase.XJArrayConstructorCall;
import jbase.jbase.XJArrayDimension;
import jbase.jbase.XJConstructorCall;
import jbase.jbase.XJJvmFormalParameter;

/**
 * Utility methods for accessing the Jbase model.
 *
 * @author Lorenzo Bettini
 *
 */
@Singleton
public class JbaseModelUtil {

	@Inject
	private JbaseNodeModelUtil nodeModelUtil;
	@Inject
	private IJvmModelAssociations jvmModelAssociations;

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
	public List<XExpression> arrayDimensionIndexAssociations(final XJArrayConstructorCall c) {
		var sortedByOffset = Stream.concat(c.getDimensions().stream(), c.getIndexes().stream())
			.sorted(Comparator.comparingInt(nodeModelUtil::getElementOffsetInProgram)).toList();

		// there's at least one dimension [ if we parsed a XJArrayConstructorCall
		var associations = new ArrayList<XExpression>();
		EObject previous = null;
		for (var current : sortedByOffset) {
			if (previous instanceof XJArrayDimension) {
				if (current instanceof XExpression expression) {
					// case "[ exp"
					associations.add(expression);
				} else {
					// case "[ ["
					associations.add(null);
				}
			}
			// else "exp [ " skip and go to the next state
			previous = current;
		}
		if (previous instanceof XJArrayDimension) {
			associations.add(null);
		}
		return associations;
	}

	/**
	 * This also takes into consideration Jvm model methods inferred from Jbase
	 * methods; in such case the parameter is not a XJJvmFormalParameter, but
	 * its original source is, and we return the original one.
	 */
	public XJJvmFormalParameter getOriginalParam(final JvmFormalParameter p) {
		if (p instanceof XJJvmFormalParameter parameter) {
			return parameter;
		}
		var sources = jvmModelAssociations.getSourceElements(p);
		var original = sources.isEmpty() ? null : sources.iterator().next();
		return original instanceof XJJvmFormalParameter parameter ? parameter : null;
	}

	public boolean isRawType(final XJConstructorCall e) {
		var type = e.getConstructor().eContainer();
		return type instanceof JvmGenericType genericType
			&& e.getTypeArguments().isEmpty()
			&& !e.isExplicitTypeArguments()
			&& !genericType.getTypeParameters().isEmpty();
	}
}
