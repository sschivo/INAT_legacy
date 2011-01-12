/**
 * 
 */
package inat.analyzer.uppaal;

import inat.analyzer.AnalysisException;

/**
 * The result analyser is responsible for the analysis of the UPPAAL trace or
 * verivication result.
 * 
 * @author B. Wanders
 * @param <R> the result type
 */
public interface ResultInterpreter<R> {
	/**
	 * Analyzes the UPPAAL output and converts it to a result.
	 * 
	 * @param output the UPPAAL output
	 * @return a result
	 * @throws AnalysisException if the analysis of the trace failed
	 */
	public R analyse(String output) throws AnalysisException;
}
