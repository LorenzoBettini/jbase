# Extract Local Variable in Eclipse

Jbase editors provide a Java-style **Extract Local Variable** dialog. It accepts a local
variable name and offers the same choices as JDT: replace all occurrences of the selected
expression, extract similar expressions in all methods in the current file, declare the local
as `final`, and use `var` for the local type. The dialog remembers choices between uses.
Its initial choices match JDT: replace all is selected; the other choices are clear.

`JbaseExtractVariableHandler` and `JbaseExtractVariableWizard` in
`jbase.ui.refactoring.internal` own the Eclipse command and dialog. Xtext's handler
constructs its Xbase wizard directly, so Jbase binds a handler that constructs this wizard.
The UI classes are excluded from aggregate coverage and Sonar coverage because their
workbench lifecycle is difficult to measure reliably; the behavior is checked by
`JbaseExtractVariableTest` in `jbase.ui.tests`.

`JbaseExtractVariableRefactoring` in `jbase.ui.refactoring` generates Java declarations.
It uses the Xbase single-selection change when there are no additional matches. For
multiple matches, it finds expressions with the same source text and EMF structure.
By default it searches the selected executable. The file-wide option searches all
executables in the current resource, creating a separate local in each affected block.
Expressions that read mutable state receive separate declarations at each statement;
replacing calls or other expressions with side effects produces a warning. `var` is
unavailable when the selected expression has no standalone inferred type, such as `null`,
a lambda, a method reference, or an array initializer.

`JbaseExtractVariableTest` covers generated edits, option behavior, the handler binding,
and dialog labels. It also guards against regressions in Java type rendering and new-block
insertion.
