package jbase.testlanguage.tests;

import java.util.ArrayList;
import java.util.List;

/** External Java types used to check narrowing with unconstrained and nested arguments. */
public class GenericPatternTypes<T> {
	public static class Extra<K, V> extends ArrayList<K> {
		private static final long serialVersionUID = 1L;
	}

	public static class Nested<K> extends ArrayList<List<K>> {
		private static final long serialVersionUID = 1L;
	}

	public class Inner<U> {
	}

	public class Child<U> extends Inner<U> {
	}
}
