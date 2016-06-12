package jbase.tests.util;

import org.eclipse.xtext.junit4.smoketest.ScenarioProcessor;

public @interface ExampleAnnotation {

	/**
	 * The processor for this smoke test.
	 */
	Class<? extends ScenarioProcessor> value();

	/**
	 * If set to true, no permutations will be applied to the test data.
	 */
	boolean processCompleteInput() default false;

	/**
	 * If set to true, the methods of the test class will be processed in
	 * parallel
	 */
	boolean processInParallel() default false;
}
