package jbase.testlanguage.tests;

public final class SealedTypes {
	private SealedTypes() {
	}

	public sealed interface Shape permits Circle, Square {
	}

	public record Circle(int radius) implements Shape {
	}

	public record Square(int side) implements Shape {
	}

	public sealed interface Nested permits NestedBranch, NestedEnd {
	}

	public sealed interface NestedBranch extends Nested permits NestedLeft, NestedRight {
	}

	public record NestedLeft() implements NestedBranch {
	}

	public record NestedRight() implements NestedBranch {
	}

	public record NestedEnd() implements Nested {
	}

	public sealed interface Records permits IntRecord, RecordEnd {
	}

	public record IntRecord(int value) implements Records {
	}

	public record RecordEnd() implements Records {
	}

	public sealed interface OpenHierarchy permits OpenBranch, ClosedBranch {
	}

	public non-sealed interface OpenBranch extends OpenHierarchy {
	}

	public record ClosedBranch() implements OpenHierarchy {
	}

	public sealed interface Generic<T> permits GenericString, GenericInteger {
	}

	public record GenericString(String value) implements Generic<String> {
	}

	public record GenericInteger(Integer value) implements Generic<Integer> {
	}

	public sealed interface EnumHierarchy permits Color {
	}

	public enum Color implements EnumHierarchy {
		RED, BLUE
	}
}
