package jbase.ui.tests;

import jbase.testlanguage.ui.tests.JbaseTestlanguageUiInjectorProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.testing.AbstractContentAssistTest;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(JbaseTestlanguageUiInjectorProvider.class)
public class JbaseContentAssistTest extends AbstractContentAssistTest {

	@Test
	public void testKeywordInEmptyProgram() throws Exception {
		newBuilder().append("w").assertProposal("while");
	}

	@Test
	public void testKeywordContinue() throws Exception {
		newBuilder().append("c").assertProposal("continue");
	}

	@Test
	public void testKeywordBreak() throws Exception {
		newBuilder().append("b").assertProposal("break");
	}

	@Test
	public void testFinal() throws Exception {
		newBuilder().append("fi").assertProposal("final");
	}

	@Test
	public void testFinalParam() throws Exception {
		newBuilder().append("op m(fi").assertProposal("final");
	}

	@Test
	public void testArgsProposed() throws Exception {
		newBuilder().append("a").assertProposal("args");
	}

	@Test
	public void testArgsProposedInBlock() throws Exception {
		newBuilder().append("{ a").assertProposal("args");
	}

	@Test
	public void testArgsProposedAsExpression() throws Exception {
		newBuilder().append("while (").assertProposal("args");
	}

	@Test
	public void testVariableReferenceInAssignment() throws Exception {
		newBuilder().append("int myVar = 0; myVar = my").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInAssignment2() throws Exception {
		newBuilder().append("int myVar = 0; myVar = ").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInVariableDeclaration() throws Exception {
		newBuilder().append("int myVar = 0; int i = my").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInVariableDeclaration2() throws Exception {
		newBuilder().append("int myVar = 0; int i = ").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInVariableDeclarationInBlock() throws Exception {
		newBuilder().append("int myVar = 0; { int i = ").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInVariableDeclarationInBlock2() throws Exception {
		newBuilder().append("int myVar = 0; { int i = <|>; }").assertProposalAtCursor("myVar");
	}

	@Test
	public void testVariableReferenceInAssignmentInMethod() throws Exception {
		newBuilder().append("op foo() { int myVar = 0; myVar = my").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInAssignmentInMethod2() throws Exception {
		newBuilder().append("op foo() { int myVar = 0; myVar = ").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInVariableDeclarationInMethod() throws Exception {
		newBuilder().append("op foo() { int myVar = 0; int i = my").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInVariableDeclarationInMethod2() throws Exception {
		newBuilder().append("op foo() { int myVar = 0; int i = ").assertProposal("myVar");
	}

	@Test
	public void testMethodCallProposal() throws Exception {
		newBuilder().append("op myMeth() { return 0; } my").assertProposal("myMeth");
	}

	@Test
	public void testMethodCallProposalInBlock() throws Exception {
		newBuilder().append("op myMeth() { return 0; } { my").assertProposal("myMeth");
	}

	@Test
	public void testMethodCallProposalInExpressio() throws Exception {
		newBuilder().append("op myMeth() { return 0; } while(my").assertProposal("myMeth");
	}

	@Test
	public void testMemberFeatureCallProposalInMethod() throws Exception {
		newBuilder().append("op m() { String s = \"\"; s.to").assertProposal("toString");
	}

	@Test
	public void testMemberFeatureCallProposalInBlock() throws Exception {
		newBuilder().append("String s = \"\"; s.to").assertProposal("toString");
	}

	@Test
	public void testVariableReferenceInArrayAccess() throws Exception {
		newBuilder().append("int myVar = 0; int[] ar; ar[").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInArrayAccess2() throws Exception {
		newBuilder().append("int myVar = 0; int[] ar; ar[my<|>]").assertProposalAtCursor("myVar");
	}

	@Test
	public void testMemberFeatureCallOnArrayAccess() throws Exception {
		newBuilder().append("int[][] arr; arr[0].").assertProposal("toString");
	}

	@Test
	public void testMemberFeatureCallOnArrayAccess2() throws Exception {
		newBuilder().append("int[][] arr; arr[0].to").assertProposal("toString");
	}

	@Test
	public void testVariableReferenceInConditional() throws Exception {
		newBuilder().append("int myVar = 0; myVar = true ? my").assertProposal("myVar");
	}

	@Test
	public void testVariableReferenceInConditional2() throws Exception {
		newBuilder().append("int myVar = 0; myVar = true ? ").assertProposal("myVar");
	}

	@Test
	public void testForExpression() throws Exception {
		newBuilder().append("""
			java.util.List<String> strings;
			for (String e :\s\
			""").assertProposal("strings");
	}

	@Test
	public void testForExpression2() throws Exception {
		newBuilder().append("""
			java.util.List<String> strings;
			for (String e : s\
			""").assertProposal("strings");
	}

	@Test
	public void testCastType() throws Exception {
		newBuilder().append("(LinkedL").assertProposal("java.util.LinkedList");
	}

	@Test
	public void testProposalAndImportForListType() throws Exception {
		newBuilder().append("final LinkedL").applyProposal("LinkedList").expectContent("""
			import java.util.LinkedList;

			final LinkedList\
			""".replace("\n", System.lineSeparator()));
	}
}
