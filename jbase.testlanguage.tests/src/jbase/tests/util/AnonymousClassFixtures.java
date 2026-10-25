package jbase.tests.util;

/** External Java types exercising anonymous constructor and inheritance rules. */
public class AnonymousClassFixtures {
	public static class Base {
		private final int value;

		protected Base(final int value) {
			this.value = value;
		}

		public Base(final String... values) {
			value = values.length;
		}

		public Base(final Object first, final Object second) {
			value = first.toString().length() + second.toString().length();
		}

		private Base(final boolean value) {
			this.value = 0;
		}

		public int value() {
			return value;
		}
	}

	public static class Generic<T> {
		private final T value;

		public Generic(final T value) {
			this.value = value;
		}

		public T get() {
			return value;
		}
	}
}
