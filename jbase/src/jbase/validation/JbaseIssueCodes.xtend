package jbase.validation

class JbaseIssueCodes {
	public static val PREFIX = "jbase."

	public static val NOT_ARRAY_TYPE = PREFIX + "NotArrayType"
	public static val INVALID_BRANCHING_STATEMENT = PREFIX + "InvalidBranchingStatement"
	public static val MISSING_SEMICOLON = PREFIX + "MissingSemicolon"
	public static val MISSING_PARENTHESES = PREFIX + "MissingParentheses"
	public static val MISSING_DEFAULT = PREFIX + "MissingDefault"
	public static val DUPLICATE_METHOD = PREFIX + "DuplicateMethod"
	public static val ARRAY_CONSTRUCTOR_EITHER_DIMENSION_EXPRESSION_OR_INITIALIZER = PREFIX +
		"ArrayConstructorEitherDimensionExpressionOrInitializer"
	public static val ARRAY_CONSTRUCTOR_BOTH_DIMENSION_EXPRESSION_AND_INITIALIZER = PREFIX +
		"ArrayConstructorBothDimensionExpressionAndInitializer"
	public static val ARRAY_CONSTRUCTOR_DIMENSION_EXPRESSION_AFTER_EMPTY_DIMENSION = PREFIX +
		"ArrayConstructorDimensionExpressionAfterEmptyExpression"
	public static val INVALID_USE_OF_VAR_ARGS = PREFIX + "InvalidUseOfVarArgs"
	public static val INVALID_CLASS_OBJECT_EXPRESSION = PREFIX + "InvalidClassObjectExpression"
	public static val INCOMPLETE_CLASS_OBJECT = PREFIX + "IncompleteClassObject"
	public static val MISSING_RETURN = PREFIX + "MissingReturn"
	public static val INVALID_CHARACTER_CONSTANT = PREFIX + "InvalidCharacterConstant"
	public static val NOT_INITIALIZED_VARIABLE = PREFIX + "NotInitializedVariable"
	public static val MISSING_RESOURCES = PREFIX + "MissingResources"
	public static val NOT_AUTO_CLOSEABLE = PREFIX + "NotAutoCloseable"

	protected new() {
	}
}