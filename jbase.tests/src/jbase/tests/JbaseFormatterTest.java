package jbase.tests;

import com.google.inject.Inject;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.formatter.FormatterTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseFormatterTest extends JbaseAbstractTest {

	@Inject
	private FormatterTestHelper formatterTestHelper;

	@Test
	public void testProgramSpaces() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				import java.util.List;
				import static java.util.Arrays.*;

				method m1() : int {
					return 0;
				}

				op m() : int {
					return 0;
				}

				op n() {
					return 0;
				}

				System.out.println(m());

				""");
			expression.setToBeFormatted("""


				import  java.util.List ;
				import static  java.util.Arrays.* ;


				method m1()  :  int  { return 0; }
				op m()  :  int  { return 0; }
				op n() { return 0; }


				System.out.println(m());


				""");
		});
	}

	@Test
	public void testProgramSpacesWithOnlyMain() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				import java.util.List;
				import static java.util.Arrays.*;

				System.out.println(m());

				""");
			expression.setToBeFormatted("""


				import  java.util.List ;
				import static  java.util.Arrays.* ;




				System.out.println(m());


				""");
		});
	}

	@Test
	public void testProgramSpacesWithOnlyImports() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				import java.util.List;
				import static java.util.Arrays.*;

				""");
			expression.setToBeFormatted("""


				import  java.util.List ;
				import static  java.util.Arrays.* ;

				""");
		});
	}

	@Test
	public void testFormatImportSection() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				import java.util.List;
				import static java.util.Arrays.*;

				""");
			expression.setToBeFormatted("""
				import  java.util.List ;
				import static  java.util.Arrays.* ;
				""");
		});
	}

	@Test
	public void testSemicolonInSingleStatements() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				if (s)
					continue;
				if (s)
					System.out.println(s);
				if (s)
					a[0].println();
				if (s)
					a[0];
				if (s)
					int j = 0;
				if (s)
					do {
						int j = 0;
					} while (true);
				""");
			expression.setToBeFormatted("""
				if (s)
					continue ;
				if (s)
					System.out.println(s) ;
				if (s)
					a[0].println() ;
				if (s)
					a [ 0 ] ;
				if (s)
					int  j  =  0  ;
				if (s)
					do { int j = 0; } while  (true);
				""");
		});
	}

	@Test
	public void testSemicolonInSingleStatements2() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				if (s)
					a[0].println();

				for (String s : strings)
					a[0].println();
				""");
			expression.setToBeFormatted("""
				if (s)
					a   [    0 ].println() ;

				for (String s : strings)
					a   [    0 ].println() ;
				""");
		});
	}

	@Test
	public void testEmptyStatements() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				;
				;
				""");
			expression.setToBeFormatted("""
				; ;\s
				""");
		});
	}

	@Test
	public void testFormatBlock() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				{
					System.out.println("1");
					System.out.println("2");
				}
				""");
			expression.setToBeFormatted("""
				{ System.out.println("1");  System.out.println("2") ;  }
				""");
		});
	}

	@Test
	public void testMain() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				System.out.println("1");
				System.out.println("2");
				""");
			expression.setToBeFormatted("""
				System.out.println("1");  System.out.println("2") ;\s\s
				""");
		});
	}

	@Test
	public void testMainAsBlock() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				{
					System.out.println("1");
					System.out.println("2");
				}
				""");
			expression.setToBeFormatted("""
				{   System.out.println("1");  System.out.println("2") ;   }
				""");
		});
	}

	@Test
	public void testFormatSystemOut() throws Exception {
		formatterTestHelper.assertFormatted(expression -> expression.setToBeFormatted("""
			System.out.println("Hello");
			"""));
	}

	@Test
	public void testFormatSystemOut2() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				System.out.println("Hello");
				""");
			expression.setToBeFormatted("""
				System.out.println( "Hello" ) ;
				""");
		});
	}

	@Test
	public void testVariableDeclaration() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				int i;
				""");
			expression.setToBeFormatted("""
				int  i ;
				""");
		});
	}

	@Test
	public void testVariableDeclarationWithInitialization() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				int i = 0;
				""");
			expression.setToBeFormatted("""
				int  i  =   0;
				""");
		});
	}

	@Test
	public void testVariableDeclarations() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				int i, j, k;
				""");
			expression.setToBeFormatted("""
				int  i , j , k ;
				""");
		});
	}

	@Test
	public void testVariableDeclarationsWithInitialization() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				int i = 0, j = 1, k = 2;
				""");
			expression.setToBeFormatted("""
				int  i  =  0  , j  =  1 , k  =  2 ;
				""");
		});
	}

	@Test
	public void testAssignment() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				i = 0;
				""");
			expression.setToBeFormatted("""
				i  =  0   ;
				""");
		});
	}

	@Test
	public void testAssignmentWithArrayAccess() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				i[0] = 0;
				""");
			expression.setToBeFormatted("""
				i [ 0 ]  =  0   ;
				""");
		});
	}

	@Test
	public void testAssignmentWithArrayAccesses() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				i[0][1] = 0;
				""");
			expression.setToBeFormatted("""
				i [ 0 ] [ 1 ] =  0   ;
				""");
		});
	}

	@Test
	public void testArrayAccesses() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				j = i[0][1];
				""");
			expression.setToBeFormatted("""
				j = i [ 0 ] [ 1 ]   ;
				""");
		});
	}

	@Test
	public void testMemberFeatureCallArrayAccesses() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				args[0].length;
				args[0][1].length;
				""");
			expression.setToBeFormatted("""
				args  [ 0 ]   . length   ;
				args  [ 0 ] [ 1 ]  . length   ;
				""");
		});
	}

	@Test
	public void testMethod() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				method m(int i, final String s) : int {
					return 0;
				}

				""");
			expression.setToBeFormatted("""
				method  m ( int  i ,  final  String  s )  :  int   {					return 0; 			}
				""");
		});
	}

	@Test
	public void testMethodWithComment() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				/**
				 * This is a comment
				 */
				method m(int i, final String s) : int {
					return 0;
				}

				""");
			expression.setToBeFormatted("""
				/**
				 * This is a comment
				 */
				method  m ( int  i ,  final  String  s ) : int  {					return 0; 			}
				""");
		});
	}

	@Test
	public void testOperation() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				op m(int i, final String s) : int {
					return 0;
				}

				""");
			expression.setToBeFormatted("""
				op  m ( int  i ,  final  String  s )  :  int   {					return 0; 			}
				""");
		});
	}

	@Test
	public void testOperationWithComment() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				/**
				 * This is a comment
				 */
				op m(int i, final String s) : int {
					return 0;
				}

				""");
			expression.setToBeFormatted("""
				/**
				 * This is a comment
				 */
				op  m ( int  i ,  final  String  s ) : int  {					return 0; 			}
				""");
		});
	}

	@Test
	public void testBinaryOperator() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				int j = i + 0 * 1;
				""");
			expression.setToBeFormatted("""
				int j  =   i  +0  *  1 ;
				""");
		});
	}

	@Test
	public void testFeatureCall() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				m(1 + 3, 2);
				""");
			expression.setToBeFormatted("""
				m (  1+3  ,   2  )  ;
				""");
		});
	}

	@Test
	public void testPrefixOperator() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				++i;
				""");
			expression.setToBeFormatted("""
				++  i  ;
				""");
		});
	}

	@Test
	public void testPostfixOperator() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				i++;
				""");
			expression.setToBeFormatted("""
				i  ++  ;
				""");
		});
	}

	@Test
	public void testCasted() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				(String) s;
				""");
			expression.setToBeFormatted("""
				(  String  )   s ;
				""");
		});
	}

	@Test
	public void testConstructorCall() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				new List<Integer, String>(1 + 3, 2);
				""");
			expression.setToBeFormatted("""
				new  List  < Integer  ,  String  >  (  1+3  ,   2  )  ;
				""");
		});
	}

	@Test
	public void testArrayConstructorCall() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				new List<Integer, String>[0][1];
				""");
			expression.setToBeFormatted("""
				new  List  < Integer  ,  String  >  [ 0 ] [  1  ]  ;
				""");
		});
	}

	@Test
	public void testArrayConstructorCallWithArrayLiteral() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				new List[1][] {0, 1, 1 + 2};
				new List[1][2] {0, 1, 1 + 2};
				new List[][2] {0, 1, 1 + 2};
				new List[][] {0, 1, 1 + 2};
				new List[][];
				""");
			expression.setToBeFormatted("""
				new  List    [  1  ] [    ]  {0 ,  1  ,  1+2 };
				new  List    [  1  ] [  2  ]  {0 ,  1  ,  1+2 };
				new  List    [    ] [  2  ]  {0 ,  1  ,  1+2 };
				new  List    [    ] [    ]  {0 ,  1  ,  1+2 };
				new  List    [    ] [    ]  ;
				""");
		});
	}

	@Test
	public void testArrayLiteral() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				int[] a = {0, 1, 1 + 2};
				int[][] b = {{0, 1}, 1 + 2};
				""");
			expression.setToBeFormatted("""
				int[] a = { 0 , 1 ,  1  +  2 } ;
				int[][] b = { { 0 ,  1 } , 1 + 2};
				""");
		});
	}

	@Test
	public void testBasicForLoop() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				for (int i = 0; i < argsNum; i += 1) {
					System.out.println("args[" + i + "] = " + args[i]);
				}
				""");
			expression.setToBeFormatted("""
				for  (int  i  =  0  ; i  <  argsNum ;  i  +=  1 )  {
					System.out.println ( "args[" + i + "] = " + args[i] );
				}
				""");
		});
	}

	@Test
	public void testForEachLoop() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				for (String s : strings)
					continue;
				for (String s : strings)
					System.out.println(s);
				for (String s : strings) {
					System.out.println(s);
				}
				for (final String s : strings) {
					System.out.println(s);
				}
				""");
			expression.setToBeFormatted("""
				for ( String  s  :  strings )
					continue ;
				for ( String  s  :  strings )
					System.out.println(s) ;
				for  ( String  s  :  strings )  {
					System.out.println ( s ) ;
				}
				for  (  final  String  s  :  strings )  {
					System.out.println ( s ) ;
				}
				""");
		});
	}

	@Test
	public void testBranchingStatements() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				continue;
				break;
				""");
			expression.setToBeFormatted("""
				continue ;  break   ;
				""");
		});
	}

	@Test
	public void testIfStatements() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				if (args.length() == 0)
					System.out.println("No args");
				else
					System.out.println("Args");
				if (args.length() == 0)
					System.out.println("No args");
				else
					System.out.println("Args");
				if (args.length() == 0) {
					System.out.println("No args");
				} else {
					System.out.println("Args");
				}
				if (args.length() == 0) {
					System.out.println("No args");
				} else if (args.length() == 0) {
					System.out.println("Args");
				}
				if (args.length() == 0) {
					System.out.println("No args");
				}
				""");
			expression.setToBeFormatted("""
				if  ( args.length( )  ==  0 )
					System.out.println ("No args") ;\s
				else 	System.out.println( "Args");
				if  ( args.length( )  ==  0 )
					System.out.println ("No args") ; 	else 	System.out.println( "Args");
				if  ( args.length( )  ==  0 )   {
					System.out.println ("No args");  }
				else 	{System.out.println( "Args"); }
				if  ( args.length( )  ==  0 )   {
					System.out.println ("No args");  }
				else  if  ( args.length( )  ==  0 ) 	{System.out.println( "Args"); }
				if  ( args.length( )  ==  0 )   {
					System.out.println ("No args");  }
				""");
		});
	}

	@Test
	public void testWhileStatements() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				while (args.length() == 0)
					System.out.println("No args");
				while (args.length() == 0) {
					System.out.println("No args");
				}
				""");
			expression.setToBeFormatted("""
				while  ( args.length( )  ==  0 )
					System.out.println ("No args") ;\s
				while ( args.length( )  ==  0 )   {
					System.out.println ("No args");  }
				""");
		});
	}

	@Test
	public void testDoWhileStatements() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				do
					System.out.println("No args");
				while (args.length() == 0);
				do {
					System.out.println("No args");
				} while (args.length() == 0);
				""");
			expression.setToBeFormatted("""
				do\s\s
					System.out.println ("No args") ;\s
				while  ( args.length( )  ==  0 ) ;
				do  {
					System.out.println ("No args") ;\s
				}   while  ( args.length( )  ==  0 ) ;
				""");
		});
	}

	@Test
	public void testConditionalExpression() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				int i = j > 0 ? 1 : '2';
				int j = ( j > 0 ? 1 : '2' );
				""");
			expression.setToBeFormatted("""
				int i =  j  >  0  ?  1  :  '2' ;
				int j =  ( j  >  0  ?  1  :  '2' ) ;
				""");
		});
	}

	@Test
	public void testSwitch() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				switch (argsNum) {
					case 0: System.out.println("0");
					default: System.out.println("default");
				}
				""");
			expression.setToBeFormatted("""
				switch  ( argsNum )  {
					case 0  :  System.out.println("0");
					default  :  System.out.println("default");
				}
				""");
		});
	}

	@Test
	public void testSwitchWithCascadeCases() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				switch (argsNum) {
					case 0:
					case 1:
					case 2: System.out.println("0");
					break;
					default: System.out.println("default");
				}
				""");
			expression.setToBeFormatted("""
				switch  ( argsNum )  {
					case 0  :
					case 1  :
					case 2  :  System.out.println("0") ;  break;
					default  :  System.out.println("default");
				}
				""");
		});
	}

	@Test
	public void testWildcard() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				List<? extends List> l;
				""");
			expression.setToBeFormatted("""
				List< ?  extends  List >  l ;
				""");
		});
	}

	@Test
	public void testInstanceOf() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				boolean b = d instanceof String;
				""");
			expression.setToBeFormatted("""
				boolean b = d  instanceof  String ;
				""");
		});
	}

	@Test
	public void testThrow() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				throw new Exception("test");
				""");
			expression.setToBeFormatted("""
				throw  new  Exception ( "test" );
				""");
		});
	}

	@Test
	public void testTryFinally() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				try {
					int i = 0;
				} finally {
					System.out.println("finally");
				}
				""");
			expression.setToBeFormatted("""
				try  {
				int i = 0;
				}  finally  {
					 System.out.println("finally");
				}
				""");
		});
	}

	@Test
	public void testTryCatch() throws Exception {
		// formatting in the presence of catch does not seem to work
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				try {
					int i = 0;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					System.out.println("finally");
				}
				""");
			expression.setToBeFormatted("""
				try  {
				int i = 0;
				}  catch  ( Exception  e ) {
					 e.printStackTrace();
				}  finally  {
					 System.out.println("finally");
				}
				""");
		});
	}

	@Test
	public void testTryWithResources() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				try (String s = ""; String s = "") {
					int i = 0;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					System.out.println("finally");
				}
				""");
			expression.setToBeFormatted("""
				try  ( String s = "" ; String s =  "" )  {
				int i = 0;
				}  catch  ( Exception  e ) {
					 e.printStackTrace();
				}  finally  {
					 System.out.println("finally");
				}
				""");
		});
	}

	@Test
	public void testSynchronized() throws Exception {
		// formatting in the presence of catch does not seem to work
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				Object o;
				synchronized (o) {
					o.wait();
				}
				""");
			expression.setToBeFormatted("""
				Object o;
				synchronized  ( o )  {
					 o.wait();
				}
				""");
		});
	}

	@Test
	public void testConstructor() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				new java.util.List<String>();
				""");
			expression.setToBeFormatted("""
				new  java.util.List < String > ( );
				""");
		});
	}

	@Test
	public void testDiamond() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				new java.util.List<>();
				""");
			expression.setToBeFormatted("""
				new  java.util.List <  > ( );
				""");
		});
	}

	@Test
	public void testConstructorWithoutTypeArguments() throws Exception {
		formatterTestHelper.assertFormatted(expression -> {
			expression.setExpectation("""
				new java.util.List();
				""");
			expression.setToBeFormatted("""
				new  java.util.List  ( );
				""");
		});
	}
}
