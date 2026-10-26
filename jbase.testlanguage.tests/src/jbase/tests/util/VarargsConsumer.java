package jbase.tests.util;

/**
 * Test functional interface with a varargs parameter for lambda code-generation coverage.
 */
@FunctionalInterface
public interface VarargsConsumer {

	/**
	 * Accepts a prefix and any number of values.
	 *
	 * @param prefix the fixed argument
	 * @param values the variable number of remaining arguments
	 */
	void accept(String prefix, String... values);
}
