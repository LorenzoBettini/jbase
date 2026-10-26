package jbase.tests;

import com.google.inject.Inject;
import jbase.formatting2.JbaseFormatter;
import jbase.testlanguage.tests.JbaseTestlanguageInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.formatter.FormatterTestHelper;
import org.eclipse.xtext.testing.formatter.FormatterTestRequest;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageInjectorProvider.class)
public class JbaseFormatterTest extends JbaseAbstractTest {

	@Inject
	private FormatterTestHelper formatterTestHelper;

	@Inject
	private JbaseFormatter jbaseFormatter;

	@Test
	public void testDispatchWithNull() { // NOSONAR: just testing that the formatter does not throw an exception when called with null
		jbaseFormatter.format(null, null);
	}

	@Test
	public void testAnonymousClasses() {
		assertFormatted(request -> {
			request.setToBeFormatted("var value=new Object(){n:int method get():int{return n;}};");
			request.setExpectation("""
				var value = new Object() {
					n:int
					method get() : int {
						return n;
					}
				};
				""");
		});
		assertFormatted(request -> {
			request.setToBeFormatted("Object value=new Object(){};");
			request.setExpectation("""
				Object value = new Object() {
				};
				""");
		});
	}

	@Test
	public void testQualifiedInnerClassCreation() {
		assertFormatted(request -> {
			request.setToBeFormatted("Object value=outer.new Inner();");
			request.setExpectation("Object value = outer.new Inner();\n");
		});
	}

	@Test
	public void testEnhancedSwitch() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				Object value="hello";
				int n=switch(value){case final String s when s.length()>1->{yield s.length();}
				case null,default->0;};
				switch(n){case 0,1->System.out.println(n);default->{System.out.println("other");}}
				""");
			request.setExpectation("""
				Object value = "hello";
				int n = switch (value) {
					case final String s when s.length() > 1 -> {
						yield s.length();
					}
					case null, default -> 0;
				};
				switch (n) {
					case 0, 1 -> System.out.println(n);
					default -> {
						System.out.println("other");
					}
				}
				""");
		});
	}

	@Test
	public void testTryWithExistingVariableResource() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				var input=new java.io.ByteArrayInputStream(new byte[0]);try(input){}
				""");
			request.setExpectation("""
				var input = new java.io.ByteArrayInputStream(new byte[0]);
				try (input) {
				}
				""");
		});
	}

	@Test
	public void testMixedTryWithResources() {
		assertFormatted(request -> {
				request.setToBeFormatted("""
					var input = new java.io.StringReader("a");try(input;var second=new java.io.StringReader("b")){}
					""");
				request.setExpectation("""
					var input = new java.io.StringReader("a");
					try (input; var second = new java.io.StringReader("b")) {
					}
					""");
		});
	}

	@Test
	public void testColonSwitchExpression() {
		assertFormatted(request -> {
			request.setToBeFormatted("int n=switch(1){case 0,1:yield 1;default:yield 2;};");
			request.setExpectation("""
				int n = switch (1) {
					case 0, 1:
					yield 1;
					default:
					yield 2;
				};
				""");
		});
	}

	@Test
	public void testInstanceOfPatterns() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				Object value="hello";
				if(value instanceof final  String   text){System.out.println(text);}
				""");
			request.setExpectation("""
				Object value = "hello";
				if (value instanceof final String text) {
					System.out.println(text);
				}
				""");
		});
	}

	@Test
	public void testVarLambdaParameters() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				java.util.function.IntBinaryOperator sum=(final  var  x , var  y)->x+y;
				""");
			request.setExpectation("""
				java.util.function.IntBinaryOperator sum = (final var x, var y) -> x + y;
				""");
		});
	}

	@Test
	public void testSourceAnnotations() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				import jbase.tests.util.SourceAnnotation;
				import java.util.Map;
				import java.util.Map.Entry;
				import java.util.function.IntUnaryOperator;
				method annotated(@SourceAnnotation String parameter):void{}
				@SourceAnnotation int local=1;
				@SourceAnnotation var inferred=1;
				@SourceAnnotation final String text="text";
				java.util.List<@SourceAnnotation ? extends String> values=null;
				Map.@SourceAnnotation Entry<String,String> entry=null;
				IntUnaryOperator increment=(@SourceAnnotation var value)->value+1;
				IntUnaryOperator identity=(@SourceAnnotation final int value)->value;
				""");
			request.setExpectation("""
				import jbase.tests.util.SourceAnnotation;
				import java.util.Map;
				import java.util.Map.Entry;
				import java.util.function.IntUnaryOperator;

				method annotated(@SourceAnnotation String parameter) : void {
				}

				@SourceAnnotation int local = 1;
				@SourceAnnotation var inferred = 1;
				@SourceAnnotation final String text = "text";
				java.util.List<@SourceAnnotation ? extends String> values = null;
				Map.@SourceAnnotation Entry<String, String> entry = null;
				IntUnaryOperator increment = (@SourceAnnotation var value) -> value + 1;
				IntUnaryOperator identity = (@SourceAnnotation final int value) -> value;
				""");
		});
	}

	@Test
	public void testLambdaExpressions() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				java.util.function.BiFunction<String, String, String> join=( String a ,String b )->a+b;
				Runnable r=()->{System.out.println("hello");};
				""");
			request.setExpectation("""
				java.util.function.BiFunction<String, String, String> join = (String a, String b) -> a + b;
				Runnable r = () -> {
					System.out.println("hello");
				};
				""");
		});
	}

	/**
	 * Java text blocks use LF, while the formatter uses the platform line separator.
	 */
	private void assertFormatted(final Procedure1<FormatterTestRequest> initializer) {
		formatterTestHelper.assertFormatted(request -> {
			initializer.apply(request);
			request.setExpectation(request.getExpectationOrToBeFormatted().toString()
					.replace("\n", System.lineSeparator()));
			request.setToBeFormatted(request.getToBeFormatted().toString()
					.replace("\n", System.lineSeparator()));
		});
	}

	@Test
	public void testProgramSpaces()  {
		assertFormatted(expression -> {
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
	public void testProgramSpacesWithOnlyMain()  {
		assertFormatted(expression -> {
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
	public void testProgramSpacesWithOnlyImports()  {
		assertFormatted(expression -> {
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
	public void testFormatImportSection()  {
		assertFormatted(expression -> {
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
	public void testSemicolonInSingleStatements()  {
		assertFormatted(expression -> {
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
	public void testSemicolonInSingleStatements2()  {
		assertFormatted(expression -> {
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
	public void testAssertStatements() {
		assertFormatted(expression -> {
			expression.setToBeFormatted("""
				assert   condition ;
				assert condition:"message";
				""");
			expression.setExpectation("""
				assert condition;
				assert condition : "message";
				""");
		});
	}

	@Test
	public void testEmptyStatements() {
		assertFormatted(expression -> {
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
	public void testFormatBlock() {
		assertFormatted(expression -> {
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
	public void testMain() {
		assertFormatted(expression -> {
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
	public void testMainAsBlock() {
		assertFormatted(expression -> {
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
	public void testFormatSystemOut() {
		assertFormatted(expression -> expression.setToBeFormatted("""
			System.out.println("Hello");
			"""));
	}

	@Test
	public void testFormatSystemOut2() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				System.out.println("Hello");
				""");
			expression.setToBeFormatted("""
				System.out.println( "Hello" ) ;
				""");
		});
	}

	@Test
	public void testVarDeclarations() {
		assertFormatted(request -> {
			request.setToBeFormatted("""
				var  value=1;final   var  text= "hello";
				for(final  var  item:new String[]{text}){System.out.println(item);}
				try(final  var  input=new java.io.StringReader(text)){input.read();}
				""");
			request.setExpectation("""
				var value = 1;
				final var text = "hello";
				for (final var item : new String[] {text}) {
					System.out.println(item);
				}
				try (final var input = new java.io.StringReader(text)) {
					input.read();
				}
				""");
		});
	}

	@Test
	public void testVarIdentifier() {
		assertFormatted(request -> {
			request.setToBeFormatted("int  var ; var  value=1; final   var var=2;");
			request.setExpectation("int var;\nvar value = 1;\nfinal var var = 2;\n");
		});
	}

	@Test
	public void testVariableDeclaration() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				int i;
				""");
			expression.setToBeFormatted("""
				int  i ;
				""");
		});
	}

	@Test
	public void testVariableDeclarationWithInitialization() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				int i = 0;
				""");
			expression.setToBeFormatted("""
				int  i  =   0;
				""");
		});
	}

	@Test
	public void testVariableDeclarations() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				int i, j, k;
				""");
			expression.setToBeFormatted("""
				int  i , j , k ;
				""");
		});
	}

	@Test
	public void testVariableDeclarationsWithInitialization() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				int i = 0, j = 1, k = 2;
				""");
			expression.setToBeFormatted("""
				int  i  =  0  , j  =  1 , k  =  2 ;
				""");
		});
	}

	@Test
	public void testAssignment() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				i = 0;
				""");
			expression.setToBeFormatted("""
				i  =  0   ;
				""");
		});
	}

	@Test
	public void testAssignmentWithArrayAccess() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				i[0] = 0;
				""");
			expression.setToBeFormatted("""
				i [ 0 ]  =  0   ;
				""");
		});
	}

	@Test
	public void testAssignmentWithArrayAccesses() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				i[0][1] = 0;
				""");
			expression.setToBeFormatted("""
				i [ 0 ] [ 1 ] =  0   ;
				""");
		});
	}

	@Test
	public void testArrayAccesses() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				j = i[0][1];
				""");
			expression.setToBeFormatted("""
				j = i [ 0 ] [ 1 ]   ;
				""");
		});
	}

	@Test
	public void testMemberFeatureCallArrayAccesses() {
		assertFormatted(expression -> {
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
	public void testMethod() {
		assertFormatted(expression -> {
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
	public void testMethodWithComment() {
		assertFormatted(expression -> {
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
	public void testOperation() {
		assertFormatted(expression -> {
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
	public void testOperationWithComment() {
		assertFormatted(expression -> {
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
	public void testBinaryOperator() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				int j = i + 0 * 1;
				""");
			expression.setToBeFormatted("""
				int j  =   i  +0  *  1 ;
				""");
		});
	}

	@Test
	public void testFeatureCall() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				m(1 + 3, 2);
				""");
			expression.setToBeFormatted("""
				m (  1+3  ,   2  )  ;
				""");
		});
	}

	@Test
	public void testPrefixOperator() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				++i;
				""");
			expression.setToBeFormatted("""
				++  i  ;
				""");
		});
	}

	@Test
	public void testPostfixOperator()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				i++;
				""");
			expression.setToBeFormatted("""
				i  ++  ;
				""");
		});
	}

	@Test
	public void testCasted()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				(String) s;
				""");
			expression.setToBeFormatted("""
				(  String  )   s ;
				""");
		});
	}

	@Test
	public void testConstructorCall()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				new List<Integer, String>(1 + 3, 2);
				""");
			expression.setToBeFormatted("""
				new  List  < Integer  ,  String  >  (  1+3  ,   2  )  ;
				""");
		});
	}

	@Test
	public void testArrayConstructorCall()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				new List<Integer, String>[0][1];
				""");
			expression.setToBeFormatted("""
				new  List  < Integer  ,  String  >  [ 0 ] [  1  ]  ;
				""");
		});
	}

	@Test
	public void testArrayConstructorCallWithArrayLiteral()  {
		assertFormatted(expression -> {
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
	public void testArrayLiteral()  {
		assertFormatted(expression -> {
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
	public void testBasicForLoop()  {
		assertFormatted(expression -> {
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
	public void testForEachLoop()  {
		assertFormatted(expression -> {
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
	public void testBranchingStatements()  {
		assertFormatted(expression -> {
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
	public void testLabeledStatementsAndBranches() {
		assertFormatted(expression -> {
			expression.setExpectation("""
				exit: {
					break exit;
				}
				outer: while (true)
					continue outer;
				""");
			expression.setToBeFormatted("""
				exit : { break   exit ; }
				outer:while (true) continue   outer ;
				""");
		});
	}

	@Test
	public void testIfStatements()  {
		assertFormatted(expression -> {
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
	public void testWhileStatements()  {
		assertFormatted(expression -> {
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
	public void testDoWhileStatements()  {
		assertFormatted(expression -> {
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
	public void testConditionalExpression()  {
		assertFormatted(expression -> {
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
	public void testSwitch()  {
		assertFormatted(expression -> {
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
	public void testSwitchWithCascadeCases()  {
		assertFormatted(expression -> {
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
	public void testWildcard()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				List<? extends List> l;
				""");
			expression.setToBeFormatted("""
				List< ?  extends  List >  l ;
				""");
		});
	}

	@Test
	public void testInstanceOf()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				boolean b = d instanceof String;
				""");
			expression.setToBeFormatted("""
				boolean b = d  instanceof  String ;
				""");
		});
	}

	@Test
	public void testThrow()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				throw new Exception("test");
				""");
			expression.setToBeFormatted("""
				throw  new  Exception ( "test" );
				""");
		});
	}

	@Test
	public void testTryFinally()  {
		assertFormatted(expression -> {
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
	public void testTryCatch()  {
		// formatting in the presence of catch does not seem to work
		assertFormatted(expression -> {
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
	public void testTryWithResources()  {
		assertFormatted(expression -> {
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
	public void testSynchronized()  {
		// formatting in the presence of catch does not seem to work
		assertFormatted(expression -> {
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
	public void testConstructor()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				new java.util.List<String>();
				""");
			expression.setToBeFormatted("""
				new  java.util.List < String > ( );
				""");
		});
	}

	@Test
	public void testDiamond()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				new java.util.List<>();
				""");
			expression.setToBeFormatted("""
				new  java.util.List <  > ( );
				""");
		});
	}

	@Test
	public void testConstructorWithoutTypeArguments()  {
		assertFormatted(expression -> {
			expression.setExpectation("""
				new java.util.List();
				""");
			expression.setToBeFormatted("""
				new  java.util.List  ( );
				""");
		});
	}
}
