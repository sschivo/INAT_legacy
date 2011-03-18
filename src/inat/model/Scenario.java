package inat.model;
import java.util.*;

public class Scenario {
	protected HashMap<String, Double> parameters = new HashMap<String, Double>();
	public static final int INFINITE_TIME = -1;
	
	public Scenario() {
	}
	
	@SuppressWarnings("unchecked")
	public Scenario(Scenario source) {
		this.parameters = (HashMap<String, Double>)source.parameters.clone();
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
	protected int computeFormula(int r1Level, int r2Level) {
		double k2 = parameters.get("k2"),
			   E = r1Level,
			   km = parameters.get("km"),
			   Stot = parameters.get("Stot"),
			   S = Stot - r2Level;
		double rate = k2 * E * S / (km + S);
		if (rate != 0) {
			return Math.max(1, (int)Math.round(1 / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
		} else {
			return INFINITE_TIME;
		}
	}

	public List<Integer> generateTimes(int dimension) {
		List<Integer> times = new LinkedList<Integer>();
		for (int i=0;i<dimension;i++) {
			for (int j=0;j<dimension;j++) {
				times.add(computeFormula(j, i));
			}
		}
		return times;
	}
	
	public String[] listVariableParameters() {
		return new String[]{};
	}
	
}
