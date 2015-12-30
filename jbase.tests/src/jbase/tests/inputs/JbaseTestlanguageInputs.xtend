package jbase.tests.inputs

class JbaseTestlanguageInputs {

	def helloWorldMethod() {
		'''
		op sayHelloWorld(String m) : void {
			System.out.println(m);
		}
		
		sayHelloWorld("Hello world!");
		'''
	}

	def javaLikeVariableDeclarations() {
		'''
		op foo() {
			int i = 0;
			int j;
			j = 1;
			return (i > 0);
		}
		
		int i = 0;
		boolean b = false;
		boolean cond = (i > 0);
		'''
	}

	def additionalSemicolons() {
		'''
		op m() { return; }
		int i = 0;;;
		while (i < 10) {
			i = i + 1;
		}
		'''
	}

	def assignToParam() {
		'''
		op m(int a) : void {
			a = 1;
		}
		'''
	}

	def assignToParamAndReturn() {
		'''
		op m(int a) {
			return a = 1;
		}
		'''
	}

	def arrayAssignElementFinalParameter() {
		'''
		op m(int[] a) : void {
			a[0] = 1;
		}
		'''
	}

	def arrayAccess() {
		'''
		op getIndex() {
			return 0;
		}
		
		String[] a;
		int i = 0;
		int j = 1;
		a[i+j] = "test";
		a[i-getIndex()+1] = "test";
		'''
	}

	def arrayAccessFromFeatureCall() {
		'''
		op getArray() : int[] {
			return null;
		}
		
		int i;
		i = getArray()[0];
		'''
	}

	def arrayAccessAsArgument() {
		'''
		op getArg(int i) : int {
			return i;
		}
		
		int[] j = null;
		getArg(j[0]);
		'''
	}

	def switchStatementReturnType() {
		'''
		op move(int p) : int {
			switch (p) {
				case 0: return 2;
				case 1: return 1;
				case 2: return 1;
				case 3: return 2;
				case 4: return 1;
				default: return -1;
			}
		}
		'''
	}

	def varNameSameAsMethodName() {
'''
op numOfDigits(int num) {
  int numOfDigits = 1;
  while (num/10>0) {
    numOfDigits = numOfDigits+1;
    num = num/10;
  }
  return numOfDigits;
}

System.out.println("numOfDigits(3456): " +numOfDigits(3456)); 
'''
	}

	def bubbleSort() {
'''
op bubbleSort(int[] array) : void {
    boolean swapped = true;
    int j = 0;
    int tmp;
    while (swapped) {
        swapped = false;
        j = j + 1;
        for (int i = 0; i < array.length - j; i++) {
            if (array[i] > array[i + 1]) {
                tmp = array[i];
                array[i] = array[i + 1];
                array[i + 1] = tmp;
                swapped = true;
            }
        }
    }
}
'''
	}

	def sudoku() {
'''
/**
 * Example Sudoku of the book
 */
op nextCell( int[] c,int[][] s ) : int[] {
  for (int j = c[1] + 1; j < s.length; j++) {
    if (s[c[0]][j] == 0) {
      return new int[] { c[0], j };
    }
  }
  for (int i = c[0] + 1; i < s.length; i++) {
    for (int j = 0; j < s.length; j++) {
      if (s[i][j] == 0) {
        return new int[] { i, j };
      }
    }
  }
  return new int[] { -1, -1 };
}
op feasible(int d, int[] c, int n, int[][] s) : boolean {
  for (int i = 0; i < n * n; i++) {
    if (s[c[0]][i] == d) {
      return false;
    }
  }
  for (int i = 0; i < n * n; i++) {
    if (s[i][c[1]] == d) {
      return false;
    }
  }
  int fr = (c[0] / n) * n;
  int fc = (c[1] / n) * n;
  for (int i = fr; i < fr + n; i++) {
    for (int j = fc; j < fc + n; j++) {
      if (s[i][j] == d) {
        return false;
      }
    }
  }
  return true;
}
op feasibleDigits(int[] c, int n, int[][] s) : boolean[] {
  boolean[] r = new boolean[n * n];
  for (int d = 1; d <= n * n; d++) {
    r[d - 1] = feasible(d, c, n, s);
  }
  return r;
}
op solvable(int[] c, int n, int[][] s) : boolean {
  boolean[] a = feasibleDigits(c, n, s);
  for (int d = 1; d <= n*n; d++) {
    if (a[d-1]) {
      s[c[0]][c[1]] = d;
      int[] nc = nextCell(c, s);
      if (nc[0] >= 0 && !solvable(nc, n, s)) {
        s[c[0]][c[1]] = 0;
      } else {
        return true;
      }
    }
  }
  return false;
}
op printBoard(int[][] s) : void {
  for (int i = 0; i < s.length; i++) {
    for (int j = 0; j < s.length; j++) {
      System.out.print(s[i][j] + " ");
    }
   System.out.println();
  }
}

op testMe() : void {
  int[][] s = { { 4, 0, 0, 0 }, { 0, 0, 0, 3 }, 
    { 0, 1, 3, 0 },{ 0, 0, 0, 2 } };
  int n = 2;
  printBoard( s );
  int[] p = nextCell(new int[] { 0, -1 }, s);
  System.out.println(solvable(p, n, s));
  printBoard( s );
'''
	}

}
