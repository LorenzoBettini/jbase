package jbase.ui.refactoring.internal;

import jbase.ui.refactoring.JbaseExtractVariableRefactoring;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;

/**
 * JDT-style input dialog backed by Jbase's expression refactoring.
 *
 * @author Lorenzo Bettini
 */
public class JbaseExtractVariableWizard extends RefactoringWizard {
	private static final String PREFERENCES = "jbase.ui.extractLocalVariable";

	/** Creates the dialog for a prepared Jbase refactoring. */
	public JbaseExtractVariableWizard(final JbaseExtractVariableRefactoring refactoring) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle("Extract Local Variable");
	}

	@Override
	protected void addUserInputPages() {
		addPage(new InputPage((JbaseExtractVariableRefactoring) getRefactoring()));
	}

	@Override
	public String getWindowTitle() {
		return "Extract Local Variable";
	}

	/** Holds the four Java extraction options and validates the proposed name. */
	public static class InputPage extends UserInputWizardPage {
		private final JbaseExtractVariableRefactoring refactoring;
		private Text nameField;
		private Button replaceAll;
		private Button replaceInFile;
		private Button declareFinal;
		private Button useVar;

		/** Creates a page for the selected expression. */
		public InputPage(final JbaseExtractVariableRefactoring refactoring) {
			super("JbaseExtractLocalVariableInputPage");
			this.refactoring = refactoring;
			setTitle("Extract Local Variable");
			setDescription("Enter a name for the new local variable");
		}

		@Override
		public void createControl(final Composite parent) {
			final var preferences = InstanceScope.INSTANCE.getNode(PREFERENCES);
			final var composite = new Composite(parent, SWT.NONE);
			final var layout = new GridLayout(2, false);
			layout.verticalSpacing = 8;
			composite.setLayout(layout);
			composite.setFont(parent.getFont());
			final var label = new Label(composite, SWT.NONE);
			label.setText("&Variable name:");
			nameField = new Text(composite, SWT.BORDER);
			nameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			nameField.setText(refactoring.getVariableName());
			nameField.selectAll();
			nameField.addModifyListener(event -> validateName());

			replaceAll = checkbox(composite,
				"&Replace all occurrences of the selected expression with references "
					+ "to the local variable", preferences.getBoolean("replaceAll", true));
			replaceInFile = checkbox(composite,
				"&Extract similar expression in all methods",
				preferences.getBoolean("replaceInFile", false));
			final var indent = (GridData) replaceInFile.getLayoutData();
			indent.horizontalIndent = 20;
			replaceInFile.setEnabled(replaceAll.getSelection());
			replaceAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					replaceInFile.setEnabled(replaceAll.getSelection());
					if (!replaceAll.getSelection()) {
						replaceInFile.setSelection(false);
					}
				}
			});
			declareFinal = checkbox(composite, "&Declare the local variable as 'final'",
				preferences.getBoolean("declareFinal", false));
			useVar = checkbox(composite, "Declare the local variable &type as 'var'",
				preferences.getBoolean("useVar", false));
			useVar.setEnabled(refactoring.canUseVar());
			if (!useVar.isEnabled()) {
				useVar.setSelection(false);
			}
			setControl(composite);
			Dialog.applyDialogFont(composite);
			validateName();
		}

		private Button checkbox(final Composite parent, final String text,
				final boolean selected) {
			final var button = new Button(parent, SWT.CHECK);
			button.setText(text);
			button.setSelection(selected);
			button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			return button;
		}

		private void validateName() {
			setPageComplete(refactoring.validateNewVariableName(nameField.getText()));
		}

		private void updateRefactoring() {
			refactoring.setVariableName(nameField.getText());
			refactoring.setFinal(declareFinal.getSelection());
			refactoring.setUseVar(useVar.getSelection());
			refactoring.setReplaceAll(replaceAll.getSelection());
			refactoring.setReplaceInFile(replaceInFile.getSelection());
		}

		@Override
		protected boolean performFinish() {
			updateRefactoring();
			final var preferences = InstanceScope.INSTANCE.getNode(PREFERENCES);
			preferences.putBoolean("replaceAll", replaceAll.getSelection());
			preferences.putBoolean("replaceInFile", replaceInFile.getSelection());
			preferences.putBoolean("declareFinal", declareFinal.getSelection());
			preferences.putBoolean("useVar", useVar.getSelection());
			try {
				preferences.flush();
			} catch (BackingStoreException e) {
				// The choice still applies to this refactoring when preferences cannot be saved.
			}
			return super.performFinish();
		}

		@Override
		public IWizardPage getNextPage() {
			updateRefactoring();
			return super.getNextPage();
		}

		@Override
		public void setVisible(final boolean visible) {
			if (visible) {
				nameField.setFocus();
			}
			super.setVisible(visible);
		}
	}
}
