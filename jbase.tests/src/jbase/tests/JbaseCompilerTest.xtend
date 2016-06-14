package jbase.tests

import com.google.inject.Inject
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider
import jbase.testlanguage.validation.JbaseTestlanguageValidator
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * For compilation tests we use JbaseTestlanguage since we test also
 * parameters.
 */
@RunWith(typeof(XtextRunner))
@InjectWith(typeof(JbaseTestlanguageInjectorProvider))
class JbaseCompilerTest extends JbaseAbstractCompilerTest {

	@Inject
	var JbaseTestlanguageValidator validator

	@Before
	def void initValidator() {
		validator.skipVariableInitializationCheck = false
	}

	@Test def void testEmptyProgram() {
		"".checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
  }
}
'''
		)
	}

	@Test def void testEmptyStatement() {
		";".checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {;
  }
}
'''
		)
	}

	@Test def void testEmptyStatements() {
		";;".checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {;;
  }
}
'''
		)
	}

	@Test def void testHelloWorld() {
		helloWorld.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    System.out.println("Hello world!");
  }
}
'''
			)
	}

	@Test def void testHelloWorldMethod() {
		helloWorldMethod.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testJavaLikeVariableDeclarations() {
		javaLikeVariableDeclarations.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSimpleAccess() {
		'''
		int i;
		i = 0;
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int i = 0;
    i = 0;
  }
}
'''
			)
	}

	@Test def void testSimpleArrayAccess() {
		simpleArrayAccess.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    String[] a = null;
    a[0] = "test";
  }
}
'''
			)
	}

	@Test def void testArrayAccess() {
		arrayAccess.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testArrayAssign() {
		arrayAssign.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    String[] a = null;
    String[] b = null;
    a = b;
  }
}
'''
			)
	}

	@Test def void testArrayAccessInRightHandsideExpression() {
		arrayAccessInRightHandsideExpression.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[] a = null;
    int i = 0;
    i = a[0];
  }
}
'''
			)
	}

	@Test def void testArrayAccessFromFeatureCall() {
		arrayAccessFromFeatureCall.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testArrayAccessFromMemberFeatureCallReceiver() {
		arrayAccessFromMemberFeatureCallReceiver.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[][] arr = null;
    int l = 0;
    int _length = arr[0].length;
    l = _length;
    boolean _equals = arr[0].equals(arr[1]);
    System.out.println(_equals);
    int _hashCode = arr[0].hashCode();
    System.out.println(_hashCode);
  }
}
'''
			)
	}

	@Test def void testArrayAccessFromMemberFeatureCallReceiver2() {
		'''
		String firstArg = args[0];
		char arg = args[0].toCharArray()[0];
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    String firstArg = args[0];
    char arg = args[0].toCharArray()[0];
  }
}
'''
			)
	}

	@Test def void testArrayAccessFromMemberFeatureCallReceiverClone() {
		arrayAccessFromMemberFeatureCallReceiverClone.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[][] arr = null;
    int[] cl = arr[0].clone();
  }
}
'''
			)
	}

	@Test def void testArrayAccessFromMemberFeatureCallReceiverClone2() {
		'''
		// clone has a generic type, so the result is inferred
		String[][] a = null;
		String[] cl1 = a[0].clone();
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		@SuppressWarnings("all")
		public class MyFile {
		  public static void main(String[] args) throws Throwable {
		    String[][] a = null;
		    String[] cl1 = a[0].clone();
		  }
		}
		'''
			)
	}

	@Test def void testArrayAccessAsArgument() {
		arrayAccessAsArgument.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testArrayAccessInForLoop() {
		arrayAccessInForLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testArrayAccessInBinaryOp() {
		arrayAccessInBinaryOp.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[] a = null;
    boolean result = (a[1] > a[2]);
  }
}
'''
			)
	}

	@Test def void testArrayAccessInParenthesizedExpression() {
		arrayAccessInParenthesizedExpression.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[] a = null;
    int i = 0;
    i = a[1];
  }
}
'''
			)
	}

	@Test def void testMultiArrayAccessInRightHandsideExpression() {
		multiArrayAccessInRightHandsideExpression.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[][] a = null;
    int i = 0;
    i = a[0][(1 + 2)];
  }
}
'''
			)
	}

	@Test def void testMultiArrayAccessInLeftHandsideExpression() {
		multiArrayAccessInLeftHandsideExpression.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testArrayConstructorCallInVarDecl() {
		arrayConstructorCallInVarDecl.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[] i = new int[10];
    String[] a = new String[args.length];
  }
}
'''
			)
	}

	@Test def void testMultiArrayConstructorCallInVarDecl() {
		multiArrayConstructorCallInVarDecl.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[][] i = new int[10][20];
    String[][] a = new String[args.length][(args.length + 1)];
  }
}
'''
			)
	}

	@Test def void testMultiArrayConstructorCallWithPartialDimensions() {
		'''
		int[][][] a1 = new int[0][][];
		int[][][] a2 = new int[0][1][];
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[][][] a1 = new int[0][][];
    int[][][] a2 = new int[0][1][];
  }
}
'''
			)
	}

	@Test def void testMultiArrayConstructorCallWithArrayLiteral() {
		multiArrayConstructorCallWithArrayLiteral.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[] i = new int[] { 0, 1, 2 };
    int[][] j = new int[][] { new int[] { 0, 1, 2 }, new int[] { 3, 4, 5 } };
  }
}
'''
			)
	}

	@Test def void testIfThenElseWithoutBlocks() {
		ifThenElseWithoutBlocks.expectationsForIfThenElse
	}

	@Test def void testIfThenElseWithBlocks() {
		ifThenElseWithBlocks.expectationsForIfThenElse
	}
	
	/**
	 * Xbase compiles if then else with blocks even if they're not
	 * there in the original program
	 */
	private def expectationsForIfThenElse(CharSequence input) {
		input.checkCompilation(
			'''
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
			'''
		)
	}

	@Test def void testArrayLiteral() {
		arrayLiteral.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[] a = { 0, 1, 2 };
  }
}
'''
			)
	}

	@Test def void testEmptyArrayLiteral() {
		emptyArrayLiteral.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int[] a = {};
  }
}
'''
			)
	}

	@Test def void testWhileWithSemicolon() {
		whileWithSemicolon.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int i = 0;
    while ((i < 10)) {;
    }
  }
}
'''
			)
	}

	@Test def void testWhileWithoutBlock() {
		whileWithoutBlock.expectationsForWhile
	}

	@Test def void testWhileWithBlock() {
		whileWithBlock.expectationsForWhile
	}

	/**
	 * Xbase compiles if while with blocks even if they're not
	 * there in the original program
	 */
	private def expectationsForWhile(CharSequence input) {
		input.checkCompilation(
'''
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
'''

		)
	}

	@Test def void testDoWhileWithoutBlock() {
		doWhileWithoutBlock.expectationsForDoWhile
	}

	@Test def void testDoWhileWithBlock() {
		doWhileWithBlock.expectationsForDoWhile
	}

	/**
	 * Xbase compiles if while with blocks even if they're not
	 * there in the original program
	 */
	private def expectationsForDoWhile(CharSequence input) {
		input.checkCompilation(
'''
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
'''

		)
	}

	@Test def void testAdditionalSemicolons() {
		additionalSemicolons.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testPostIncrement() {
		postIncrement.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int i = 0;
    int j = i++;
    j++;
  }
}
'''
			)
	}

	@Test def void testPreIncrementAndDecrement() {
		preIncrementAndDecrement.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testMultiAssign() {
		multiAssign.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int i = 0;
    int _i = i;
    i = (_i + 2);
  }
}
'''
			)
	}

	@Test def void testForLoopTranslatedToJavaWhileSingleStatement() {
		forLoopTranslatedToJavaWhileSingleStatement.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testForLoopTranslatedToJavaWhileInsideIf() {
		forLoopTranslatedToJavaWhileInsideIf.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testForLoopTranslatedToJavaWhileNoExpression() {
		forLoopTranslatedToJavaWhileNoExpression.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testForLoopTranslatedToJavaWhileEarlyExit() {
		forLoopTranslatedToJavaWhileEarlyExit.checkCompilation(
'''
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
''', false 
			)
		/** 
		 * this is not valid input since i += 1 is considered not reachable
		 * we use it only to test the compiler.
		 * 
		 * In Xtext 2.9 the body is generated anyway (not valid Java code).
		 * 
		 * In Xtext 2.8 the body is not generated, while in Xtext 2.7.3 the body
		 * was generated anyway:
		 * 
		 * 
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
      }
    }
  }
}
		 */
	}

	@Test def void testForLoopTranslatedToJavaForNoExpression() {
		'''
		int argsNum = args.length();
		for (int i = 0; ; i++)
			System.out.println(""+i);
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInForLoopTranslatedToJavaFor() {
		continueInForLoopTranslatedToJavaFor.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInForLoopTranslatedToJavaWhile() {
		continueInForLoopTranslatedToJavaWhile.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInForLoopTranslatedToJavaWhileNoExpression() {
		continueInForLoopTranslatedToJavaWhileNoExpression.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInBothIfBranchesInForLoopTranslatedToJavaWhile() {
		continueInBothIfBranchesInForLoopTranslatedToJavaWhile.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueSingleInForLoopTranslatedToJavaWhile() {
		continueSingleInForLoopTranslatedToJavaWhile.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInForLoopTranslatedToJavaWhileAndOtherStatementsAfterLoop() {
		continueInForLoopTranslatedToJavaWhileAndOtherStatementsAfterLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInWhileLoop() {
		continueInWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInBothIfBranchesInWhileLoop() {
		continueInBothIfBranchesInWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInDoWhileLoop() {
		continueInDoWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testContinueInBothIfBranchesInDoWhileLoop() {
		continueInBothIfBranchesInDoWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakInForLoopTranslatedToJavaFor() {
		breakInForLoopTranslatedToJavaFor.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakInForLoopTranslatedToJavaWhile() {
		breakInForLoopTranslatedToJavaWhile.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakInBothIfBranchesInForLoopTranslatedToJavaWhile() {
		breakInBothIfBranchesInForLoopTranslatedToJavaWhile.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakSingleInForLoopTranslatedToJavaWhile() {
		breakSingleInForLoopTranslatedToJavaWhile.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakInWhileLoop() {
		breakInWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakInBothIfBranchesInWhileLoop() {
		breakInBothIfBranchesInWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakInDoWhileLoop() {
		breakInDoWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testBreakInBothIfBranchesInDoWhileLoop() {
		breakInBothIfBranchesInDoWhileLoop.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testEmptySwitchStatement() {
		emptySwitchStatement.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int argsNum = args.length;
    switch (argsNum) {
    }
  }
}
'''
			)
	}

	@Test def void testSwitchStatementWithCaseAndDefault() {
		switchStatementWithCaseAndDefault.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSwitchStatementWithCaseAndDefaultMultipleStatements() {
		switchStatementWithCaseAndDefaultMultipleStatements.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSwitchStatementWithBreak() {
		switchStatementWithBreak.checkCompilation(
'''
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
'''
			)
	}

	/**
	 * This would work only for Java 1.7 so for the moment we'll not deal with that
	 */
	// @Test
	def void testSwitchStatementWithStrings() {
		switchStatementWithStrings.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSwitchStatementWithChars() {
		switchStatementWithChars.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSwitchStatementWithBytes() {
		switchStatementWithBytes.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSwitchStatementReturnType() {
		switchStatementReturnType.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testSwitchStatementReturnTypeWithFallback() {
		'''
		op move(int p) : int {
			switch (p) {
				case 0: System.out.println("0"); // the default is executed
				default: return -1;
			}
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testVarNameSameAsMethodName() {
		varNameSameAsMethodName.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testCharTranslatedToJavaChar() {
		'''
		char c1 = 'c';
		char c2 = '\n';
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    char c1 = 'c';
    char c2 = '\n';
  }
}
'''
			)
	}

	@Test def void testEqualsTranslation() {
		'''
		boolean b;
		b = 1 == 2;
		b = 1 != 2;
		b = "a" == "b";
		b = "a" != "b";
		
		String a = new String("a");
		System.out.println("a" == "a"); // true
		System.out.println(a == "a"); // false
		System.out.println("a".equals("a")); // true
		'''.checkCompilation(
'''
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
    boolean _equals = "a".equals("a");
    System.out.println(_equals);
  }
}
'''
			)
	}

	@Test def void testSeveralVariableDeclarations() {
		'''
		int i = 0, j = 1, k = 0;
		System.out.println(i);
		System.out.println(j);
		System.out.println(k);
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSeveralVariableDeclarationsFinal() {
		'''
		final int i = 0, j = 1, k = 2;
		System.out.println(i);
		System.out.println(j);
		System.out.println(k);
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSeveralVariableDeclarationsInForLoop() {
		'''
		for (int i = 0, j = 1, k = 0; i < 0; i++) {
			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
		}
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSeveralVariableDeclarationsInForLoopWithoutVariableInitialization() {
		// we want to check the compiler when there's no initialization for variables
		// (which Xbase can handle)
		// so we must disable the check for initialized variables
		validator.skipVariableInitializationCheck = true

		'''
		for (int i, j = 1, k; i < 0; i++) {
			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
		}
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSeveralUpdatesInForLoop() {
		'''
		for (int i = 0, j = 1; i < 0; i++, j++) {
			System.out.println(i);
			System.out.println(j);
		}
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSeveralVariableDeclarationsInForLoopTranslatedToJavaWhile() {
		'''
		for (int i = 0, j = 1, k = 0; i < 0; i += 1) {
			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
		}
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSeveralAssignmentsInForLoop() {
		'''
		int i;
		int j;
		int k;
		for (i = 0, j = 1, k = 1; i < 0; i++) {
			System.out.println(i);
			System.out.println(j);
			System.out.println(k);
		}
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSpecialOperators() {
		'''
		int i = 2;
		int j = 3;
		int k = 4;
		
		i *= j;
		i /= j;
		i %= j;
		'''.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testNumberLiterals() {
		numberLiterals.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    byte b = 100;
    short s = 1000;
    char c = 1000;
  }
}
'''
		)
	}

	@Test def void testNumberLiteralsInBinaryOperations() {
		// https://github.com/LorenzoBettini/javamm/issues/34
		'''
		System.out.println(1 + 128);
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    System.out.println((1 + 128));
  }
}
'''
		)
	}

	@Test def void testUnaryOperationInBinaryOperations() {
		'''
		System.out.println(1 + -128);
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    System.out.println((1 + -128));
  }
}
'''
		)
	}

	@Test def void testNumberLiteralsInUnaryOperations() {
		// https://github.com/LorenzoBettini/javamm/issues/53
		'''
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
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testCharLiterals() {
		charLiterals.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testCastedExpression() {
		'''
		op m(char c) : int { return 0; }
		
		int i;
		char c = 'c';
		i = (int) c;
		char r1 = (char) (int) c;
		char r2 = (char) m((char) 0);
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testConditionalExpression() {
		'''
		int i = 0;
		i = i > 0 ? 1 : 2;
		int j = i > 0 ? 1 : 2;
		Object o = i < 0 ? 1 : "a";
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testConditionalExpression2() {
		'''
		int i = 0;
		int n = 0;
		if((i=(i==0?5:7)) > 0) n = 4;
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testVectorWithQualifiedName() {
		'''
		java.util.Vector v = new java.util.Vector();
		'''.checkCompilation(
'''
package jbasetestlanguage;

import java.util.Vector;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    Vector v = new Vector<Object>();
  }
}
'''
		)
	}

	@Test def void testVectorWithQualifiedNameAndGeneric() {
		'''
		java.util.Vector<Object> v = new java.util.Vector();
		'''.checkCompilation(
'''
package jbasetestlanguage;

import java.util.Vector;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    Vector<Object> v = new Vector<Object>();
  }
}
'''
		)
	}

	@Test def void testVectorWithQualifiedNameAndGenericInConstructor() {
		'''
		java.util.Vector<String> v = new java.util.Vector<String>();
		'''.checkCompilation(
'''
package jbasetestlanguage;

import java.util.Vector;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    Vector<String> v = new Vector<String>();
  }
}
'''
		)
	}

	@Test def void testWildcardExtends() {
		'''
		java.util.Vector<? extends String> v = new java.util.Vector<String>();
		System.out.println(v.get(0)); // read is allowed
		'''.checkCompilation(
'''
package jbasetestlanguage;

import java.util.Vector;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    Vector<? extends String> v = new Vector<String>();
    String _get = v.get(0);
    System.out.println(_get);
  }
}
'''
		)
	}

	@Test def void testWildcardSuper() {
		'''
		java.util.Vector<? super String> v = new java.util.Vector<String>();
		v.add("s"); // write is allowed
		'''.checkCompilation(
'''
package jbasetestlanguage;

import java.util.Vector;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    Vector<? super String> v = new Vector<String>();
    v.add("s");
  }
}
'''
		)
	}

	@Test def void testImports() {
		'''
		import java.util.List;
		import java.util.LinkedList;
		import java.util.ArrayList;
		
		List l1 = new LinkedList();
		List l2 = new ArrayList();
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testFinalVariable() {
		'''
		final int i = 0;
		System.out.println(i);
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    final int i = 0;
    System.out.println(i);
  }
}
'''
		)
	}

	@Test def void testForEachLoop() {
		'''
		import java.util.List;
		import java.util.ArrayList;
		
		List<String> strings = new ArrayList<String>();
		strings.add("first");
		strings.add("second");
		
		for (String s : strings)
			System.out.println(s);
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testForEachLoopBlock() {
		'''
		import java.util.List;
		import java.util.ArrayList;
		
		List<String> strings = new ArrayList<String>();
		strings.add("first");
		strings.add("second");
		
		for (String s : strings) {
			System.out.println(s);
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testForEachLoopWithFinalParam() {
		'''
		import java.util.List;
		import java.util.ArrayList;
		
		List<String> strings = new ArrayList<String>();
		strings.add("first");
		strings.add("second");
		
		for (final String s : strings)
			System.out.println(s);
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testInstanceOf() {
		'''
		import java.util.Collection;
		import java.util.List;
		import java.util.ArrayList;
		
		Collection<String> strings = new ArrayList<String>();
		if (strings instanceof List) {
			// get is in List but not in Collection
			System.out.println(((List)strings).get(0));
		}
		'''.checkCompilation(
'''
package jbasetestlanguage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    Collection<String> strings = new ArrayList<String>();
    if ((strings instanceof List)) {
      Object _get = ((List) strings).get(0);
      System.out.println(_get);
    }
  }
}
'''
		)
	}

	@Test def void testAssignmentToJvmOperationReference() {
		'''
		op m(String s) : String { return null; }
		m = "a";
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testAssignmentToFieldReference() {
		'''
		aString : String
		method m(String s) : void {
			aString = s;
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testAssignmentToAssignment() {
		'''
		int i, j;
		i = j = 0;
		'''.checkCompilation(
'''
package jbasetestlanguage;

@SuppressWarnings("all")
public class MyFile {
  public static void main(String[] args) throws Throwable {
    int i = 0;
    int j = 0;
    i = (j = 0);
  }
}
'''
		)
	}

	@Test def void testAssignmentToAssignment2() {
		'''
		int i = 0;
		int j = 0;
		int n = 0;
		n=i=(j==0 ? 1 : 2);
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testAssignmentAsCallArgument() {
		'''
		aString : String
		method m(String s) : void {
			m(aString = s);
			this.m(aString = s);
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testAssignmentAsCallArgument2() {
		'''
		int i = 0;
		Math.max( i = i + 1, i == 1 ? 1 : 2);
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testThrow() {
		'''
		method m(String s) : void {
			throw new Exception("test");
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testTryCatch() {
		'''
		try {
			int i = 0;
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testTryCatchFinally() {
		'''
		try {
			int i = 0;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("finally");
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testTryFinally() {
		'''
		try {
			int i = 0;
		} finally {
			System.out.println("finally");
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testSynchronized() {
		'''
		Object o = null;
		synchronized (o) {
			o.wait();
		}
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testClassObjectImported() {
		'''
		import java.util.Date;
		
		System.out.println(Date.class);
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import java.util.Date;
		
		@SuppressWarnings("all")
		public class MyFile {
		  public static void main(String[] args) throws Throwable {
		    System.out.println(Date.class);
		  }
		}
		'''
		)
	}

	@Test def void testClassObjectInArray() {
		'''
		import java.util.Date;
		
		Class<?>[] classes = {String.class, Date.class};
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import java.util.Date;
		
		@SuppressWarnings("all")
		public class MyFile {
		  public static void main(String[] args) throws Throwable {
		    Class<?>[] classes = { String.class, Date.class };
		  }
		}
		'''
		)
	}

	@Test def void testClassObject() {
		'''
		System.out.println(String.class);
		Class<String> c = String.class;
		String name = String.class.getName();
		System.out.println(String[].class);
		Class<String[]> c1 = String[].class;
		String name1 = String[].class.getName();
		// raw types for variables
		Class c3 = String[].class;
		Class<?> c4 = String[].class;
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testAnnotation() {
		'''
		import com.google.inject.Inject;
		
		@Inject
		o : Object
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import com.google.inject.Inject;
		
		@SuppressWarnings("all")
		public class MyFile {
		  @Inject
		  private Object o;
		  
		  public static void main(String[] args) throws Throwable {
		  }
		}
		'''
		)
	}

	@Test def void testAnnotation2() {
		'''
		import jbase.tests.util.ExampleAnnotation;
		import org.eclipse.xtext.xbase.junit.typesystem.TypeSystemSmokeTester;
		
		@ExampleAnnotation(value = TypeSystemSmokeTester.class)
		o : Object
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import jbase.tests.util.ExampleAnnotation;
		import org.eclipse.xtext.xbase.junit.typesystem.TypeSystemSmokeTester;
		
		@SuppressWarnings("all")
		public class MyFile {
		  @ExampleAnnotation(value = TypeSystemSmokeTester.class)
		  private Object o;
		  
		  public static void main(String[] args) throws Throwable {
		  }
		}
		'''
		)
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePair() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3(value = String.class)
		o : Object
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import jbase.tests.util.ExampleAnnotation3;
		
		@SuppressWarnings("all")
		public class MyFile {
		  @ExampleAnnotation3(value = String.class)
		  private Object o;
		  
		  public static void main(String[] args) throws Throwable {
		  }
		}
		'''
		)
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesAndExplicitValuePairAndMultiple() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3(value = { String.class, Integer.class })
		o : Object
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import jbase.tests.util.ExampleAnnotation3;
		
		@SuppressWarnings("all")
		public class MyFile {
		  @ExampleAnnotation3(value = { String.class, Integer.class })
		  private Object o;
		  
		  public static void main(String[] args) throws Throwable {
		  }
		}
		'''
		)
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesAndSingleValue() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3(String.class)
		o : Object
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import jbase.tests.util.ExampleAnnotation3;
		
		@SuppressWarnings("all")
		public class MyFile {
		  @ExampleAnnotation3(String.class)
		  private Object o;
		  
		  public static void main(String[] args) throws Throwable {
		  }
		}
		'''
		)
	}

	@Test def void testAnnotationsWithExpectedMultipleValuesArrayLiteral() {
		'''
		import jbase.tests.util.ExampleAnnotation3;
		
		@ExampleAnnotation3({String.class, Integer.class})
		o : Object
		'''.checkCompilation(
		'''
		package jbasetestlanguage;
		
		import jbase.tests.util.ExampleAnnotation3;
		
		@SuppressWarnings("all")
		public class MyFile {
		  @ExampleAnnotation3({ String.class, Integer.class })
		  private Object o;
		  
		  public static void main(String[] args) throws Throwable {
		  }
		}
		'''
		)
	}

	@Test def void testLoopsWithConditionAlwaysTrue() {
		'''
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
		'''.checkCompilation(
'''
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
'''
		)
	}

	@Test def void testBubbleSort() {
		bubbleSort.checkCompilation(
'''
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
'''
			)
	}

	@Test def void testSudoku() {
		sudoku.assertExecuteMain(
'''
4 0 0 0 
0 0 0 3 
0 1 3 0 
0 0 0 2 
true
4 3 2 1 
1 2 4 3 
2 1 3 4 
3 4 1 2 
'''
		)
	}

}
