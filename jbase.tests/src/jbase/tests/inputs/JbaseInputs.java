package jbase.tests.inputs;

public class JbaseInputs {

	public CharSequence helloWorld() {
		return """
			System.out.println("Hello world!");
			""";
	}

	public CharSequence simpleArrayAccess() {
		return """
			String[] a;
			a[0] = "test";
			""";
	}

	public CharSequence arrayAssign() {
		return """
			String[] a;
			String[] b = null;
			a = b;
			""";
	}

	public CharSequence arrayAccessInRightHandsideExpression() {
		return """
			int[] a = null;
			int i;
			i = a[0];
			""";
	}

	public CharSequence arrayAccessFromMemberFeatureCallReceiver() {
		return """
			int[][] arr = null;
			int l;
			l = arr[0].length;
			System.out.println(arr[0].equals(arr[1]));
			System.out.println(arr[0].hashCode());
			""";
	}

	public CharSequence arrayAccessFromMemberFeatureCallReceiverClone() {
		return """
			int[][] arr = null;
			int[] cl = arr[0].clone();
			""";
	}

	public CharSequence arrayAccessInForLoop() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1) {
				System.out.println("args[" + i + "] = " + args[i] );
			}
			""";
	}

	public CharSequence arrayAccessInBinaryOp() {
		return """
			int[] a = null;
			boolean result = a[1] > a[2];
			""";
	}

	public CharSequence arrayAccessInParenthesizedExpression() {
		return """
			int[] a = null;
			int i;
			i = (a)[1];
			""";
	}

	public CharSequence multiArrayAccessInRightHandsideExpression() {
		return """
			int[][] a = null;
			int i;
			i = a[0][1+2];
			""";
	}

	public CharSequence multiArrayAccessInLeftHandsideExpression() {
		return """
			int[][] a = null;
			int[][] b = null;
			a[0][1+2] = 1;
			a[0] = b[1];
			a = b;
			""";
	}

	public CharSequence arrayConstructorCallInVarDecl() {
		return """
			int[] i = new int[10];
			String[] a = new String[args.length];
			""";
	}

	public CharSequence multiArrayConstructorCallInVarDecl() {
		return """
			int[][] i = new int[10][20];
			String[][] a = new String[args.length][args.length+1];
			""";
	}

	public CharSequence multiArrayConstructorCallWithArrayLiteral() {
		return """
			int[] i = new int[] {0, 1, 2};
			int[][] j = new int[][] {{0, 1, 2},{3, 4, 5}};
			""";
	}

	public CharSequence constructorCallWithDiamondInVarDecl() {
		return """
			java.util.List<String> list1 = new java.util.ArrayList<>();
			java.util.List<? extends String> list2 = new java.util.ArrayList<>();
			java.util.List<java.util.LinkedList<String>> list3 = new java.util.ArrayList<>();
			""";
	}

	public CharSequence constructorCallWithDiamondInVarDeclNestedWildcard() {
		return """
			java.util.List<java.util.LinkedList<? extends String>> list3 = new java.util.ArrayList<>();
			""";
	}

	public CharSequence constructorCallWithDiamondInAssignment() {
		return """
			java.util.List<String> list1;
			list1 = new java.util.ArrayList<>();
			java.util.List<? extends String> list2;
			list2 = new java.util.ArrayList<>();
			java.util.List<java.util.LinkedList<String>> list3;
			list3 = new java.util.ArrayList<>();
			""";
	}

	public CharSequence constructorCallWithDiamondInAssignmentNestedWildcard() {
		return """
			java.util.List<java.util.LinkedList<? extends String>> list3;
			list3 = new java.util.ArrayList<>();
			""";
	}

	public CharSequence ifThenElseWithoutBlocks() {
		return """
			if (args.length() == 0)
				System.out.println("No args");
			else
				System.out.println("Args");
			""";
	}

	public CharSequence ifThenElseWithBlocks() {
		return """
			if (args.length() == 0) {
				System.out.println("No args");
			} else {
				System.out.println("Args");
			}
			""";
	}

	public CharSequence arrayLiteral() {
		return """
			int[] a = { 0, 1, 2 };
			""";
	}

	public CharSequence emptyArrayLiteral() {
		return """
			int[] a = {};
			""";
	}

	public CharSequence whileWithSemicolon() {
		return """
			int i = 0;
			while (i < 10)
				;
			""";
	}

	public CharSequence whileWithoutBlock() {
		return """
			int i = 0;
			while (i < 10)
				i = i + 1;
			""";
	}

	public CharSequence whileWithBlock() {
		return """
			int i = 0;
			while (i < 10) {
				i = i + 1;
			}
			""";
	}

	public CharSequence doWhileWithoutBlock() {
		return """
			int i = 0;
			do
				i = i + 1;
			while (i < 10);
			""";
	}

	public CharSequence doWhileWithBlock() {
		return """
			int i = 0;
			do {
				i = i + 1;
			} while (i < 10);
			""";
	}

	public CharSequence postIncrement() {
		return """
			int i = 0;
			int j = i++;
			j++;
			""";
	}

	public CharSequence preIncrementAndDecrement() {
		return """
			int i = 0;
			int j = ++i;
			j = ++i;
			++j;
			int k = --i;
			k = --i;
			--k;
			""";
	}

	public CharSequence multiAssign() {
		return """
			int i = 0;
			i += 2;
			""";
	}

	public CharSequence forLoopTranslatedToJavaWhileSingleStatement() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1)
				System.out.println(""+i);
			""";
	}

	public CharSequence forLoopTranslatedToJavaWhileInsideIf() {
		return """
			int argsNum = args.length();
			if (argsNum != 0)
				for (int i = 0; i < argsNum; i += 1)
					System.out.println(""+i);
			""";
	}

	public CharSequence forLoopTranslatedToJavaWhileNoExpression() {
		return """
			int argsNum = args.length();
			for (int i = 0; ; i += 1)
				System.out.println(""+i);
			""";
	}

	/**
	 * this is not valid input since i += 1 is considered not reachable
	 * we use it only to test the compiler
	 */
	public CharSequence forLoopTranslatedToJavaWhileEarlyExit() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1)
				return;
			""";
	}

	public CharSequence continueInForLoopTranslatedToJavaFor() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i++) {
				if (argsNum > 0) {
					continue;
				} else {
					System.out.println("");
				}
			}
			""";
	}

	public CharSequence continueInForLoopTranslatedToJavaWhile() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1) {
				if (argsNum > 0) {
					continue;
				} else {
					System.out.println("");
				}
			}
			""";
	}

	public CharSequence continueInForLoopTranslatedToJavaWhileNoExpression() {
		return """
			int argsNum = args.length();
			for (int i = 0; ; i += 1) {
				if (argsNum > 0) {
					continue;
				} else {
					System.out.println("");
				}
			}
			""";
	}

	public CharSequence continueInBothIfBranchesInForLoopTranslatedToJavaWhile() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1) {
				if (argsNum > 0) {
					continue;
				} else {
					continue;
				}
			}
			""";
	}

	public CharSequence continueSingleInForLoopTranslatedToJavaWhile() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1)
				continue;
			""";
	}

	public CharSequence continueInForLoopTranslatedToJavaWhileAndOtherStatementsAfterLoop() {
		return """
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
			""";
	}

	public CharSequence continueInWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			while (i < argsNum) {
				if (argsNum > 0) {
					continue;
				} else {
					System.out.println("");
				}
			}
			""";
	}

	public CharSequence continueInBothIfBranchesInWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			while (i < argsNum) {
				if (argsNum > 0) {
					continue;
				} else {
					continue;
				}
			}
			""";
	}

	public CharSequence continueInDoWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			do {
				if (argsNum > 0) {
					continue;
				} else {
					System.out.println("");
				}
			} while (i < argsNum);
			""";
	}

	public CharSequence continueInBothIfBranchesInDoWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			do {
				if (argsNum > 0) {
					continue;
				} else {
					continue;
				}
			} while (i < argsNum);
			""";
	}

	public CharSequence breakInForLoopTranslatedToJavaFor() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i++) {
				if (argsNum > 0) {
					break;
				} else {
					System.out.println("");
				}
			}
			""";
	}

	public CharSequence breakInForLoopTranslatedToJavaWhile() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1) {
				if (argsNum > 0) {
					break;
				} else {
					System.out.println("");
				}
			}
			""";
	}

	public CharSequence breakInBothIfBranchesInForLoopTranslatedToJavaWhile() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1) {
				if (argsNum > 0) {
					break;
				} else {
					break;
				}
			}
			""";
	}

	public CharSequence breakSingleInForLoopTranslatedToJavaWhile() {
		return """
			int argsNum = args.length();
			for (int i = 0; i < argsNum; i += 1)
				break;
			""";
	}

	public CharSequence breakInWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			while (i < argsNum) {
				if (argsNum > 0) {
					break;
				} else {
					System.out.println("");
				}
			}
			""";
	}

	public CharSequence breakInBothIfBranchesInWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			while (i < argsNum) {
				if (argsNum > 0) {
					break;
				} else {
					break;
				}
			}
			""";
	}

	public CharSequence breakInDoWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			do {
				if (argsNum > 0) {
					break;
				} else {
					System.out.println("");
				}
			} while (i < argsNum);
			""";
	}

	public CharSequence breakInBothIfBranchesInDoWhileLoop() {
		return """
			int argsNum = args.length();
			int i = 0;
			do {
				if (argsNum > 0) {
					break;
				} else {
					break;
				}
			} while (i < argsNum);
			""";
	}

	public CharSequence emptySwitchStatement() {
		return """
			int argsNum = args.length();
			switch (argsNum) {
			\t
			}
			""";
	}

	public CharSequence switchStatementWithCaseAndDefault() {
		return """
			int argsNum = args.length();
			switch (argsNum) {
				case 0 : System.out.println("0");
				default: System.out.println("default");
			}
			""";
	}

	public CharSequence switchStatementWithCaseAndDefaultMultipleStatements() {
		return """
			int argsNum = args.length();
			int i;
			switch (argsNum) {
				case 0 : i = 0; System.out.println("0");
				default: i = -1; System.out.println("default");
			}
			""";
	}

	public CharSequence switchStatementWithBreak() {
		return """
			int argsNum = args.length();
			int i;
			switch (argsNum) {
				case 0 :\s
					i = 0;
					System.out.println("0");
					break;
				default:\s
					i = -1;\s
					System.out.println("default");
					break;
			}
			""";
	}

	/**
	 * This would work only for Java 1.7 so for the moment we'll not deal with that
	 */
	public CharSequence switchStatementWithStrings() {
		return """
			int argsNum = args.length();
			String arg = args[0];
			int i;
			switch (arg) {
				case "first" :\s
					i = 0;
					System.out.println("0");
					break;
				default:\s
					i = -1;\s
					System.out.println("default");
					break;
			}
			""";
	}

	public CharSequence switchStatementWithChars() {
		return """
			int argsNum = args.length();
			String firstArg = args[0];
			char arg = firstArg.toCharArray()[0];
			// char arg = args[0].toCharArray()[0];
			int i;
			switch (arg) {
				case 'f' :\s
					i = 0;
					System.out.println("0");
					break;
				default:\s
					i = -1;\s
					System.out.println("default");
					break;
			}
			""";
	}

	public CharSequence switchStatementWithBytes() {
		return """
			byte b = 0;
			switch (b) {
				case 10:
					System.out.println("10");
				case 'f' :\s
					System.out.println("f");
					break;
				default:\s
					System.out.println("default");
					break;
			}
			""";
	}

	public CharSequence numberLiterals() {
		return """
			byte b = 100;
			short s = 1000;
			char c = 1000;
			""";
	}

	public CharSequence charLiterals() {
		return """
			byte b = 'c';
			short s = 'c';
			char c = 'c';
			int i = 'c';
			long l = 'c';
			double d = 'c';
			float f = 'c';
			""";
	}
}
