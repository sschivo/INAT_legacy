/**
 * 
 */
package inat.analyzer.uppaal;

import java.io.IOException;

import inat.analyzer.AnalysisException;
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
	private final ResultInterpreter<LevelResult> resultInterpreter;

	/**
	 * Constructor.
	 * 
	 * @param resultInterpreter the analyzer to use when interpreting the UPPAAL
	 *            output
	 * @param transformer the model transformer to use when transforming the
	 *            {@link Model} to an UPPAAL model
	 */
	public UppaalModelAnalyzer(ResultInterpreter<LevelResult> resultInterpreter, ModelTransformer transformer) {
		super();
		this.resultInterpreter = resultInterpreter;
		this.transformer = transformer;
	}

	@Override
	public LevelResult analyze(Model m) throws AnalysisException {
		// create UPPAAL model
		final String uppaalModel = this.transformer.transform(m);
		// create UPPAAL query
		final String uppaalQuery = "E<> (globalTime > " + 500 + ")";

		// do low-level I/O UPPAAL interaction
		UppaalInvoker invoker = new UppaalInvoker();
		String output;

		try {
			output = invoker.trace(uppaalModel, uppaalQuery);
		} catch (IOException e) {
			throw new AnalysisException("The analysis failed due to an I/O exception while invoking UPPAAL.", e);
		} catch (InterruptedException e) {
			throw new AnalysisException(
					"The analysis failed due to the analysis being interrupted while waiting for UPPAAL.", e);
		}

		// if the ouput is null, we have no trace
		if (output != null) {
			// interpret the resulting trace
			LevelResult result = this.resultInterpreter.analyse(m, output);

			return result;
		} else {
			return null;
		}
	}
}
