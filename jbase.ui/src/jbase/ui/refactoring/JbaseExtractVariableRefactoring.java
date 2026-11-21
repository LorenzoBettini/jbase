package jbase.ui.refactoring;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.resource.ILocationInFileProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.refactoring.impl.EditorDocumentChange;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XConstructorCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XNullLiteral;
import org.eclipse.xtext.xbase.XPostfixOperation;
import org.eclipse.xtext.xbase.compiler.ISourceAppender;
import org.eclipse.xtext.xbase.typesystem.IBatchTypeResolver;
import org.eclipse.xtext.xbase.ui.document.DocumentRewriter;
import org.eclipse.xtext.xbase.ui.document.DocumentRewriter.Section;
import org.eclipse.xtext.xbase.ui.imports.ReplaceConverter;
import org.eclipse.xtext.xbase.ui.refactoring.ExpressionUtil;
import org.eclipse.xtext.xbase.ui.refactoring.ExtractVariableRefactoring;
import org.eclipse.xtext.xbase.ui.refactoring.NewFeatureNameUtil;

import com.google.inject.Inject;

import jbase.jbase.XJArrayLiteral;

/**
 * Extracts Java local declarations and optionally replaces similar Jbase expressions.
 *
 * @author Lorenzo Bettini
 */
public class JbaseExtractVariableRefactoring extends ExtractVariableRefactoring {
	@Inject private IBatchTypeResolver typeResolver;
	@Inject private ExpressionUtil expressionUtil;
	@Inject private ILocationInFileProvider locations;
	@Inject private DocumentRewriter.Factory rewriterFactory;
	@Inject private ReplaceConverter replaceConverter;
	@Inject private NewFeatureNameUtil nameUtil;

	private IXtextDocument document;
	private XtextEditor editor;
	private XExpression expression;
	private DocumentRewriter multiRewriter;
	private boolean useVar;
	private boolean replaceAll;
	private boolean replaceInFile;

	/** Captures the selected expression while Xbase prepares its single-occurrence path. */
	@Override
	public boolean initialize(final XtextEditor editor, final XExpression expression) {
		final var ready = super.initialize(editor, expression);
		this.editor = editor;
		this.document = editor.getDocument();
		this.expression = expression;
		return ready;
	}

	/** Selects Java local type inference when the expression supports it. */
	public void setUseVar(final boolean useVar) {
		this.useVar = useVar;
	}

	/** Whether the selected expression can initialize a Java {@code var} local. */
	public boolean canUseVar() {
		if (expression instanceof XNullLiteral || expression instanceof XClosure
				|| expression instanceof XJArrayLiteral) {
			return false;
		}
		final var type = typeResolver.resolveTypes(expression).getActualType(expression);
		return type.isValidHint();
	}

	/** Controls replacement of similar expressions in the selected executable. */
	public void setReplaceAll(final boolean replaceAll) {
		this.replaceAll = replaceAll;
	}

	/** Extends replacement to other executables in the current Jbase resource. */
	public void setReplaceInFile(final boolean replaceInFile) {
		this.replaceInFile = replaceInFile;
	}

	@Override
	protected void appendDeclaration(final ISourceAppender section,
			final ITextRegion expressionRegion) throws BadLocationException {
		final var type = typeResolver.resolveTypes(expression).getActualType(expression);
		section.append(isFinal() ? "final " : "");
		if (useVar) {
			section.append("var");
		} else {
			section.append(type);
		}
		section.append(" ").append(getVariableName()).append(" = ")
			.append(document.get(expressionRegion.getOffset(), expressionRegion.getLength()))
			.append(";");
	}

	@Override
	public RefactoringStatus checkFinalConditions(final IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		multiRewriter = null;
		if (useVar && !canUseVar()) {
			final var invalid = new RefactoringStatus();
			invalid.addFatalError("The selected expression cannot initialize a 'var' local");
			return invalid;
		}
		if (!replaceAll) {
			return super.checkFinalConditions(monitor);
		}
		final var matches = findMatches();
		if (matches.size() <= 1 || matches.getFirst().region().getOffset()
				== matches.getFirst().successorRegion().getOffset()) {
			return super.checkFinalConditions(monitor);
		}
		final var status = validateNewVariableName(getVariableName());
		if (!status.isOK()) {
			return status;
		}
		if (hasSideEffects(expression)) {
			status.addWarning("Replacing multiple occurrences may change when the expression runs");
		}
		multiRewriter = rewriterFactory.create(document, (XtextResource) expression.eResource());
		writeGroups(monitor, groupMatches(matches));
		return status;
	}

	private Map<EObject, List<Match>> groupMatches(final List<Match> matches) {
		final Map<EObject, List<Match>> groups = new LinkedHashMap<>();
		final var shareDeclaration = canShareDeclaration(expression);
		for (final var match : matches) {
			final EObject key = shareDeclaration
				&& match.successor().eContainer() instanceof XBlockExpression
				? match.successor().eContainer() : match.successor();
			groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(match);
		}
		return groups;
	}

	private void writeGroups(final IProgressMonitor monitor, final Map<EObject, List<Match>> groups)
			throws OperationCanceledException {
		var groupNumber = 0;
		for (final var group : groups.values()) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
			final var name = groupNumber++ == 0 ? getVariableName()
				: uniqueName(group.getFirst().successor(), groupNumber);
			writeGroup(group, name);
		}
	}

	private String uniqueName(final XExpression successor, final int start) {
		nameUtil.setFeatureScopeContext(successor);
		for (var suffix = start; ; suffix++) {
			final var candidate = getVariableName() + suffix;
			final var status = new RefactoringStatus();
			nameUtil.checkNewFeatureName(candidate, true, status);
			if (status.isOK()) {
				return candidate;
			}
		}
	}

	/** Finds equal expressions in the selected executable, or throughout this source file. */
	private List<Match> findMatches() {
		final var selectedText = sourceText(expression);
		final var selectedBody = enclosingBody(expression);
		final var candidates = new ArrayList<XExpression>();
		candidates.add(expression);
		for (final var candidate : EcoreUtil2.getAllContentsOfType(
				expression.eResource().getContents().getFirst(), XExpression.class)) {
			if (candidate != expression && candidate.eClass() == expression.eClass()
					&& (replaceInFile || enclosingBody(candidate) == selectedBody)
					&& selectedText.equals(sourceText(candidate))
					&& EcoreUtil.equals(expression, candidate)) {
				candidates.add(candidate);
			}
		}
		final var matches = new ArrayList<Match>();
		for (final var candidate : candidates) {
			final var successor = expressionUtil.findSuccessorExpressionForVariableDeclaration(candidate);
			if (successor != null) {
				final var region = locations.getFullTextRegion(candidate);
				final var successorRegion = locations.getFullTextRegion(successor);
				if (candidate == expression || successorRegion.getOffset() != region.getOffset()) {
					matches.add(new Match(candidate, successor, region, successorRegion));
				}
			}
		}
		return matches;
	}

	private EObject enclosingBody(final EObject object) {
		EObject body = object.eResource().getContents().getFirst();
		for (EObject current = object; current != null; current = current.eContainer()) {
			if (current instanceof XClosure || current instanceof XBlockExpression
					&& !(current.eContainer() instanceof XExpression)) {
				body = current;
				break;
			}
		}
		return body;
	}

	private String sourceText(final XExpression candidate) {
		final var region = locations.getFullTextRegion(candidate);
		return document.get().substring(region.getOffset(), region.getOffset() + region.getLength());
	}

	private boolean hasSideEffects(final XExpression selected) {
		return isSideEffecting(selected) || EcoreUtil2
			.getAllContentsOfType(selected, XExpression.class).stream().anyMatch(this::isSideEffecting);
	}

	private boolean isSideEffecting(final XExpression candidate) {
		return candidate instanceof XAssignment || candidate instanceof XPostfixOperation
			|| candidate instanceof XConstructorCall
			|| candidate instanceof XAbstractFeatureCall call
				&& call.getFeature() instanceof JvmOperation;
	}

	/** A shared declaration must not move a variable read across later statements. */
	private boolean canShareDeclaration(final XExpression candidate) {
		return !(candidate instanceof XAbstractFeatureCall)
			&& EcoreUtil2.getAllContentsOfType(candidate, XAbstractFeatureCall.class).isEmpty()
			&& !hasSideEffects(candidate);
	}

	/** Writes one declaration and replacements for expressions sharing a statement block. */
	private void writeGroup(final List<Match> group, final String name) {
		group.sort(Comparator.comparingInt(match -> match.region().getOffset()));
		final var first = group.getFirst();
		final var successor = first.successorRegion();
		final var inBlock = first.successor().eContainer() instanceof XBlockExpression;
		final var replacements = new LinkedHashMap<Match, Section>();
		for (final var match : group) {
			replacements.put(match,
				multiRewriter.newSection(match.region().getOffset(), match.region().getLength()));
		}
		final Section declaration;
		boolean sameLine = false;
		if (inBlock) {
			declaration = multiRewriter.newSection(successor.getOffset(), 0);
		} else {
			final var source = document.get();
			final var separator = multiRewriter.getLineSeparator();
			final var precedingSeparator = source.lastIndexOf(separator, successor.getOffset() - 1);
			final var lineStart = precedingSeparator < 0 ? 0
				: precedingSeparator + separator.length();
			sameLine = !source.substring(lineStart, successor.getOffset()).isBlank();
			final var insertionOffset = sameLine ? successor.getOffset()
				: Math.max(0, precedingSeparator);
			declaration = multiRewriter.newSection(insertionOffset, 0);
			if (!Character.isWhitespace(source.charAt(Math.max(0, insertionOffset - 1)))) {
				declaration.append(" ");
			}
			declaration.append("{").increaseIndentation().newLine();
			final var end = multiRewriter.newSection(successor.getOffset() + successor.getLength(),
				0, declaration.getBaseIndentLevel(), false);
			end.newLine().append("}");
		}
		final var type = typeResolver.resolveTypes(first.expression())
			.getActualType(first.expression());
		declaration.append(isFinal() ? "final " : "");
		if (useVar) {
			declaration.append("var");
		} else {
			declaration.append(type);
		}
		declaration.append(" ").append(name).append(" = ")
			.append(sourceText(first.expression())).append(";");
		if (inBlock || sameLine) {
			declaration.newLine();
		}
		for (final var section : replacements.values()) {
			section.append(name);
		}
	}

	@Override
	public Change createChange(final IProgressMonitor monitor)
			throws CoreException, OperationCanceledException {
		if (multiRewriter == null) {
			return super.createChange(monitor);
		}
		final var edit = replaceConverter.convertToTextEdit(multiRewriter.getChanges());
		final var change = new EditorDocumentChange("Extract local variable", editor, false);
		change.setEdit(edit);
		change.setTextType(expression.eResource().getURI().fileExtension());
		return change;
	}

	private record Match(XExpression expression, XExpression successor, ITextRegion region,
			ITextRegion successorRegion) { }
}
