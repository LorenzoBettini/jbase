package jbase.tests;

import com.google.inject.Inject;
import jbase.testlanguage.validation.JbaseTestlanguageValidator;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A compilation showcase of the Java features supported by Jbase.
 * Keep this single program up to date when adding Java features.
 */
@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class AllJavaFeaturesTest extends JbaseAbstractCompilerTest {

	@Inject
	private JbaseTestlanguageValidator validator;

	@Test
	public void testAllJavaFeatures() throws Exception {
		validator.setSkipVariableInitializationCheck(false);
		checkCompilation("""
			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			import java.util.ArrayList;
			import java.util.List;
			import java.util.function.IntFunction;
			import java.util.function.IntSupplier;
			import java.util.function.IntUnaryOperator;
			import java.util.function.Supplier;
			import jbase.testlanguage.tests.RecordPatternTypes.Point;
			import jbase.testlanguage.tests.SealedTypes.Shape;
			import jbase.testlanguage.tests.SealedTypes.Circle;
			import jbase.testlanguage.tests.SealedTypes.Square;
			import static java.lang.Math.abs;

			@SuppressWarnings({"unused", "deprecation"})
			value : Object

			method @Deprecated echo(final String prefix, String... parts) : String {
				this.value = prefix;
				return prefix + parts[0];
			}

			op sum(final int... values) : int {
				int total = 0;
				for (final int value : values) {
					total += value;
				}
				return total;
			}

			// Primitive types, literals, declarations, assignments and operators.
			final boolean enabled = true;
			assert enabled;
			assert enabled : "Enable assertions with -ea";
			byte small = (byte) 1;
			short medium = (short) 2;
			short positive = +1;
			char letter = 'a';
			int count = 0, limit = 3;
			int positiveCount = +count;
			int promoted = +small;
			long mask = 0xFFL;
			int binary = 0b1010;
			int octal = 077;
			float fraction = 1.5f;
			double decimal = 2.5e2;
			String text = "hello";
			String multiline = \"\"\"
				first line
				  second line
				\"\"\";
			Object absent = null;
			int hexadecimal = 0xFF;
			int assigned;
			assigned = count = 1;
			count += 2;
			count -= 1;
			count *= 2;
			count /= 2;
			count %= 3;
			count++;
			count--;
			++count;
			--count;
			int arithmetic = count + (-limit) * 2 / 1 % 3;
			mask &= 15L;
			mask |= 16L;
			mask ^= 1L;
			long bits = (~mask & 3L) | (mask ^ 2L);
			long shifted = (mask << 1) + (mask >> 1) + (mask >>> 1);
			count <<= 1;
			count >>= 1;
			count >>>= 1;
			boolean comparison = count < limit && count <= limit || count > limit && count >= limit;
			boolean equality = text == absent || text != absent;
			boolean logical = !enabled | (enabled & false) ^ true;
			String choice = comparison ? text : "fallback";
			Object boxed = count;
			int unboxed = (Integer) boxed;
			boolean stringValue = boxed instanceof String;
			if (boxed instanceof String matched) System.out.println(matched);
			if (!(boxed instanceof String matched)) System.out.println(boxed);
			else System.out.println(matched);
			Object point = new Point(1, 2);
			if (point instanceof Point(int x, var y)) System.out.println(x + y);
			int pointSum = switch (point) {
				case Point(var x, var y) -> x + y;
				default -> 0;
			};
			Shape shape = new Circle(1);
			int shapeSize = switch (shape) {
				case Circle circle -> circle.radius();
				case Square square -> square.side();
			};
			Class<?> type = String.class;
			Class<?> arrayType = String[].class;
			Class<int[][]> primitiveArrayType = int[][].class;
			Class<String[][][]> referenceArrayType = String[][][].class;
			;

			// Arrays, constructors, generics, diamond inference and wildcards.
			int[] numbers = {1, 2, 3};
			int[] empty = {};
			int[] sized = new int[limit];
			int[][] matrix = new int[][] {{1, 2}, {3, 4}};
			int[][] partial = new int[limit][];
			matrix[0][1] = numbers[0];
			numbers[0] += 2;
			matrix[1][0] *= numbers[0];
			numbers[1] = numbers[1] + matrix[0][1];
			int length = matrix[0].length;
			int[] copy = numbers.clone();
			List<String> names = new ArrayList<>();
			names.add(text);
			if (names instanceof ArrayList<String> arrayList) System.out.println(arrayList);
			boolean arrayListCheck = names instanceof ArrayList<String>;
			List<? extends CharSequence> readable = names;
			List<? super String> writable = new ArrayList<Object>();
			writable.add(choice);
			var inferred = new ArrayList<String>();
			final var offset = 2;
			inferred.add(names.get(0));

			// Branches, all loop forms, break, continue and switch fall-through.
			if (enabled) count = abs(-1);
			else count = 0;
			if (count != 0) {
				count = sum(numbers);
			}
			for (int i = 0, j = limit; i < j; i++, j--) {
				if (i == 1) continue;
				numbers[i] = i;
			}
			for (var i = 0; i < limit; i++) {
				if (i == 2) break;
			}
			for (var name : names) {
				System.out.println(name);
			}
			while (count < limit) count++;
			do {
				count--;
			} while (count > 0);
			for (;;) {
				break;
			}
			switch (count) {
			case 0:
			case 1:
				count++;
				break;
			default:
				count = 0;
			}
			switch (text) {
			case "hello":
				System.out.println(text);
				break;
			default:
				System.out.println(choice);
			}

			// Java 21 switch expressions, guarded type rules, null labels and yield.
			int selected = switch (boxed) {
				case String s when s.length() > 1 -> s.length();
				case Integer integerValue -> { yield integerValue.intValue(); }
				case null, default -> -1;
			};
			switch (selected) {
				case 0, 1 -> System.out.println(selected);
				default -> System.out.println("other");
			}

			Long boxedLong = 1L;
			int fromBoxedLong = switch (boxedLong) { case Long longNumber -> longNumber.intValue(); };
			Boolean boxedBoolean = true;
			boolean fromBoxedBoolean = switch (boxedBoolean) { case Boolean b -> b.booleanValue(); };
			Float boxedFloat = 1.0f;
			float fromBoxedFloat = switch (boxedFloat) { case Float f -> f.floatValue(); };
			Double boxedDouble = 1.0;
			double fromBoxedDouble = switch (boxedDouble) { case Double d -> d.doubleValue(); };

			// Expression and block lambdas, explicit parameters and captured locals.
			IntUnaryOperator increment = n -> n + offset;
			IntUnaryOperator decrement = (final int n) -> {
				return n - offset;
			};
			IntSupplier supplier = () -> increment.applyAsInt(offset);
			var task = (Runnable) () -> {
				System.out.println(supplier.getAsInt());
			};
			task.run();
			IntUnaryOperator identity = (final var n) -> n;

			// Static, bound, unbound, constructor and array constructor references.
			IntUnaryOperator absolute = Math::abs;
			IntSupplier textLength = text::length;
			java.util.function.ToIntFunction<String> unboundLength = String::length;
			Supplier<ArrayList<String>> listFactory = ArrayList<String>::new;
			IntFunction<int[]> arrayFactory = int[]::new;

			// Exceptions, resource management and synchronization.
			try {
				if (absent == null) throw new IllegalArgumentException(text);
			} catch (final IllegalArgumentException e) {
				System.out.println(e.getMessage());
			} finally {
				System.out.println("done");
			}
			try {
				throw new IOException(text);
			} catch (IOException | IllegalArgumentException e) {
				System.out.println(e.getMessage());
			}
			try (final var first = new ByteArrayInputStream(new byte[] {1});
				ByteArrayInputStream second = new ByteArrayInputStream(new byte[] {2})) {
				System.out.println(first.read() + second.read());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			try (InputStream input = new ByteArrayInputStream(new byte[] {1})) {
				System.out.println(input.read());
			} catch (IOException | IllegalArgumentException e) {
				System.out.println(e.getMessage());
			}
			var existingInput = new ByteArrayInputStream(new byte[] {3});
			try (existingInput; var secondInput = new ByteArrayInputStream(new byte[] {4})) {
				System.out.println(existingInput.read());
				System.out.println(secondInput.read());
			}
			synchronized (names) {
				names.clear();
			}
			""", """
			package jbasetestlanguage;

			import java.io.ByteArrayInputStream;
			import java.io.IOException;
			import java.io.InputStream;
			import java.util.ArrayList;
			import java.util.List;
			import java.util.function.IntFunction;
			import java.util.function.IntSupplier;
			import java.util.function.IntUnaryOperator;
			import java.util.function.Supplier;
			import java.util.function.ToIntFunction;
			import jbase.testlanguage.tests.RecordPatternTypes;
			import jbase.testlanguage.tests.SealedTypes;
			import org.eclipse.xtext.xbase.lib.Exceptions;

			@SuppressWarnings("all")
			public class MyFile {
			  @SuppressWarnings({ "unused", "deprecation" })
			  private Object value;

			  @Deprecated
			  public String echo(final String prefix, String... parts) {
			    this.value = prefix;
			    return (prefix + parts[0]);
			  }

			  public static int sum(final int... values) {
			    int total = 0;
			    for (final int value : values) {
			      int _tal = total;
			      total = (_tal + value);
			    }
			    return total;
			  }

			  public static void main(String[] args) throws Throwable {
			    final boolean enabled = true;
			    assert enabled;
			    assert enabled : "Enable assertions with -ea";
			    byte small = ((byte) 1);
			    short medium = ((short) 2);
			    short positive = +1;
			    char letter = 'a';
			    int count = 0;
			    int limit = 3;
			    int _plus = (+count);
			    int positiveCount = _plus;
			    int _plus_1 = (+small);
			    int promoted = _plus_1;
			    long mask = 0xFFL;
			    int binary = 0xa;
			    int octal = 0x3f;
			    float fraction = 1.5f;
			    double decimal = 2.5e2;
			    String text = "hello";
			    String multiline = \"\"\"
			    first line
			      second line
			    \"\"\";
			    Object absent = null;
			    int hexadecimal = 0xFF;
			    int assigned = 0;
			    assigned = (count = 1);
			    int _count = count;
			    count = (_count + 2);
			    int _count_1 = count;
			    count = (_count_1 - 1);
			    int _count_2 = count;
			    count = (_count_2 * 2);
			    int _count_3 = count;
			    count = (_count_3 / 2);
			    int _count_4 = count;
			    count = (_count_4 % 3);
			    count++;
			    count--;
			    ++count;
			    --count;
			    int _minus = (-limit);
			    int _multiply = (_minus * 2);
			    int _divide = (_multiply / 1);
			    int _modulo = (_divide % 3);
			    int arithmetic = (count + _modulo);
			    long _mask = mask;
			    mask = (_mask & 15L);
			    long _mask_1 = mask;
			    mask = (_mask_1 | 16L);
			    long _mask_2 = mask;
			    mask = (_mask_2 ^ 1L);
			    long _bitwiseNot = (~mask);
			    long _bitwiseAnd = (_bitwiseNot & 3L);
			    long bits = (_bitwiseAnd | (mask ^ 2L));
			    long shifted = (((mask << 1) + (mask >> 1)) + (mask >>> 1));
			    count <<= 1;
			    count >>= 1;
			    count >>>= 1;
			    boolean comparison = (((count < limit) && (count <= limit)) || ((count > limit) && (count >= limit)));
			    boolean equality = ((text == absent) || (text != absent));
			    boolean _not = (!enabled);
			    boolean logical = (_not | ((enabled & false) ^ true));
			    String _xjconditionalexpression = null;
			    if (comparison) {
			      _xjconditionalexpression = text;
			    } else {
			      _xjconditionalexpression = "fallback";
			    }
			    String choice = _xjconditionalexpression;
			    Object boxed = Integer.valueOf(count);
			    int unboxed = (((Integer) boxed)).intValue();
			    boolean stringValue = (boxed instanceof String);
			    if ((boxed instanceof String matched)) {
			      System.out.println(matched);
			    }
			    if ((!(boxed instanceof String matched))) {
			      System.out.println(boxed);
			    } else {
			      System.out.println(matched);
			    }
			    Object point = new RecordPatternTypes.Point(1, 2);
			    if ((point instanceof RecordPatternTypes.Point(int x, var y))) {
			      System.out.println((x + y));
			    }
			    int pointSum = switch (point) {
			      case RecordPatternTypes.Point(var x, var y) -> {
			        yield (x + y);
			      }
			      default -> {
			        yield 0;
			      }
			    };
			    SealedTypes.Shape shape = new SealedTypes.Circle(1);
			    int shapeSize = switch (shape) {
			      case SealedTypes.Circle circle -> {
			        yield circle.radius();
			      }
			      case SealedTypes.Square square -> {
			        yield square.side();
			      }
			    };
			    Class<?> type = String.class;
			    Class<?> arrayType = String[].class;
			    Class<int[][]> primitiveArrayType = int[][].class;
			    Class<String[][][]> referenceArrayType = String[][][].class;;
			    int[] numbers = { 1, 2, 3 };
			    int[] empty = {};
			    int[] sized = new int[limit];
			    int[][] matrix = new int[][] { new int[] { 1, 2 }, new int[] { 3, 4 } };
			    int[][] partial = new int[limit][];
			    matrix[0][1] = numbers[0];
			    numbers[0] += 2;
			    matrix[1][0] *= numbers[0];
			    int _plus_2 = (numbers[1] + matrix[0][1]);
			    numbers[1] = _plus_2;
			    int length = matrix[0].length;
			    int[] copy = numbers.clone();
			    List<String> names = new ArrayList<String>();
			    names.add(text);
			    if ((names instanceof ArrayList<String> arrayList)) {
			      System.out.println(arrayList);
			    }
			    boolean arrayListCheck = (names instanceof ArrayList<String>);
			    List<? extends CharSequence> readable = names;
			    List<? super String> writable = new ArrayList<Object>();
			    writable.add(choice);
			    ArrayList<String> inferred = new ArrayList<String>();
			    final int offset = 2;
			    inferred.add(names.get(0));
			    if (enabled) {
			      count = Math.abs(-1);
			    } else {
			      count = 0;
			    }
			    if ((count != 0)) {
			      count = MyFile.sum(numbers);
			    }
			    for (int i = 0, j = limit; (i < j); i++, j--) {
			      {
			        if ((i == 1)) {
			          continue;
			        }
			        numbers[i] = i;
			      }
			    }
			    for (int i = 0; (i < limit); i++) {
			      if ((i == 2)) {
			        break;
			      }
			    }
			    for (String name : names) {
			      System.out.println(name);
			    }
			    while ((count < limit)) {
			      count++;
			    }
			    do {
			      count--;
			    } while((count > 0));
			    for (;;) {
			      break;
			    }
			    switch (count) {
			      case 0:
			      case 1:
			        count++;
			        break;
			      default:
			        count = 0;
			    }
			    switch (text) {
			      case "hello":
			        System.out.println(text);
			        break;
			      default:
			        System.out.println(choice);
			    }
			    int selected = switch (boxed) {
			      case String s when (s.length() > 1) -> {
			        yield s.length();
			      }
			      case Integer integerValue -> {
			        yield integerValue.intValue();
			      }
			      case null, default -> {
			        yield -1;
			      }
			    };
			    switch (selected) {
			      case 0, 1 -> {
			        System.out.println(selected);
			      }
			      default -> {
			        System.out.println("other");
			      }
			    }
			    Long boxedLong = Long.valueOf(1L);
			    int fromBoxedLong = switch (boxedLong) {
			      case Long longNumber -> {
			        yield longNumber.intValue();
			      }
			    };
			    Boolean boxedBoolean = Boolean.valueOf(true);
			    boolean fromBoxedBoolean = switch (boxedBoolean) {
			      case Boolean b -> {
			        yield b.booleanValue();
			      }
			    };
			    Float boxedFloat = Float.valueOf(1.0f);
			    float fromBoxedFloat = switch (boxedFloat) {
			      case Float f -> {
			        yield f.floatValue();
			      }
			    };
			    Double boxedDouble = Double.valueOf(1.0);
			    double fromBoxedDouble = switch (boxedDouble) {
			      case Double d -> {
			        yield d.doubleValue();
			      }
			    };
			    final IntUnaryOperator _function = (int n) -> {
			      return (n + offset);
			    };
			    IntUnaryOperator increment = _function;
			    final IntUnaryOperator _function_1 = (int n) -> {
			      return (n - offset);
			    };
			    IntUnaryOperator decrement = _function_1;
			    final IntSupplier _function_2 = () -> {
			      return increment.applyAsInt(offset);
			    };
			    IntSupplier supplier = _function_2;
			    final Runnable _function_3 = () -> {
			      System.out.println(supplier.getAsInt());
			    };
			    Runnable task = ((Runnable) _function_3);
			    task.run();
			    final IntUnaryOperator _function_4 = (int n) -> {
			      return n;
			    };
			    IntUnaryOperator identity = _function_4;
			    IntUnaryOperator absolute = Math::abs;
			    IntSupplier textLength = text::length;
			    ToIntFunction<String> unboundLength = String::length;
			    Supplier<ArrayList<String>> listFactory = ArrayList<String>::new;
			    IntFunction<int[]> arrayFactory = int[]::new;
			    try {
			      if ((absent == null)) {
			        throw new IllegalArgumentException(text);
			      }
			    } catch (final Throwable _t) {
			      if (_t instanceof IllegalArgumentException) {
			        final IllegalArgumentException e = (IllegalArgumentException)_t;
			        System.out.println(e.getMessage());
			      } else {
			        throw Exceptions.sneakyThrow(_t);
			      }
			    } finally {
			      System.out.println("done");
			    }
			    try {
			      throw new IOException(text);
			    } catch (final Throwable _t) {
			      if (_t instanceof IOException || _t instanceof IllegalArgumentException) {
			        final Exception e = (Exception)_t;
			        System.out.println(e.getMessage());
			      } else {
			        throw Exceptions.sneakyThrow(_t);
			      }
			    }
			    try (final ByteArrayInputStream first = new ByteArrayInputStream(new byte[] { 1 }); final ByteArrayInputStream second = new ByteArrayInputStream(new byte[] { 2 })) {
			      int _read = first.read();
			      int _read_1 = second.read();
			      int _plus_3 = (_read + _read_1);
			      System.out.println(_plus_3);
			    } catch (final Throwable _t) {
			      if (_t instanceof IOException) {
			        final IOException e = (IOException)_t;
			        throw new RuntimeException(e);
			      } else {
			        throw Exceptions.sneakyThrow(_t);
			      }
			    }
			    try (final InputStream input = new ByteArrayInputStream(new byte[] { 1 })) {
			      System.out.println(input.read());
			    } catch (final Throwable _t) {
			      if (_t instanceof IOException || _t instanceof IllegalArgumentException) {
			        final Exception e = (Exception)_t;
			        System.out.println(e.getMessage());
			      } else {
			        throw Exceptions.sneakyThrow(_t);
			      }
			    }
			    ByteArrayInputStream existingInput = new ByteArrayInputStream(new byte[] { 3 });
			    try (existingInput; final ByteArrayInputStream secondInput = new ByteArrayInputStream(new byte[] { 4 })) {
			      System.out.println(existingInput.read());
			      System.out.println(secondInput.read());
			    }
			    synchronized (names) {
			      names.clear();
			    }
			  }
			}
			""");
	}
}
