/**
 * 
 */
package inat.analyzer.uppaal;

import inat.analyzer.ModelAnalyzer;
import inat.model.Model;
import inat.analyzer.LevelResult;

/**
 * The UPPAAL concentration level analyzer.
 * 
 * @author B. Wanders
 */
public class UppaalModelAnalyzer implements ModelAnalyzer<LevelResult> {
	/**
	 * The transformer that should be used for the given UPPAAL model.
	 */
	private final ModelTransformer transformer;
	/**
	 * The result analyser that is going to analyze the UPPAAL output.
	 */
	private final ResultInterpreter<LevelResult> resultAnalyzer;

	/**
	 * Constructor.
	 * 
	 * @param resultAnalyzer the analyzer to use when interpreting the UPPAAL
	 *            output
	 * @param transformer the model transformer to use when transforming the
	 *            {@link Model} to an UPPAAL model
	 */
	public UppaalModelAnalyzer(ResultInterpreter<LevelResult> resultAnalyzer, ModelTransformer transformer) {
		super();
		this.resultAnalyzer = resultAnalyzer;
		this.transformer = transformer;
	}

	@Override
	public LevelResult analyze(Model m) {
		// create UPPAAL model
		final String uppaalModel = this.transformer.transform(m);
		// create UPPAAL query
		final String uppaalQuery = "E<> (globalTime > " + 0 + ")";

		// do low-level I/O UPPAAL interaction
		// FIXME abstract UPPAAL interaction into its own class
		String output = generateTrace(uppaalModel, uppaalQuery);

		// analyze the resulting trace
		LevelResult result = this.resultAnalyzer.analyse(output);

		return result;
	}

	private String generateTrace(String uppaalModel, String uppaalQuery) {
		// FIXME: invoke UPPAAL tooling
		// TODO Auto-generated method stub
		return "FAIL!";
	}
}
