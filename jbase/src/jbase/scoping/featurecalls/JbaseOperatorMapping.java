/**
 * 
 */
package jbase.scoping.featurecalls;

import static org.eclipse.xtext.naming.QualifiedName.create;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.scoping.featurecalls.OperatorMapping;

import com.google.inject.Singleton;

/**
 * To make '==' be translated exactly to '==' as in Java, not into 'equals' like
 * it happens by default in Xbase for objects, and to handle bitwise operators.
 * 
 * @author Lorenzo Bettini
 *
 */
@Singleton
public class JbaseOperatorMapping extends OperatorMapping {

	/**
	 * Special method on array that is called without parenthesis
	 */
	public static final String ARRAY_LENGTH = "length";

	public static final QualifiedName BITWISE_AND = create("&");
	public static final QualifiedName BITWISE_OR = create("|");
	public static final QualifiedName BITWISE_XOR = create("^");
	public static final QualifiedName BITWISE_NOT = create("~");

	@Override
	protected void initializeMapping() {
		super.initializeMapping();

		map.remove(TRIPLE_EQUALS);
		map.put(EQUALS, create(OP_PREFIX + "tripleEquals"));

		map.remove(TRIPLE_NOT_EQUALS);
		map.put(NOT_EQUALS, create(OP_PREFIX + "tripleNotEquals"));

		map.put(BITWISE_AND, create("bitwiseAnd"));
		map.put(BITWISE_OR, create("bitwiseOr"));
		map.put(BITWISE_XOR, create("bitwiseXor"));
		map.put(BITWISE_NOT, create("bitwiseNot"));
	}
}
