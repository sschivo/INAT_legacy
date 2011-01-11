/**
 * 
 */
package inat.analyzer.uppaal;

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
	 */
	public R analyse(String output);
}
