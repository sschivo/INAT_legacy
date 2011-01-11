/**
 * 
 */
package inat.analyzer;

import inat.model.Model;

/**
 * A model analyzer is responsible for analyzing the {@link Model}. After
 * analysis it will return a result.
 * 
 * @author B. Wanders
 * @param <R> the result type
 */
public interface ModelAnalyzer<R> {

	/**
	 * Analyzes the model and returns the result.
	 * 
	 * @param m the model to analyze
	 * @return the result of the analysis.
	 */
	public R analyze(Model m);
}
