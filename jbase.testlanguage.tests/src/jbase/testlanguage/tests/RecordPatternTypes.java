package jbase.testlanguage.tests;

import java.util.List;

/** Records declared in Java, independently of the host language's declaration syntax. */
public class RecordPatternTypes {
	public record Point(int x, int y) {
	}

	public record Segment(Point start, Point end) {
	}

	public record Box<T>(T value) {
	}

	public record NumberBox<T extends Number>(T value) {
	}

	public interface Value<T> {
	}

	public record Implementing<T>(T value) implements Value<T> {
	}

	public record Pair(Object left, Object right) {
	}

	public record Data(String text, int[] numbers, List<String> words) {
		public static final int UNUSED = 0;
	}

	public record Empty() {
	}

	public record Counted(int value) {
		public static int calls;

		@Override
		public int value() {
			calls++;
			return value;
		}
	}

	public record Failing(int value) {
		@Override
		public int value() {
			throw new IllegalStateException("accessor");
		}
	}
}
