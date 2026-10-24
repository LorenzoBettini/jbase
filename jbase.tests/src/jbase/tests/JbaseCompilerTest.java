package jbase.tests;

import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;
import jbase.testlanguage.validation.JbaseTestlanguageValidator;
import jbase.tests.util.Java21JbaseTestlanguageInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * For compilation tests we use JbaseTestlanguage since we test also
 * parameters.
 */
@RunWith(XtextRunner.class)
@InjectWith(Java21JbaseTestlanguageInjectorProvider.class)
public class JbaseCompilerTest extends JbaseAbstractCompilerTest {

	@Inject
	private JbaseTestlanguageValidator validator;

	@Before
	public void initValidator() {
		validator.setSkipVariableInitializationCheck(false);
	}

	@Test
	public void testEmptyProgram() throws Exception {
		checkCompilation("", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testEmptyStatement() throws Exception {
		checkCompilation(";", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {;
			  }
			}
			""");
	}

	@Test
	public void testEmptyStatements() throws Exception {
		checkCompilation(";;", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {;;
			  }
			}
			""");
	}

	@Test
	public void testHelloWorld() throws Exception {
		checkCompilation(jbaseInputs.helloWorld(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    System.out.println("Hello world!");
			  }
			}
			""");
	}

	@Test
	public void testHelloWorldMethod() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.helloWorldMethod(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void sayHelloWorld(String m) {
			    System.out.println(m);
			  }

			  public static void main(String[] args) throws Throwable {
			    MyFile.sayHelloWorld("Hello world!");
			  }
			}
			""");
	}

	@Test
	public void testJavaLikeVariableDeclarations() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.javaLikeVariableDeclarations(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static boolean foo() {
			    int i = 0;
			    int j = 0;
			    j = 1;
			    return (i > 0);
			  }

			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    boolean b = false;
			    boolean cond = (i > 0);
			  }
			}
			""");
	}

	@Test
	public void testSimpleAccess() throws Exception {
		checkCompilation("""
			int i;
			i = 0;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    i = 0;
			  }
			}
			""");
	}

	@Test
	public void testSimpleArrayAccess() throws Exception {
		checkCompilation(jbaseInputs.simpleArrayAccess(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    String[] a = null;
			    a[0] = "test";
			  }
			}
			""");
	}

	@Test
	public void testArrayAccess() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.arrayAccess(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int getIndex() {
			    return 0;
			  }

			  public static void main(String[] args) throws Throwable {
			    String[] a = null;
			    int i = 0;
			    int j = 1;
			    a[(i + j)] = "test";
			    a[((i - MyFile.getIndex()) + 1)] = "test";
			  }
			}
			""");
	}

	@Test
	public void testArrayAssign() throws Exception {
		checkCompilation(jbaseInputs.arrayAssign(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    String[] a = null;
			    String[] b = null;
			    a = b;
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessInRightHandsideExpression() throws Exception {
		checkCompilation(jbaseInputs.arrayAccessInRightHandsideExpression(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] a = null;
			    int i = 0;
			    i = a[0];
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessFromFeatureCall() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.arrayAccessFromFeatureCall(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int[] getArray() {
			    return null;
			  }

			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    i = MyFile.getArray()[0];
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessFromMemberFeatureCallReceiver() throws Exception {
		checkCompilation(jbaseInputs.arrayAccessFromMemberFeatureCallReceiver(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[][] arr = null;
			    int l = 0;
			    l = arr[0].length;
			    System.out.println(arr[0].equals(arr[1]));
			    System.out.println(arr[0].hashCode());
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessFromMemberFeatureCallReceiver2() throws Exception {
		checkCompilation("""
			String firstArg = args[0];
			char arg = args[0].toCharArray()[0];
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    String firstArg = args[0];
			    char arg = args[0].toCharArray()[0];
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessFromMemberFeatureCallReceiverClone() throws Exception {
		checkCompilation(jbaseInputs.arrayAccessFromMemberFeatureCallReceiverClone(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[][] arr = null;
			    int[] cl = arr[0].clone();
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessFromMemberFeatureCallReceiverClone2() throws Exception {
		checkCompilation("""
			// clone has a generic type, so the result is inferred
			String[][] a = null;
			String[] cl1 = a[0].clone();
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    String[][] a = null;
			    String[] cl1 = a[0].clone();
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessAsArgument() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.arrayAccessAsArgument(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int getArg(int i) {
			    return i;
			  }

			  public static void main(String[] args) throws Throwable {
			    int[] j = null;
			    MyFile.getArg(j[0]);
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessInForLoop() throws Exception {
		checkCompilation(jbaseInputs.arrayAccessInForLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        String _plus = ((("args[" + Integer.valueOf(i)) + "] = ") + args[i]);
			        System.out.println(_plus);
			        int _i = i;
			        i = (_i + 1);
			        _while = (i < argsNum);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessInBinaryOp() throws Exception {
		checkCompilation(jbaseInputs.arrayAccessInBinaryOp(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] a = null;
			    boolean result = (a[1] > a[2]);
			  }
			}
			""");
	}

	@Test
	public void testArrayAccessInParenthesizedExpression() throws Exception {
		checkCompilation(jbaseInputs.arrayAccessInParenthesizedExpression(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] a = null;
			    int i = 0;
			    i = a[1];
			  }
			}
			""");
	}

	@Test
	public void testMultiArrayAccessInRightHandsideExpression() throws Exception {
		checkCompilation(jbaseInputs.multiArrayAccessInRightHandsideExpression(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[][] a = null;
			    int i = 0;
			    i = a[0][(1 + 2)];
			  }
			}
			""");
	}

	@Test
	public void testMultiArrayAccessInLeftHandsideExpression() throws Exception {
		checkCompilation(jbaseInputs.multiArrayAccessInLeftHandsideExpression(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[][] a = null;
			    int[][] b = null;
			    a[0][(1 + 2)] = 1;
			    a[0] = b[1];
			    a = b;
			  }
			}
			""");
	}

	@Test
	public void testArrayConstructorCallInVarDecl() throws Exception {
		checkCompilation(jbaseInputs.arrayConstructorCallInVarDecl(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] i = new int[10];
			    String[] a = new String[args.length];
			  }
			}
			""");
	}

	@Test
	public void testMultiArrayConstructorCallInVarDecl() throws Exception {
		checkCompilation(jbaseInputs.multiArrayConstructorCallInVarDecl(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[][] i = new int[10][20];
			    String[][] a = new String[args.length][(args.length + 1)];
			  }
			}
			""");
	}

	@Test
	public void testMultiArrayConstructorCallWithPartialDimensions() throws Exception {
		checkCompilation("""
			int[][][] a1 = new int[0][][];
			int[][][] a2 = new int[0][1][];
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[][][] a1 = new int[0][][];
			    int[][][] a2 = new int[0][1][];
			  }
			}
			""");
	}

	@Test
	public void testMultiArrayConstructorCallWithArrayLiteral() throws Exception {
		checkCompilation(jbaseInputs.multiArrayConstructorCallWithArrayLiteral(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] i = new int[] { 0, 1, 2 };
			    int[][] j = new int[][] { new int[] { 0, 1, 2 }, new int[] { 3, 4, 5 } };
			  }
			}
			""");
	}

	@Test
	public void testConstructorWithDiamondCallInVarDecl() throws Exception {
		checkCompilation(jbaseInputs.constructorCallWithDiamondInVarDecl(), """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.LinkedList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<String> list1 = new ArrayList<String>();
			    List<? extends String> list2 = new ArrayList<String>();
			    List<LinkedList<String>> list3 = new ArrayList<LinkedList<String>>();
			  }
			}
			""");
	}

	@Test
	public void testConstructorWithDiamondCallInVarDeclNestedWildcard() throws Exception {
		checkCompilation(jbaseInputs.constructorCallWithDiamondInVarDeclNestedWildcard(), """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.LinkedList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<LinkedList<? extends String>> list3 = new ArrayList<LinkedList<? extends String>>();
			  }
			}
			""");
	}

	@Test
	public void testConstructorWithDiamondCallInAssignment() throws Exception {
		checkCompilation(jbaseInputs.constructorCallWithDiamondInAssignment(), """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.LinkedList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<String> list1 = null;
			    ArrayList<String> _arrayList = new ArrayList<String>();
			    list1 = _arrayList;
			    List<? extends String> list2 = null;
			    ArrayList<String> _arrayList_1 = new ArrayList<String>();
			    list2 = _arrayList_1;
			    List<LinkedList<String>> list3 = null;
			    ArrayList<LinkedList<String>> _arrayList_2 = new ArrayList<LinkedList<String>>();
			    list3 = _arrayList_2;
			  }
			}
			""");
	}

	@Test
	public void testConstructorWithDiamondCallInAssignmentNestedWildcard() throws Exception {
		checkCompilation(jbaseInputs.constructorCallWithDiamondInAssignmentNestedWildcard(), """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.LinkedList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<LinkedList<? extends String>> list3 = null;
			    ArrayList<LinkedList<? extends String>> _arrayList = new ArrayList<LinkedList<? extends String>>();
			    list3 = _arrayList;
			  }
			}
			""");
	}

	@Test
	public void testIfThenElseWithoutBlocks() throws Exception {
		expectationsForIfThenElse(jbaseInputs.ifThenElseWithoutBlocks());
	}

	@Test
	public void testIfThenElseWithBlocks() throws Exception {
		expectationsForIfThenElse(jbaseInputs.ifThenElseWithBlocks());
	}

	/**
	 * Xbase compiles if then else with blocks even if they're not
	 * there in the original program
	 */
	private void expectationsForIfThenElse(final CharSequence input) throws Exception {
		checkCompilation(input, """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int _length = args.length;
			    boolean _tripleEquals = (_length == 0);
			    if (_tripleEquals) {
			      System.out.println("No args");
			    } else {
			      System.out.println("Args");
			    }
			  }
			}
			""");
	}

	@Test
	public void testArrayLiteral() throws Exception {
		checkCompilation(jbaseInputs.arrayLiteral(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] a = { 0, 1, 2 };
			  }
			}
			""");
	}

	@Test
	public void testEmptyArrayLiteral() throws Exception {
		checkCompilation(jbaseInputs.emptyArrayLiteral(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int[] a = {};
			  }
			}
			""");
	}

	@Test
	public void testWhileWithSemicolon() throws Exception {
		checkCompilation(jbaseInputs.whileWithSemicolon(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    while ((i < 10)) {;
			    }
			  }
			}
			""");
	}

	@Test
	public void testWhileWithoutBlock() throws Exception {
		expectationsForWhile(jbaseInputs.whileWithoutBlock());
	}

	@Test
	public void testWhileWithBlock() throws Exception {
		expectationsForWhile(jbaseInputs.whileWithBlock());
	}

	/**
	 * Xbase compiles if while with blocks even if they're not
	 * there in the original program
	 */
	private void expectationsForWhile(final CharSequence input) throws Exception {
		checkCompilation(input, """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    while ((i < 10)) {
			      i = (i + 1);
			    }
			  }
			}
			""");
	}

	@Test
	public void testDoWhileWithoutBlock() throws Exception {
		expectationsForDoWhile(jbaseInputs.doWhileWithoutBlock());
	}

	@Test
	public void testDoWhileWithBlock() throws Exception {
		expectationsForDoWhile(jbaseInputs.doWhileWithBlock());
	}

	/**
	 * Xbase compiles if while with blocks even if they're not
	 * there in the original program
	 */
	private void expectationsForDoWhile(final CharSequence input) throws Exception {
		checkCompilation(input, """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    do {
			      i = (i + 1);
			    } while((i < 10));
			  }
			}
			""");
	}

	@Test
	public void testAdditionalSemicolons() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.additionalSemicolons(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void m() {
			    return;
			  }

			  public static void main(String[] args) throws Throwable {
			    int i = 0;;;
			    while ((i < 10)) {
			      i = (i + 1);
			    }
			  }
			}
			""");
	}

	@Test
	public void testPostIncrement() throws Exception {
		checkCompilation(jbaseInputs.postIncrement(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int j = i++;
			    j++;
			  }
			}
			""");
	}

	@Test
	public void testPreIncrementAndDecrement() throws Exception {
		checkCompilation(jbaseInputs.preIncrementAndDecrement(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int j = ++i;
			    int _plusPlus = ++i;
			    j = _plusPlus;
			    ++j;
			    int k = --i;
			    int _minusMinus = --i;
			    k = _minusMinus;
			    --k;
			  }
			}
			""");
	}

	@Test
	public void testMultiAssign() throws Exception {
		checkCompilation(jbaseInputs.multiAssign(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int _i = i;
			    i = (_i + 2);
			  }
			}
			""");
	}

	@Test
	public void testForLoopComparisonOperators() throws Exception {
		// Regression for https://github.com/LorenzoBettini/jbase/issues/122.
		for (final var operator : new String[] { "<", "<=", ">", ">=" }) {
			checkCompilation("""
				int x = 0;
				int y = 10;
				for (int i = 0; i %s y; i++) {
					x += i;
				}
				""".formatted(operator), """
				package jbasetestlanguage;

				@SuppressWarnings("all")
				public class MyFile {
				  public static void main(String[] args) throws Throwable {
				    int x = 0;
				    int y = 10;
				    for (int i = 0; (i %s y); i++) {
				      int _x = x;
				      x = (_x + i);
				    }
				  }
				}
				""".formatted(operator));
		}
	}

	@Test
	public void testForLoopTranslatedToJavaWhileSingleStatement() throws Exception {
		checkCompilation(jbaseInputs.forLoopTranslatedToJavaWhileSingleStatement(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        System.out.println(("" + Integer.valueOf(i)));
			        int _i = i;
			        i = (_i + 1);
			        _while = (i < argsNum);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testForLoopTranslatedToJavaWhileInsideIf() throws Exception {
		checkCompilation(jbaseInputs.forLoopTranslatedToJavaWhileInsideIf(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    if ((argsNum != 0)) {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        System.out.println(("" + Integer.valueOf(i)));
			        int _i = i;
			        i = (_i + 1);
			        _while = (i < argsNum);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testForLoopTranslatedToJavaWhileNoExpression() throws Exception {
		checkCompilation(jbaseInputs.forLoopTranslatedToJavaWhileNoExpression(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = true;
			      while (_while) {
			        System.out.println(("" + Integer.valueOf(i)));
			        int _i = i;
			        i = (_i + 1);
			        _while = true;
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testForLoopTranslatedToJavaWhileEarlyExit() throws Exception {
		/**
		 *  this is not valid input since i += 1 is considered not reachable
		 *  we use it only to test the compiler.
		 *
		 *  In Xtext 2.9 the body is generated anyway (not valid Java code).
		 *
		 *  In Xtext 2.8 the body is not generated, while in Xtext 2.7.3 the body
		 *  was generated anyway:
		 *
		 * 		package jbasetestlanguage;
		 *
		 * 		@SuppressWarnings("all")
		 * 		public class MyFile {
		 * 		  public static void main(String[] args) throws Throwable {
		 * 		    int argsNum = args.length;
		 * 		    {
		 * 		      int i = 0;
		 * 		      boolean _while = (i < argsNum);
		 * 		      while (_while) {
		 * 		        return;
		 * 		      }
		 * 		    }
		 * 		  }
		 * 		}
		 */
		checkCompilation(jbaseInputs.forLoopTranslatedToJavaWhileEarlyExit(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        return;
			        int _i = i;
			        i = (_i + 1);
			        _while = (i < argsNum);
			      }
			    }
			  }
			}
			""", false);
	}

	@Test
	public void testForLoopTranslatedToJavaForNoExpression() throws Exception {
		checkCompilation("""
			int argsNum = args.length();
			for (int i = 0; ; i++)
				System.out.println(""+i);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    for (int i = 0;; i++) {
			      System.out.println(("" + Integer.valueOf(i)));
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueInForLoopTranslatedToJavaFor() throws Exception {
		checkCompilation(jbaseInputs.continueInForLoopTranslatedToJavaFor(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    for (int i = 0; (i < argsNum); i++) {
			      if ((argsNum > 0)) {
			        continue;
			      } else {
			        System.out.println("");
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueInForLoopTranslatedToJavaWhile() throws Exception {
		checkCompilation(jbaseInputs.continueInForLoopTranslatedToJavaWhile(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        if ((argsNum > 0)) {
			          int _i = i;
			          i = (_i + 1);
			          _while = (i < argsNum);
			          continue;
			        } else {
			          System.out.println("");
			        }
			        int _i_1 = i;
			        i = (_i_1 + 1);
			        _while = (i < argsNum);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueInForLoopTranslatedToJavaWhileNoExpression() throws Exception {
		checkCompilation(jbaseInputs.continueInForLoopTranslatedToJavaWhileNoExpression(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = true;
			      while (_while) {
			        if ((argsNum > 0)) {
			          int _i = i;
			          i = (_i + 1);
			          _while = true;
			          continue;
			        } else {
			          System.out.println("");
			        }
			        int _i_1 = i;
			        i = (_i_1 + 1);
			        _while = true;
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueInBothIfBranchesInForLoopTranslatedToJavaWhile() throws Exception {
		checkCompilation(jbaseInputs.continueInBothIfBranchesInForLoopTranslatedToJavaWhile(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        if ((argsNum > 0)) {
			          int _i = i;
			          i = (_i + 1);
			          _while = (i < argsNum);
			          continue;
			        } else {
			          int _i_1 = i;
			          i = (_i_1 + 1);
			          _while = (i < argsNum);
			          continue;
			        }
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueSingleInForLoopTranslatedToJavaWhile() throws Exception {
		checkCompilation(jbaseInputs.continueSingleInForLoopTranslatedToJavaWhile(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        int _i = i;
			        i = (_i + 1);
			        _while = (i < argsNum);
			        continue;
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueInForLoopTranslatedToJavaWhileAndOtherStatementsAfterLoop()
			throws Exception {
		checkCompilation(
				jbaseInputs.continueInForLoopTranslatedToJavaWhileAndOtherStatementsAfterLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        if ((argsNum > 0)) {
			          int _i = i;
			          i = (_i + 1);
			          _while = (i < argsNum);
			          continue;
			        } else {
			          System.out.println("");
			        }
			        int _i_1 = i;
			        i = (_i_1 + 1);
			        _while = (i < argsNum);
			      }
			    }
			    int j = 0;
			    System.out.println(j);
			  }
			}
			""");
	}

	@Test
	public void testContinueInWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.continueInWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    while ((i < argsNum)) {
			      if ((argsNum > 0)) {
			        continue;
			      } else {
			        System.out.println("");
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueInBothIfBranchesInWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.continueInBothIfBranchesInWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    while ((i < argsNum)) {
			      if ((argsNum > 0)) {
			        continue;
			      } else {
			        continue;
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testContinueInDoWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.continueInDoWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    do {
			      if ((argsNum > 0)) {
			        continue;
			      } else {
			        System.out.println("");
			      }
			    } while((i < argsNum));
			  }
			}
			""");
	}

	@Test
	public void testContinueInBothIfBranchesInDoWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.continueInBothIfBranchesInDoWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    do {
			      if ((argsNum > 0)) {
			        continue;
			      } else {
			        continue;
			      }
			    } while((i < argsNum));
			  }
			}
			""");
	}

	@Test
	public void testBreakInForLoopTranslatedToJavaFor() throws Exception {
		checkCompilation(jbaseInputs.breakInForLoopTranslatedToJavaFor(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    for (int i = 0; (i < argsNum); i++) {
			      if ((argsNum > 0)) {
			        break;
			      } else {
			        System.out.println("");
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testBreakInForLoopTranslatedToJavaWhile() throws Exception {
		checkCompilation(jbaseInputs.breakInForLoopTranslatedToJavaWhile(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        if ((argsNum > 0)) {
			          break;
			        } else {
			          System.out.println("");
			        }
			        int _i = i;
			        i = (_i + 1);
			        _while = (i < argsNum);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testBreakInBothIfBranchesInForLoopTranslatedToJavaWhile() throws Exception {
		checkCompilation(jbaseInputs.breakInBothIfBranchesInForLoopTranslatedToJavaWhile(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        if ((argsNum > 0)) {
			          break;
			        } else {
			          break;
			        }
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testBreakSingleInForLoopTranslatedToJavaWhile() throws Exception {
		checkCompilation(jbaseInputs.breakSingleInForLoopTranslatedToJavaWhile(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    {
			      int i = 0;
			      boolean _while = (i < argsNum);
			      while (_while) {
			        break;
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testBreakInWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.breakInWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    while ((i < argsNum)) {
			      if ((argsNum > 0)) {
			        break;
			      } else {
			        System.out.println("");
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testBreakInBothIfBranchesInWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.breakInBothIfBranchesInWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    while ((i < argsNum)) {
			      if ((argsNum > 0)) {
			        break;
			      } else {
			        break;
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testBreakInDoWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.breakInDoWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    do {
			      if ((argsNum > 0)) {
			        break;
			      } else {
			        System.out.println("");
			      }
			    } while((i < argsNum));
			  }
			}
			""");
	}

	@Test
	public void testBreakInBothIfBranchesInDoWhileLoop() throws Exception {
		checkCompilation(jbaseInputs.breakInBothIfBranchesInDoWhileLoop(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    do {
			      if ((argsNum > 0)) {
			        break;
			      } else {
			        break;
			      }
			    } while((i < argsNum));
			  }
			}
			""");
	}

	@Test
	public void testEmptySwitchStatement() throws Exception {
		checkCompilation(jbaseInputs.emptySwitchStatement(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    switch (argsNum) {
			    }
			  }
			}
			""");
	}

	@Test
	public void testSwitchStatementWithCaseAndDefault() throws Exception {
		checkCompilation(jbaseInputs.switchStatementWithCaseAndDefault(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    switch (argsNum) {
			      case 0:
			        System.out.println("0");
			      default:
			        System.out.println("default");
			    }
			  }
			}
			""");
	}

	@Test
	public void testSwitchStatementWithCaseAndDefaultMultipleStatements() throws Exception {
		checkCompilation(jbaseInputs.switchStatementWithCaseAndDefaultMultipleStatements(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    switch (argsNum) {
			      case 0:
			        i = 0;
			        System.out.println("0");
			      default:
			        {
			          i = -1;
			          System.out.println("default");
			        }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSwitchStatementWithBreak() throws Exception {
		checkCompilation(jbaseInputs.switchStatementWithBreak(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    int i = 0;
			    switch (argsNum) {
			      case 0:
			        i = 0;
			        System.out.println("0");
			        break;
			      default:
			        {
			          i = -1;
			          System.out.println("default");
			          break;
			        }
			    }
			  }
			}
			""");
	}

	/**
	 * This would work only for Java 1.7 so for the moment we'll not deal with that
	 */
	// @Test
	public void testSwitchStatementWithStrings() throws Exception {
		checkCompilation(jbaseInputs.switchStatementWithStrings(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    String arg = args[0];
			    int i = 0;
			    switch (arg) {
			      case "first":
			        i = 0;
			        System.out.println("0");
			        break;
			      default:
			        {
			          i = (-1);
			          System.out.println("default");
			          break;
			        }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSwitchStatementWithChars() throws Exception {
		checkCompilation(jbaseInputs.switchStatementWithChars(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int argsNum = args.length;
			    String firstArg = args[0];
			    char arg = firstArg.toCharArray()[0];
			    int i = 0;
			    switch (arg) {
			      case 'f':
			        i = 0;
			        System.out.println("0");
			        break;
			      default:
			        {
			          i = -1;
			          System.out.println("default");
			          break;
			        }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSwitchStatementWithBytes() throws Exception {
		checkCompilation(jbaseInputs.switchStatementWithBytes(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    byte b = 0;
			    switch (b) {
			      case 10:
			        System.out.println("10");
			      case 'f':
			        System.out.println("f");
			        break;
			      default:
			        {
			          System.out.println("default");
			          break;
			        }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSwitchStatementReturnType() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.switchStatementReturnType(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int move(int p) {
			    switch (p) {
			      case 0:
			        return 2;
			      case 1:
			        return 1;
			      case 2:
			        return 1;
			      case 3:
			        return 2;
			      case 4:
			        return 1;
			      default:
			        return -1;
			    }
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testSwitchStatementReturnTypeWithFallback() throws Exception {
		checkCompilation("""
			op move(int p) : int {
				switch (p) {
					case 0: System.out.println("0"); // the default is executed
					default: return -1;
				}
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int move(int p) {
			    switch (p) {
			      case 0:
			        System.out.println("0");
			      default:
			        return -1;
			    }
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testVarNameSameAsMethodName() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.varNameSameAsMethodName(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int numOfDigits(int num) {
			    int numOfDigits = 1;
			    while (((num / 10) > 0)) {
			      {
			        numOfDigits = (numOfDigits + 1);
			        num = (num / 10);
			      }
			    }
			    return numOfDigits;
			  }

			  public static void main(String[] args) throws Throwable {
			    int _numOfDigits = MyFile.numOfDigits(3456);
			    String _plus = ("numOfDigits(3456): " + Integer.valueOf(_numOfDigits));
			    System.out.println(_plus);
			  }
			}
			""");
	}

	@Test
	public void testCharTranslatedToJavaChar() throws Exception {
		checkCompilation("""
			char c1 = 'c';
			char c2 = '\\n';
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    char c1 = 'c';
			    char c2 = '\\n';
			  }
			}
			""");
	}

	@Test
	public void testEqualsTranslation() throws Exception {
		checkCompilation("""
			boolean b;
			b = 1 == 2;
			b = 1 != 2;
			b = "a" == "b";
			b = "a" != "b";

			String a = new String("a");
			System.out.println("a" == "a"); // true
			System.out.println(a == "a"); // false
			System.out.println("a".equals("a")); // true
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    boolean b = false;
			    b = (1 == 2);
			    b = (1 != 2);
			    b = ("a" == "b");
			    b = ("a" != "b");
			    String a = new String("a");
			    System.out.println(("a" == "a"));
			    System.out.println((a == "a"));
			    System.out.println("a".equals("a"));
			  }
			}
			""");
	}

	@Test
	public void testSeveralVariableDeclarations() throws Exception {
		checkCompilation("""
			int i = 0, j = 1, k = 0;
			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int j = 1;
			    int k = 0;
			    System.out.println(i);
			    System.out.println(j);
			    System.out.println(k);
			  }
			}
			""");
	}

	@Test
	public void testSeveralVariableDeclarationsFinal() throws Exception {
		checkCompilation("""
			final int i = 0, j = 1, k = 2;
			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    final int i = 0;
			    final int j = 1;
			    final int k = 2;
			    System.out.println(i);
			    System.out.println(j);
			    System.out.println(k);
			  }
			}
			""");
	}

	@Test
	public void testSeveralVariableDeclarationsInForLoop() throws Exception {
		checkCompilation("""
			for (int i = 0, j = 1, k = 0; i < 0; i++) {
				System.out.println(i);
				System.out.println(j);
				System.out.println(k);
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    for (int i = 0, j = 1, k = 0; (i < 0); i++) {
			      {
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(k);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSeveralVariableDeclarationsInForLoopWithoutVariableInitialization()
			throws Exception {
		// we want to check the compiler when there's no initialization for variables
		// (which Xbase can handle)
		// so we must disable the check for initialized variables
		validator.setSkipVariableInitializationCheck(true);
		checkCompilation("""
			for (int i, j = 1, k; i < 0; i++) {
				System.out.println(i);
				System.out.println(j);
				System.out.println(k);
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    for (int i = 0, j = 1, k = 0; (i < 0); i++) {
			      {
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(k);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSeveralUpdatesInForLoop() throws Exception {
		checkCompilation("""
			for (int i = 0, j = 1; i < 0; i++, j++) {
				System.out.println(i);
				System.out.println(j);
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    for (int i = 0, j = 1; (i < 0); i++, j++) {
			      {
			        System.out.println(i);
			        System.out.println(j);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSeveralVariableDeclarationsInForLoopTranslatedToJavaWhile() throws Exception {
		checkCompilation("""
			for (int i = 0, j = 1, k = 0; i < 0; i += 1) {
				System.out.println(i);
				System.out.println(j);
				System.out.println(k);
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int j = 1;
			    int k = 0;
			    boolean _while = (i < 0);
			    while (_while) {
			      {
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(k);
			      }
			      int _i = i;
			      i = (_i + 1);
			      _while = (i < 0);
			    }
			  }
			}
			""");
	}

	@Test
	public void testSeveralAssignmentsInForLoop() throws Exception {
		checkCompilation("""
			int i;
			int j;
			int k;
			for (i = 0, j = 1, k = 1; i < 0; i++) {
				System.out.println(i);
				System.out.println(j);
				System.out.println(k);
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int j = 0;
			    int k = 0;
			    for (i = 0, j = 1, k = 1; (i < 0); i++) {
			      {
			        System.out.println(i);
			        System.out.println(j);
			        System.out.println(k);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testSpecialOperators() throws Exception {
		checkCompilation("""
			int i = 2;
			int j = 3;
			int k = 4;

			i *= j;
			i /= j;
			i %= j;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 2;
			    int j = 3;
			    int k = 4;
			    int _i = i;
			    i = (_i * j);
			    int _i_1 = i;
			    i = (_i_1 / j);
			    int _i_2 = i;
			    i = (_i_2 % j);
			  }
			}
			""");
	}

	@Test
	public void testNumberLiterals() throws Exception {
		checkCompilation(jbaseInputs.numberLiterals(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    byte b = 100;
			    short s = 1000;
			    char c = 1000;
			  }
			}
			""");
	}

	@Test
	public void testNumberLiteralsInBinaryOperations() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/34
		checkCompilation("""
			System.out.println(1 + 128);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    System.out.println((1 + 128));
			  }
			}
			""");
	}

	@Test
	public void testUnaryOperationInBinaryOperations() throws Exception {
		checkCompilation("""
			System.out.println(1 + -128);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    System.out.println((1 + -128));
			  }
			}
			""");
	}

	@Test
	public void testNumberLiteralsInUnaryOperations() throws Exception {
		// https://github.com/LorenzoBettini/javamm/issues/53
		checkCompilation("""
			byte b = -1;
			short s = -1;
			short s2 = -10000;
			short s3 = -(+10000);
			short s4 = +(-(+10000));
			//short s5 = 1 + -128; // this does not work yet
			char c2 = +1; // OK
			System.out.println(-1);
			System.out.println(+1);
			System.out.println(-(+1));
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    byte b = -1;
			    short s = -1;
			    short s2 = -10000;
			    short s3 = -(+10000);
			    short s4 = +(-(+10000));
			    char c2 = +1;
			    System.out.println(-1);
			    System.out.println(+1);
			    System.out.println(-(+1));
			  }
			}
			""");
	}

	@Test
	public void testCharLiterals() throws Exception {
		checkCompilation(jbaseInputs.charLiterals(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    byte b = 'c';
			    short s = 'c';
			    char c = 'c';
			    int i = 'c';
			    long l = 'c';
			    double d = 'c';
			    float f = 'c';
			  }
			}
			""");
	}

	@Test
	public void testCastedExpression() throws Exception {
		checkCompilation("""
			op m(char c) : int { return 0; }

			int i;
			char c = 'c';
			i = (int) c;
			char r1 = (char) (int) c;
			char r2 = (char) m((char) 0);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static int m(char c) {
			    return 0;
			  }

			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    char c = 'c';
			    i = ((int) c);
			    char r1 = ((char) ((int) c));
			    int _m = MyFile.m(((char) 0));
			    char r2 = ((char) _m);
			  }
			}
			""");
	}

	@Test
	public void testCastPrecedenceOverDivision() throws Exception {
		// https://github.com/LorenzoBettini/jbase/issues/145
		checkCompilation("""
			int somma = 15;
			int numero = 6;
			double media = (double) somma / numero;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int somma = 15;
			    int numero = 6;
			    double media = (((double) somma) / numero);
			  }
			}
			""");
	}

	@Test
	public void testCastPrecedenceExecution() throws Exception {
		assertExecuteMain("""
			op testMe() : void {
				int somma = 15;
				int numero = 6;
				System.out.println((double) somma / numero);
				System.out.println((double) (somma / numero));
				System.out.println((double) -somma / numero);
				System.out.println((double) +15 / numero);
				System.out.println((double) (int) 15.9 / numero);
				System.out.println(2 * (int) 2.9 + 1);
				System.out.println((int) ~somma + numero);
				System.out.println((boolean) !false && false);
				System.out.println((double) somma++ / numero);
			}
			""", """
			2.5
			2.0
			-2.5
			2.5
			2.5
			5
			-10
			false
			2.5
			""");
	}

	@Test
	public void testConditionalExpression() throws Exception {
		checkCompilation("""
			int i = 0;
			i = i > 0 ? 1 : 2;
			int j = i > 0 ? 1 : 2;
			Object o = i < 0 ? 1 : "a";
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int _xjconditionalexpression = (int) 0;
			    if ((i > 0)) {
			      _xjconditionalexpression = 1;
			    } else {
			      _xjconditionalexpression = 2;
			    }
			    i = _xjconditionalexpression;
			    int _xjconditionalexpression_1 = (int) 0;
			    if ((i > 0)) {
			      _xjconditionalexpression_1 = 1;
			    } else {
			      _xjconditionalexpression_1 = 2;
			    }
			    int j = _xjconditionalexpression_1;
			    Object _xjconditionalexpression_2 = null;
			    if ((i < 0)) {
			      _xjconditionalexpression_2 = Integer.valueOf(1);
			    } else {
			      _xjconditionalexpression_2 = "a";
			    }
			    Object o = _xjconditionalexpression_2;
			  }
			}
			""");
	}

	@Test
	public void testConditionalExpression2() throws Exception {
		checkCompilation("""
			int i = 0;
			int n = 0;
			if((i=(i==0?5:7)) > 0) n = 4;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int n = 0;
			    int _xjconditionalexpression = (int) 0;
			    if ((i == 0)) {
			      _xjconditionalexpression = 5;
			    } else {
			      _xjconditionalexpression = 7;
			    }
			    int _i = (i = _xjconditionalexpression);
			    boolean _greaterThan = (_i > 0);
			    if (_greaterThan) {
			      n = 4;
			    }
			  }
			}
			""");
	}

	@Test
	public void testVectorWithQualifiedName() throws Exception {
		checkCompilation("""
			java.util.Vector v = new java.util.Vector();
			""", """
			package jbasetestlanguage;

			import java.util.Vector;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Vector v = new Vector<Object>();
			  }
			}
			""");
	}

	@Test
	public void testVectorWithQualifiedNameAndGeneric() throws Exception {
		checkCompilation("""
			java.util.Vector<Object> v = new java.util.Vector();
			""", """
			package jbasetestlanguage;

			import java.util.Vector;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Vector<Object> v = new Vector<Object>();
			  }
			}
			""");
	}

	@Test
	public void testVectorWithQualifiedNameAndGenericInConstructor() throws Exception {
		checkCompilation("""
			java.util.Vector<String> v = new java.util.Vector<String>();
			""", """
			package jbasetestlanguage;

			import java.util.Vector;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Vector<String> v = new Vector<String>();
			  }
			}
			""");
	}

	@Test
	public void testWildcardExtends() throws Exception {
		checkCompilation("""
			java.util.Vector<? extends String> v = new java.util.Vector<String>();
			System.out.println(v.get(0)); // read is allowed
			""", """
			package jbasetestlanguage;

			import java.util.Vector;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Vector<? extends String> v = new Vector<String>();
			    System.out.println(v.get(0));
			  }
			}
			""");
	}

	@Test
	public void testWildcardSuper() throws Exception {
		checkCompilation("""
			java.util.Vector<? super String> v = new java.util.Vector<String>();
			v.add("s"); // write is allowed
			""", """
			package jbasetestlanguage;

			import java.util.Vector;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Vector<? super String> v = new Vector<String>();
			    v.add("s");
			  }
			}
			""");
	}

	@Test
	public void testImports() throws Exception {
		checkCompilation("""
			import java.util.List;
			import java.util.LinkedList;
			import java.util.ArrayList;

			List l1 = new LinkedList();
			List l2 = new ArrayList();
			""", """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.LinkedList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List l1 = new LinkedList<Object>();
			    List l2 = new ArrayList<Object>();
			  }
			}
			""");
	}

	@Test
	public void testFinalVariable() throws Exception {
		checkCompilation("""
			final int i = 0;
			System.out.println(i);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    final int i = 0;
			    System.out.println(i);
			  }
			}
			""");
	}

	@Test
	public void testForEachLoop() throws Exception {
		checkCompilation("""
			import java.util.List;
			import java.util.ArrayList;

			List<String> strings = new ArrayList<String>();
			strings.add("first");
			strings.add("second");

			for (String s : strings)
				System.out.println(s);
			""", """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<String> strings = new ArrayList<String>();
			    strings.add("first");
			    strings.add("second");
			    for (String s : strings) {
			      System.out.println(s);
			    }
			  }
			}
			""");
	}

	@Test
	public void testForEachLoopBlock() throws Exception {
		checkCompilation("""
			import java.util.List;
			import java.util.ArrayList;

			List<String> strings = new ArrayList<String>();
			strings.add("first");
			strings.add("second");

			for (String s : strings) {
				System.out.println(s);
			}
			""", """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<String> strings = new ArrayList<String>();
			    strings.add("first");
			    strings.add("second");
			    for (String s : strings) {
			      System.out.println(s);
			    }
			  }
			}
			""");
	}

	@Test
	public void testForEachLoopWithFinalParam() throws Exception {
		checkCompilation("""
			import java.util.List;
			import java.util.ArrayList;

			List<String> strings = new ArrayList<String>();
			strings.add("first");
			strings.add("second");

			for (final String s : strings)
				System.out.println(s);
			""", """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    List<String> strings = new ArrayList<String>();
			    strings.add("first");
			    strings.add("second");
			    for (final String s : strings) {
			      System.out.println(s);
			    }
			  }
			}
			""");
	}

	@Test
	public void testInstanceOf() throws Exception {
		checkCompilation("""
			import java.util.Collection;
			import java.util.List;
			import java.util.ArrayList;

			Collection<String> strings = new ArrayList<String>();
			if (strings instanceof List) {
				// get is in List but not in Collection
				System.out.println(((List)strings).get(0));
			}
			""", """
			package jbasetestlanguage;

			import java.util.ArrayList;
			import java.util.Collection;
			import java.util.List;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Collection<String> strings = new ArrayList<String>();
			    if ((strings instanceof List)) {
			      System.out.println(((List) strings).get(0));
			    }
			  }
			}
			""");
	}

	@Test
	public void testAssignmentToJvmOperationReference() throws Exception {
		checkCompilation("""
			op m(String s) : String { return null; }
			m = "a";
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static String m(String s) {
			    return null;
			  }

			  public static void main(String[] args) throws Throwable {
			    MyFile.m("a");
			  }
			}
			""");
	}

	@Test
	public void testAssignmentToFieldReference() throws Exception {
		checkCompilation("""
			aString : String
			method m(String s) : void {
				aString = s;
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  private String aString;

			  public void m(String s) {
			    this.aString = s;
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAssignmentToAssignment() throws Exception {
		checkCompilation("""
			int i, j;
			i = j = 0;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int j = 0;
			    i = (j = 0);
			  }
			}
			""");
	}

	@Test
	public void testAssignmentToAssignment2() throws Exception {
		checkCompilation("""
			int i = 0;
			int j = 0;
			int n = 0;
			n=i=(j==0 ? 1 : 2);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int j = 0;
			    int n = 0;
			    int _xjconditionalexpression = (int) 0;
			    if ((j == 0)) {
			      _xjconditionalexpression = 1;
			    } else {
			      _xjconditionalexpression = 2;
			    }
			    int _i = (i = _xjconditionalexpression);
			    n = _i;
			  }
			}
			""");
	}

	@Test
	public void testAssignmentAsCallArgument() throws Exception {
		checkCompilation("""
			aString : String
			method m(String s) : void {
				m(aString = s);
				this.m(aString = s);
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  private String aString;

			  public void m(String s) {
			    this.m(this.aString = s);
			    this.m(this.aString = s);
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAssignmentAsCallArgument2() throws Exception {
		checkCompilation("""
			int i = 0;
			Math.max( i = i + 1, i == 1 ? 1 : 2);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int i = 0;
			    int _i = i = (i + 1);
			    int _xjconditionalexpression = (int) 0;
			    if ((i == 1)) {
			      _xjconditionalexpression = 1;
			    } else {
			      _xjconditionalexpression = 2;
			    }
			    Math.max(_i, _xjconditionalexpression);
			  }
			}
			""");
	}

	@Test
	public void testThrow() throws Exception {
		checkCompilation("""
			method m(String s) : void {
				throw new Exception("test");
			}
			""", """
			package jbasetestlanguage;

			import org.eclipse.xtext.xbase.lib.Exceptions;

			@SuppressWarnings("all")
			public class MyFile {
			  public void m(String s) {
			    try {
			      throw new Exception("test");
			    } catch (Throwable _e) {
			      throw Exceptions.sneakyThrow(_e);
			    }
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testTryCatch() throws Exception {
		checkCompilation("""
			try {
				int i = 0;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			""", """
			package jbasetestlanguage;

			import org.eclipse.xtext.xbase.lib.Exceptions;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    try {
			      int i = 0;
			    } catch (final Throwable _t) {
			      if (_t instanceof Exception) {
			        final Exception e = (Exception)_t;
			        e.printStackTrace();
			      } else if (_t instanceof Throwable) {
			        final Throwable t = (Throwable)_t;
			        t.printStackTrace();
			      } else {
			        throw Exceptions.sneakyThrow(_t);
			      }
			    }
			  }
			}
			""");
	}

	@Test
	public void testTryCatchFinally() throws Exception {
		checkCompilation("""
			try {
				int i = 0;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				System.out.println("finally");
			}
			""", """
			package jbasetestlanguage;

			import org.eclipse.xtext.xbase.lib.Exceptions;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    try {
			      int i = 0;
			    } catch (final Throwable _t) {
			      if (_t instanceof Exception) {
			        final Exception e = (Exception)_t;
			        e.printStackTrace();
			      } else {
			        throw Exceptions.sneakyThrow(_t);
			      }
			    } finally {
			      System.out.println("finally");
			    }
			  }
			}
			""");
	}

	@Test
	public void testTryFinally() throws Exception {
		checkCompilation("""
			try {
				int i = 0;
			} finally {
				System.out.println("finally");
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    try {
			      int i = 0;
			    } finally {
			      System.out.println("finally");
			    }
			  }
			}
			""");
	}

	@Test
	public void testTryWithResources() throws Exception {
		checkCompilation("""
			import java.io.FileReader;

			try (FileReader fr1 = new FileReader("");
				FileReader fr2 = new FileReader(fr1.toString()+fr1.toString());)
			{
				int i = 0;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				System.out.println("finally");
			}
			""", """
			package jbasetestlanguage;

			import java.io.FileReader;
			import org.eclipse.xtext.xbase.lib.Exceptions;
			import org.eclipse.xtext.xbase.lib.Functions.Function0;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    try (final FileReader fr1 = new Function0<FileReader>() {
			      @Override
			      public FileReader apply() {
			        try {
			          return new FileReader("");
			        } catch (Throwable _e) {
			          throw Exceptions.sneakyThrow(_e);
			        }
			      }
			    }.apply(); final FileReader fr2 = new Function0<FileReader>() {
			      @Override
			      public FileReader apply() {
			        try {
			          return new FileReader((fr1.toString() + fr1.toString()));
			        } catch (Throwable _e) {
			          throw Exceptions.sneakyThrow(_e);
			        }
			      }
			    }.apply()) {
			      int i = 0;
			    } catch (final Throwable _t) {
			      if (_t instanceof Exception) {
			        final Exception e = (Exception)_t;
			        e.printStackTrace();
			      } else {
			        throw Exceptions.sneakyThrow(_t);
			      }
			    } finally {
			      System.out.println("finally");
			    }
			  }
			}
			""");
	}

	@Test
	public void testTryWithExistingVariableResources() throws Exception {
		compilationTestHelper.compile("""
			var input = new java.io.ByteArrayInputStream(new byte[0]);
			try (input) {}
			""", result -> {
				assertNoValidationErrors(result);
				assertTrue(result.getSingleGeneratedCode(),
					result.getSingleGeneratedCode().contains("try (input)"));
				assertGeneratedJavaCodeCompiles(result);
			});

		compilationTestHelper.compile("""
			var input = new java.io.ByteArrayInputStream(new byte[0]);
			try (input; var second = new java.io.ByteArrayInputStream(new byte[0])) {}
			""", result -> {
				assertNoValidationErrors(result);
				final var generated = result.getSingleGeneratedCode();
				assertTrue(generated, generated.contains("try (input;"));
				assertTrue(generated, generated.indexOf("second = new ByteArrayInputStream")
					> generated.indexOf("try (input;"));
				assertGeneratedJavaCodeCompiles(result);
			});

		compilationTestHelper.compile("""
			import static java.lang.System.in;
			try (in) {}
			""", result -> {
				assertNoValidationErrors(result);
				assertGeneratedJavaCodeCompiles(result);
			});

		compilationTestHelper.compile("""
			var input = new java.io.StringReader("a");
			try (input) {
				input.read();
			} catch (java.io.IOException e) {
				throw new RuntimeException(e);
			} finally {}
			""", result -> {
				assertNoValidationErrors(result);
				assertGeneratedJavaCodeCompiles(result);
			});
	}

	@Test
	public void testSynchronized() throws Exception {
		checkCompilation("""
			Object o = null;
			synchronized (o) {
				o.wait();
			}
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Object o = null;
			    synchronized (o) {
			      o.wait();
			    }
			  }
			}
			""");
	}

	@Test
	public void testClassObjectImported() throws Exception {
		checkCompilation("""
			import java.util.Date;

			System.out.println(Date.class);
			""", """
			package jbasetestlanguage;

			import java.util.Date;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    System.out.println(Date.class);
			  }
			}
			""");
	}

	@Test
	public void testClassObjectInArray() throws Exception {
		checkCompilation("""
			import java.util.Date;

			Class<?>[] classes = {String.class, Date.class};
			""", """
			package jbasetestlanguage;

			import java.util.Date;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    Class<?>[] classes = { String.class, Date.class };
			  }
			}
			""");
	}

	@Test
	public void testClassObject() throws Exception {
		checkCompilation("""
			System.out.println(String.class);
			Class<String> c = String.class;
			String name = String.class.getName();
			System.out.println(String[].class);
			Class<String[]> c1 = String[].class;
			String name1 = String[].class.getName();
			// raw types for variables
			Class c3 = String[].class;
			Class<?> c4 = String[].class;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    System.out.println(String.class);
			    Class<String> c = String.class;
			    String name = String.class.getName();
			    System.out.println(String[].class);
			    Class<String[]> c1 = String[].class;
			    String name1 = String[].class.getName();
			    Class c3 = String[].class;
			    Class<?> c4 = String[].class;
			  }
			}
			""");
	}

	@Test
	public void testBitwiseOperators() throws Exception {
		checkCompilation("""
			int result;
			int i = 1, j = 2;
			result = i & j;
			System.out.println(i & j);
			result = i | j;
			System.out.println(i | j);
			result = i ^ j;
			System.out.println(i ^ j);
			result = ~j;
			System.out.println(~j);

			System.out.println(i << j);
			System.out.println(i >> j);
			System.out.println(i >>> j);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int result = 0;
			    int i = 1;
			    int j = 2;
			    result = (i & j);
			    System.out.println((i & j));
			    result = (i | j);
			    System.out.println((i | j));
			    result = (i ^ j);
			    System.out.println((i ^ j));
			    int _bitwiseNot = (~j);
			    result = _bitwiseNot;
			    int _bitwiseNot_1 = (~j);
			    System.out.println(_bitwiseNot_1);
			    System.out.println((i << j));
			    System.out.println((i >> j));
			    System.out.println((i >>> j));
			  }
			}
			""");
	}

	@Test
	public void testBitwiseOperatorsOnLong() throws Exception {
		checkCompilation("""
			long result;
			long i = 1, j = 2;
			result = i & j;
			System.out.println(i & j);
			result = i | j;
			System.out.println(i | j);
			result = i ^ j;
			System.out.println(i ^ j);
			result = ~j;
			System.out.println(~j);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    long result = 0;
			    long i = 1;
			    long j = 2;
			    result = (i & j);
			    System.out.println((i & j));
			    result = (i | j);
			    System.out.println((i | j));
			    result = (i ^ j);
			    System.out.println((i ^ j));
			    long _bitwiseNot = (~j);
			    result = _bitwiseNot;
			    long _bitwiseNot_1 = (~j);
			    System.out.println(_bitwiseNot_1);
			  }
			}
			""");
	}

	@Test
	public void testBitwiseOperatorsMultiAssign() throws Exception {
		checkCompilation("""
			int result;
			int i = 1, j = 2;
			i &= j;
			System.out.println(i &= j);
			i |= j;
			System.out.println(i |= j);
			i ^= j;
			System.out.println(i ^= j);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int result = 0;
			    int i = 1;
			    int j = 2;
			    int _i = i;
			    i = (_i & j);
			    int _i_1 = i;
			    int _bitwiseAnd = i = (_i_1 & j);
			    System.out.println(_bitwiseAnd);
			    int _i_2 = i;
			    i = (_i_2 | j);
			    int _i_3 = i;
			    int _bitwiseOr = i = (_i_3 | j);
			    System.out.println(_bitwiseOr);
			    int _i_4 = i;
			    i = (_i_4 ^ j);
			    int _i_5 = i;
			    int _bitwiseXor = i = (_i_5 ^ j);
			    System.out.println(_bitwiseXor);
			  }
			}
			""");
	}

	@Test
	public void testBitwiseOperatorsMultiAssignOnBooleans() throws Exception {
		checkCompilation("""
			boolean b = true, c = false;
			b &= c;
			System.out.println(b &= c);
			b |= c;
			System.out.println(b |= c);
			b ^= c;
			System.out.println(b ^= c);
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    boolean b = true;
			    boolean c = false;
			    boolean _b = b;
			    b = (_b & c);
			    boolean _b_1 = b;
			    boolean _bitwiseAnd = b = (_b_1 & c);
			    System.out.println(_bitwiseAnd);
			    boolean _b_2 = b;
			    b = (_b_2 | c);
			    boolean _b_3 = b;
			    boolean _bitwiseOr = b = (_b_3 | c);
			    System.out.println(_bitwiseOr);
			    boolean _b_4 = b;
			    b = (_b_4 ^ c);
			    boolean _b_5 = b;
			    boolean _bitwiseXor = b = (_b_5 ^ c);
			    System.out.println(_bitwiseXor);
			  }
			}
			""");
	}

	@Test
	public void testBitwiseOperatorsOnChars() throws Exception {
		checkCompilation("""
			System.out.println('a' & 'b');
			System.out.println('a' | 'b');
			System.out.println('a' ^ 'b');
			System.out.println(~'b');
			System.out.println('a' << 'b');
			System.out.println('a' >> 'b');
			System.out.println('a' >>> 'b');
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    System.out.println(('a' & 'b'));
			    System.out.println(('a' | 'b'));
			    System.out.println(('a' ^ 'b'));
			    int _bitwiseNot = (~'b');
			    System.out.println(_bitwiseNot);
			    System.out.println(('a' << 'b'));
			    System.out.println(('a' >> 'b'));
			    System.out.println(('a' >>> 'b'));
			  }
			}
			""");
	}

	@Test
	public void testAnnotation() throws Exception {
		checkCompilation("""
			import com.google.inject.Inject;

			@Inject
			o : Object
			""", """
			package jbasetestlanguage;

			import com.google.inject.Inject;

			@SuppressWarnings("all")
			public class MyFile {
			  @Inject
			  private Object o;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAnnotation2() throws Exception {
		checkCompilation("""
			import jbase.tests.util.ExampleAnnotation;
			import org.eclipse.xtext.xbase.testing.typesystem.TypeSystemSmokeTester;

			@ExampleAnnotation(value = TypeSystemSmokeTester.class)
			o : Object
			""", """
			package jbasetestlanguage;

			import jbase.tests.util.ExampleAnnotation;
			import org.eclipse.xtext.xbase.testing.typesystem.TypeSystemSmokeTester;

			@SuppressWarnings("all")
			public class MyFile {
			  @ExampleAnnotation(value = TypeSystemSmokeTester.class)
			  private Object o;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePair() throws Exception {
		checkCompilation("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3(value = String.class)
			o : Object
			""", """
			package jbasetestlanguage;

			import jbase.tests.util.ExampleAnnotation3;

			@SuppressWarnings("all")
			public class MyFile {
			  @ExampleAnnotation3(value = String.class)
			  private Object o;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePairAndMultiple()
			throws Exception {
		checkCompilation("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3(value = { String.class, Integer.class })
			o : Object
			""", """
			package jbasetestlanguage;

			import jbase.tests.util.ExampleAnnotation3;

			@SuppressWarnings("all")
			public class MyFile {
			  @ExampleAnnotation3(value = { String.class, Integer.class })
			  private Object o;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesAndSingleValue() throws Exception {
		checkCompilation("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3(String.class)
			o : Object
			""", """
			package jbasetestlanguage;

			import jbase.tests.util.ExampleAnnotation3;

			@SuppressWarnings("all")
			public class MyFile {
			  @ExampleAnnotation3(String.class)
			  private Object o;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testAnnotationsWithExpectedMultipleValuesArrayLiteral() throws Exception {
		checkCompilation("""
			import jbase.tests.util.ExampleAnnotation3;

			@ExampleAnnotation3({String.class, Integer.class})
			o : Object
			""", """
			package jbasetestlanguage;

			import jbase.tests.util.ExampleAnnotation3;

			@SuppressWarnings("all")
			public class MyFile {
			  @ExampleAnnotation3({ String.class, Integer.class })
			  private Object o;

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testLoopsWithConditionAlwaysTrue() throws Exception {
		checkCompilation("""
			int d = 1;
			while (1 == 1) {
				d++;
				if (d == 3)
					break;
			}
			d = 0;

			do {
				d++;
				if (d == 3)
					break;
			} while (1 == 1);
			d = 0;

			for (;;) {
				d++;
				if (d == 3)
					break;
			}
			d = 0;
			""", """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void main(String[] args) throws Throwable {
			    int d = 1;
			    while ((1 == 1)) {
			      {
			        d++;
			        if ((d == 3)) {
			          break;
			        }
			      }
			    }
			    d = 0;
			    do {
			      {
			        d++;
			        if ((d == 3)) {
			          break;
			        }
			      }
			    } while((1 == 1));
			    d = 0;
			    for (;;) {
			      {
			        d++;
			        if ((d == 3)) {
			          break;
			        }
			      }
			    }
			    d = 0;
			  }
			}
			""");
	}

	@Test
	public void testShortCircuit() throws Exception {
		final var toStringValue = """
			op mTrue() : boolean {
				System.out.println("mTrue called");
				return true;
			}

			op mFalse() : boolean {
				System.out.println("mFalse called");
				return false;
			}

			op testMe() : void {
				System.out.println("mFalse() && mTrue() = " + (mFalse() && mTrue()));
			}
			""";
		checkCompilation(toStringValue, """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static boolean mTrue() {
			    System.out.println("mTrue called");
			    return true;
			  }

			  public static boolean mFalse() {
			    System.out.println("mFalse called");
			    return false;
			  }

			  public static void testMe() {
			    String _plus = ("mFalse() && mTrue() = " + Boolean.valueOf((MyFile.mFalse() && MyFile.mTrue())));
			    System.out.println(_plus);
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
		assertExecuteMain(toStringValue, """
			mFalse called
			mFalse() && mTrue() = false
			""");
	}

	@Test
	public void testNotShortCircuitBoolean() throws Exception {
		final var toStringValue = """
			op mTrue() : boolean {
				System.out.println("mTrue called");
				return true;
			}

			op mFalse() : boolean {
				System.out.println("mFalse called");
				return false;
			}

			op testMe() : void {
				System.out.println("mFalse() ^ mTrue() = " + (mFalse() ^ mTrue()));
				System.out.println("mFalse() & mTrue() = " + (mFalse() & mTrue()));
				System.out.println("mTrue() | mFalse() = " + (mTrue() | mFalse()));
			}
			""";
		checkCompilation(toStringValue, """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static boolean mTrue() {
			    System.out.println("mTrue called");
			    return true;
			  }

			  public static boolean mFalse() {
			    System.out.println("mFalse called");
			    return false;
			  }

			  public static void testMe() {
			    boolean _mFalse = MyFile.mFalse();
			    boolean _mTrue = MyFile.mTrue();
			    boolean _bitwiseXor = (_mFalse ^ _mTrue);
			    String _plus = ("mFalse() ^ mTrue() = " + Boolean.valueOf(_bitwiseXor));
			    System.out.println(_plus);
			    boolean _mFalse_1 = MyFile.mFalse();
			    boolean _mTrue_1 = MyFile.mTrue();
			    boolean _bitwiseAnd = (_mFalse_1 & _mTrue_1);
			    String _plus_1 = ("mFalse() & mTrue() = " + Boolean.valueOf(_bitwiseAnd));
			    System.out.println(_plus_1);
			    boolean _mTrue_2 = MyFile.mTrue();
			    boolean _mFalse_2 = MyFile.mFalse();
			    boolean _bitwiseOr = (_mTrue_2 | _mFalse_2);
			    String _plus_2 = ("mTrue() | mFalse() = " + Boolean.valueOf(_bitwiseOr));
			    System.out.println(_plus_2);
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
		assertExecuteMain(toStringValue, """
			mFalse called
			mTrue called
			mFalse() ^ mTrue() = true
			mFalse called
			mTrue called
			mFalse() & mTrue() = false
			mTrue called
			mFalse called
			mTrue() | mFalse() = true
			""");
	}

	@Test
	public void testBubbleSort() throws Exception {
		checkCompilation(jbaseTestlanguageInputs.bubbleSort(), """
			package jbasetestlanguage;

			@SuppressWarnings("all")
			public class MyFile {
			  public static void bubbleSort(int[] array) {
			    boolean swapped = true;
			    int j = 0;
			    int tmp = 0;
			    while (swapped) {
			      {
			        swapped = false;
			        j = (j + 1);
			        for (int i = 0; (i < (array.length - j)); i++) {
			          boolean _greaterThan = (array[i] > array[(i + 1)]);
			          if (_greaterThan) {
			            tmp = array[i];
			            array[i] = array[(i + 1)];
			            array[(i + 1)] = tmp;
			            swapped = true;
			          }
			        }
			      }
			    }
			  }

			  public static void main(String[] args) throws Throwable {
			  }
			}
			""");
	}

	@Test
	public void testSudoku() throws Exception {
		assertExecuteMain(jbaseTestlanguageInputs.sudoku(), """
			4 0 0 0\s
			0 0 0 3\s
			0 1 3 0\s
			0 0 0 2\s
			true
			4 3 2 1\s
			1 2 4 3\s
			2 1 3 4\s
			3 4 1 2\s
			""");
	}
}
