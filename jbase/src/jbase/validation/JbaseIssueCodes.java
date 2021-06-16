package jbase.validation;

public class JbaseIssueCodes {
	private JbaseIssueCodes() {
	}

	public static final String PREFIX = "jbase.";

	public static final String NOT_ARRAY_TYPE = PREFIX + "NotArrayType";

	public static final String INVALID_BRANCHING_STATEMENT = PREFIX + "InvalidBranchingStatement";

	public static final String MISSING_SEMICOLON = PREFIX + "MissingSemicolon";

	public static final String MISSING_PARENTHESES = PREFIX + "MissingParentheses";

	public static final String MISSING_DEFAULT = PREFIX + "MissingDefault";

	public static final String DUPLICATE_METHOD = PREFIX + "DuplicateMethod";

	public static final String ARRAY_CONSTRUCTOR_EITHER_DIMENSION_EXPRESSION_OR_INITIALIZER = PREFIX
			+ "ArrayConstructorEitherDimensionExpressionOrInitializer";

	public static final String ARRAY_CONSTRUCTOR_BOTH_DIMENSION_EXPRESSION_AND_INITIALIZER = PREFIX
			+ "ArrayConstructorBothDimensionExpressionAndInitializer";

	public static final String ARRAY_CONSTRUCTOR_DIMENSION_EXPRESSION_AFTER_EMPTY_DIMENSION = PREFIX
			+ "ArrayConstructorDimensionExpressionAfterEmptyExpression";

	public static final String INVALID_USE_OF_VAR_ARGS = PREFIX + "InvalidUseOfVarArgs";

	public static final String INVALID_CLASS_OBJECT_EXPRESSION = PREFIX
			+ "InvalidClassObjectExpression";

	public static final String INCOMPLETE_CLASS_OBJECT = PREFIX + "IncompleteClassObject";

	public static final String MISSING_RETURN = PREFIX + "MissingReturn";

	public static final String INVALID_CHARACTER_CONSTANT = PREFIX + "InvalidCharacterConstant";

	public static final String NOT_INITIALIZED_VARIABLE = PREFIX + "NotInitializedVariable";

	public static final String MISSING_RESOURCES = PREFIX + "MissingResources";

	public static final String NOT_AUTO_CLOSEABLE = PREFIX + "NotAutoCloseable";

}
