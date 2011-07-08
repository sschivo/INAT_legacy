package inat.model;
import java.util.*;

/**
 * A single-reactant version of Scenario.
 * It has only one scenario, with one parameter.
 */
public class ScenarioMono extends Scenario {
	private static final String SCENARIO_ONLY_PARAMETER = Model.Properties.SCENARIO_ONLY_PARAMETER;
	protected HashMap<String, Double> parameters = new HashMap<String, Double>();
	
	public ScenarioMono() {
		setDefault();
	}
	
	public void setDefault() {
		setParameter(SCENARIO_ONLY_PARAMETER, 0.1);
	}
		
	public void setParameter(String name, Double value) {
		parameters.put(name, value);
	}
	
	public Double getParameter(String name) {
		return parameters.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, Double> getParameters() {
		return (HashMap<String, Double>)parameters.clone();
	}
	
	//If you want, you can extend Scenario modifying computeFormula in any way you like.
	//We advise to call super.computeFormula at the end of the extended method, as this
	//is the "official" complete formula.
	protected int computeFormula(int reactantLevel) {
		double param = parameters.get(SCENARIO_ONLY_PARAMETER);
		double rate = param * reactantLevel;
		if (rate > 1e-8) {
			return Math.max(1, (int)Math.round(1 / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
		} else {
			return Scenario.INFINITE_TIME;
		}
	}

	public List<Integer> generateTimes(int nLevels) {
		List<Integer> times = new LinkedList<Integer>();
		times.add(INFINITE_TIME); //if reactant is already completely inactive, no reaction
		for (int i=1;i<nLevels;i++) {
			times.add(computeFormula(i));
		}
		return times;
	}
	
	public String[] listVariableParameters() {
		return parameters.keySet().toArray(new String[1]);
	}
	
	public String toString() {
		return "Scenario";
	}
}
