/**
 * 
 */
package jbase.lib;

import org.eclipse.xtext.xbase.lib.Inline;
import org.eclipse.xtext.xbase.lib.Pure;

import com.google.common.annotations.GwtCompatible;

/**
 * This is an extension library for {@link Boolean booleans}.
 * 
 * @author Sven Efftinge - Initial contribution and API
 * @author Lorenzo Bettini - Adapted for Jbase
 * 
 * @since 0.4.0
 */
@GwtCompatible public class JbaseBooleanExtensions {
	/**
	 * A logical <code>xor</code>. This is the equivalent to the java <code>^</code> operator.
	 * 
	 * @param a
	 *            a boolean value.
	 * @param b
	 *            another boolean value.
	 * @return <code>a ^ b</code>
	 */
	@Pure
	@Inline(value="($1 ^ $2)", constantExpression=true)
	public static boolean bitwiseXor(boolean a, boolean b) {
		return a ^ b;
	}
}
