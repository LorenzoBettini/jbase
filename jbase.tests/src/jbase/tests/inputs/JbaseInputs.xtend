package jbase.tests.inputs

class JbaseInputs {

	def helloWorld() {
		'''
		System.out.println("Hello world!");
		'''
	}

	def simpleArrayAccess() {
		'''
		String[] a;
		a[0] = "test";
		'''
	}

	def arrayAssign() {
		'''
		String[] a;
		String[] b = null;
		a = b;
		'''
	}

	def arrayAccessInRightHandsideExpression() {
		'''
		int[] a = null;
		int i;
		i = a[0];
		'''
	}

	def arrayAccessFromMemberFeatureCallReceiver() {
		'''
		int[][] arr = null;
		int l;
		l = arr[0].length;
		System.out.println(arr[0].equals(arr[1]));
		System.out.println(arr[0].hashCode());
		'''
	}

	def arrayAccessFromMemberFeatureCallReceiverClone() {
		'''
		int[][] arr = null;
		int[] cl = arr[0].clone();
		'''
	}

	def arrayAccessInForLoop() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1) {
			System.out.println("args[" + i + "] = " + args[i] );
		}
		'''
	}

	def arrayAccessInBinaryOp() {
		'''
		int[] a = null;
		boolean result = a[1] > a[2];
		'''
	}

	def arrayAccessInParenthesizedExpression() {
		'''
		int[] a = null;
		int i;
		i = (a)[1];
		'''
	}

	def multiArrayAccessInRightHandsideExpression() {
		'''
		int[][] a = null;
		int i;
		i = a[0][1+2];
		'''
	}

	def multiArrayAccessInLeftHandsideExpression() {
		'''
		int[][] a = null;
		int[][] b = null;
		a[0][1+2] = 1;
		a[0] = b[1];
		a = b;
		'''
	}

	def arrayConstructorCallInVarDecl() {
		'''
		int[] i = new int[10];
		String[] a = new String[args.length];
		'''
	}

	def multiArrayConstructorCallInVarDecl() {
		'''
		int[][] i = new int[10][20];
		String[][] a = new String[args.length][args.length+1];
		'''
	}

	def multiArrayConstructorCallWithArrayLiteral() {
		'''
		int[] i = new int[] {0, 1, 2};
		int[][] j = new int[][] {{0, 1, 2},{3, 4, 5}};
		'''
	}

	def constructorCallWithDiamondInVarDecl() {
		'''
		java.util.List<String> list1 = new java.util.ArrayList<>();
		java.util.List<? extends String> list2 = new java.util.ArrayList<>();
		java.util.List<java.util.LinkedList<String>> list3 = new java.util.ArrayList<>();
		'''
	}

	def constructorCallWithDiamondInVarDeclNestedWildcard() {
		'''
		java.util.List<java.util.LinkedList<? extends String>> list3 = new java.util.ArrayList<>();
		'''
	}

	def ifThenElseWithoutBlocks() {
		'''
		if (args.length() == 0)
			System.out.println("No args");
		else
			System.out.println("Args");
		'''
	}

	def ifThenElseWithBlocks() {
		'''
		if (args.length() == 0) {
			System.out.println("No args");
		} else {
			System.out.println("Args");
		}
		'''
	}

	def arrayLiteral() {
		'''
		int[] a = { 0, 1, 2 };
		'''
	}

	def emptyArrayLiteral() {
		'''
		int[] a = {};
		'''
	}

	def whileWithSemicolon() {
		'''
		int i = 0;
		while (i < 10)
			;
		'''
	}

	def whileWithoutBlock() {
		'''
		int i = 0;
		while (i < 10)
			i = i + 1;
		'''
	}

	def whileWithBlock() {
		'''
		int i = 0;
		while (i < 10) {
			i = i + 1;
		}
		'''
	}

	def doWhileWithoutBlock() {
		'''
		int i = 0;
		do
			i = i + 1;
		while (i < 10);
		'''
	}

	def doWhileWithBlock() {
		'''
		int i = 0;
		do {
			i = i + 1;
		} while (i < 10);
		'''
	}

	def postIncrement() {
		'''
		int i = 0;
		int j = i++;
		j++;
		'''
	}

	def preIncrementAndDecrement() {
		'''
		int i = 0;
		int j = ++i;
		j = ++i;
		++j;
		int k = --i;
		k = --i;
		--k;
		'''
	}

	def multiAssign() {
		'''
		int i = 0;
		i += 2;
		'''
	}

	def forLoopTranslatedToJavaWhileSingleStatement() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1)
			System.out.println(""+i);
		'''
	}

	def forLoopTranslatedToJavaWhileInsideIf() {
		'''
		int argsNum = args.length();
		if (argsNum != 0)
			for (int i = 0; i < argsNum; i += 1)
				System.out.println(""+i);
		'''
	}

	def forLoopTranslatedToJavaWhileNoExpression() {
		'''
		int argsNum = args.length();
		for (int i = 0; ; i += 1)
			System.out.println(""+i);
		'''
	}

	/** 
	 * this is not valid input since i += 1 is considered not reachable
	 * we use it only to test the compiler
	 */
	def forLoopTranslatedToJavaWhileEarlyExit() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1)
			return;
		'''
	}

	def continueInForLoopTranslatedToJavaFor() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i++) {
			if (argsNum > 0) {
				continue;
			} else {
				System.out.println("");
			}
		}
		'''
	}

	def continueInForLoopTranslatedToJavaWhile() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1) {
			if (argsNum > 0) {
				continue;
			} else {
				System.out.println("");
			}
		}
		'''
	}

	def continueInForLoopTranslatedToJavaWhileNoExpression() {
		'''
		int argsNum = args.length();
		for (int i = 0; ; i += 1) {
			if (argsNum > 0) {
				continue;
			} else {
				System.out.println("");
			}
		}
		'''
	}

	def continueInBothIfBranchesInForLoopTranslatedToJavaWhile() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1) {
			if (argsNum > 0) {
				continue;
			} else {
				continue;
			}
		}
		'''
	}

	def continueSingleInForLoopTranslatedToJavaWhile() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1)
			continue;
		'''
	}

	def continueInForLoopTranslatedToJavaWhileAndOtherStatementsAfterLoop() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1) {
			if (argsNum > 0) {
				continue;
			} else {
				System.out.println("");
			}
		}
		int j = 0;
		System.out.println(j);
		'''
	}

	def continueInWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		while (i < argsNum) {
			if (argsNum > 0) {
				continue;
			} else {
				System.out.println("");
			}
		}
		'''
	}

	def continueInBothIfBranchesInWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		while (i < argsNum) {
			if (argsNum > 0) {
				continue;
			} else {
				continue;
			}
		}
		'''
	}

	def continueInDoWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		do {
			if (argsNum > 0) {
				continue;
			} else {
				System.out.println("");
			}
		} while (i < argsNum);
		'''
	}

	def continueInBothIfBranchesInDoWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		do {
			if (argsNum > 0) {
				continue;
			} else {
				continue;
			}
		} while (i < argsNum);
		'''
	}

	def breakInForLoopTranslatedToJavaFor() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i++) {
			if (argsNum > 0) {
				break;
			} else {
				System.out.println("");
			}
		}
		'''
	}

	def breakInForLoopTranslatedToJavaWhile() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1) {
			if (argsNum > 0) {
				break;
			} else {
				System.out.println("");
			}
		}
		'''
	}

	def breakInBothIfBranchesInForLoopTranslatedToJavaWhile() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1) {
			if (argsNum > 0) {
				break;
			} else {
				break;
			}
		}
		'''
	}

	def breakSingleInForLoopTranslatedToJavaWhile() {
		'''
		int argsNum = args.length();
		for (int i = 0; i < argsNum; i += 1)
			break;
		'''
	}

	def breakInWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		while (i < argsNum) {
			if (argsNum > 0) {
				break;
			} else {
				System.out.println("");
			}
		}
		'''
	}

	def breakInBothIfBranchesInWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		while (i < argsNum) {
			if (argsNum > 0) {
				break;
			} else {
				break;
			}
		}
		'''
	}

	def breakInDoWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		do {
			if (argsNum > 0) {
				break;
			} else {
				System.out.println("");
			}
		} while (i < argsNum);
		'''
	}

	def breakInBothIfBranchesInDoWhileLoop() {
		'''
		int argsNum = args.length();
		int i = 0;
		do {
			if (argsNum > 0) {
				break;
			} else {
				break;
			}
		} while (i < argsNum);
		'''
	}

	def emptySwitchStatement() {
		'''
		int argsNum = args.length();
		switch (argsNum) {
			
		}
		'''
	}

	def switchStatementWithCaseAndDefault() {
		'''
		int argsNum = args.length();
		switch (argsNum) {
			case 0 : System.out.println("0");
			default: System.out.println("default");
		}
		'''
	}

	def switchStatementWithCaseAndDefaultMultipleStatements() {
		'''
		int argsNum = args.length();
		int i;
		switch (argsNum) {
			case 0 : i = 0; System.out.println("0");
			default: i = -1; System.out.println("default");
		}
		'''
	}

	def switchStatementWithBreak() {
		'''
		int argsNum = args.length();
		int i;
		switch (argsNum) {
			case 0 : 
				i = 0;
				System.out.println("0");
				break;
			default: 
				i = -1; 
				System.out.println("default");
				break;
		}
		'''
	}

	/**
	 * This would work only for Java 1.7 so for the moment we'll not deal with that
	 */
	def switchStatementWithStrings() {
		'''
		int argsNum = args.length();
		String arg = args[0];
		int i;
		switch (arg) {
			case "first" : 
				i = 0;
				System.out.println("0");
				break;
			default: 
				i = -1; 
				System.out.println("default");
				break;
		}
		'''
	}

	def switchStatementWithChars() {
		'''
		int argsNum = args.length();
		String firstArg = args[0];
		char arg = firstArg.toCharArray()[0];
		// char arg = args[0].toCharArray()[0];
		int i;
		switch (arg) {
			case 'f' : 
				i = 0;
				System.out.println("0");
				break;
			default: 
				i = -1; 
				System.out.println("default");
				break;
		}
		'''
	}

	def switchStatementWithBytes() {
		'''
		byte b = 0;
		switch (b) {
			case 10:
				System.out.println("10");
			case 'f' : 
				System.out.println("f");
				break;
			default: 
				System.out.println("default");
				break;
		}
		'''
	}

	def numberLiterals() {
'''
byte b = 100;
short s = 1000;
char c = 1000;
'''
	}

	def charLiterals() {
'''
byte b = 'c';
short s = 'c';
char c = 'c';
int i = 'c';
long l = 'c';
double d = 'c';
float f = 'c';
'''
	}

}
