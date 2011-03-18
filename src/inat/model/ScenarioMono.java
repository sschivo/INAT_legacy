package inat.model;
import java.util.*;

public class ScenarioMono extends Scenario {
	protected HashMap<String, Double> parameters = new HashMap<String, Double>();
	
	public ScenarioMono() {
		setDefault();
	}
	
	public void setDefault() {
		setParameter("parameter", 0.1);
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
	protected int computeFormula(int reactantLevel, int nLevels) {
		double param = parameters.get("parameter");
		double rate = param * reactantLevel;
		if (rate != 0) {
			return Math.max(1, (int)Math.round(nLevels / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
		} else {
			return Scenario.INFINITE_TIME;
		}
	}

	public List<Integer> generateTimes(int dimension, int nLevels) {
		List<Integer> times = new LinkedList<Integer>();
		for (int i=0;i<dimension;i++) {
			times.add(computeFormula(i, nLevels));
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
