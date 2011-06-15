package inat.model;
import java.util.*;

public class Scenario {
	protected HashMap<String, Double> parameters = new HashMap<String, Double>();
	public static final int INFINITE_TIME = -1;
	
	public static final Scenario[] sixScenarios = new Scenario[3];
	
	static {
		sixScenarios[0] = new Scenario() {
			public int computeFormula(int r1Level, int r2Level) {
				double par = parameters.get("parameter"),
					   E = r1Level;
				double rate = par * E;
				if (rate != 0) {
					return Math.max(1, (int)Math.round(1 / rate));
				} else {
					return Scenario.INFINITE_TIME;
				}
			}
			
			public String[] listVariableParameters() {
				return new String[]{"parameter"};
			}
			
			public String toString() {
				return "Scenario 1-2-3-4";
			}
		};
		sixScenarios[0].setParameter("parameter", 0.01);
		
		sixScenarios[1] = new Scenario() {
			public int computeFormula(int r1Level, int r2Level) {
				double par1 = parameters.get("k2/km"),
					   Stot = parameters.get("Stot"),
					   S = Stot - r2Level,
					   E = r1Level;
				double rate = par1 * E * S;
				if (rate != 0) {
					return Math.max(1, (int)Math.round(1 / rate));
				} else {
					return Scenario.INFINITE_TIME;
				}
			}
			
			public String[] listVariableParameters() {
				return new String[]{"k2/km", "Stot"};
			}
			
			public String toString() {
				return "Scenario 5";
			}
		};
		sixScenarios[1].setParameter("k2/km", 0.001);
		sixScenarios[1].setParameter("Stot", 15.0);
		
		sixScenarios[2] = new Scenario() {
			public int computeFormula(int r1Level, int r2Level) {
				double k2 = parameters.get("k2"),
					   E = (double)r1Level,
					   Stot = parameters.get("Stot"),
					   S = Stot - r2Level,
					   km = parameters.get("km");
				double rate = k2 * E * S / (km + S);
				if (rate != 0) {
					return Math.max(1, (int)Math.round(1 / rate));
				} else {
					return Scenario.INFINITE_TIME;
				}
			}
			
			public String[] listVariableParameters() {
				return new String[]{"k2", "km", "Stot"};
			}
			
			public String toString() {
				return "Scenario 6";
			}
		};
		sixScenarios[2].setParameter("k2", 0.01);
		sixScenarios[2].setParameter("km", 10.0);
		sixScenarios[2].setParameter("Stot", 15.0);
	}
	
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
		if (rate > 1e-8) {
			return Math.max(1, (int)Math.round(1 / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
		} else {
			return INFINITE_TIME;
		}
	}

	public List<Integer> generateTimes(int nLevelsReactant1, int nLevelsReactant2) {
		List<Integer> times = new LinkedList<Integer>();
		for (int i=0;i<nLevelsReactant2-1;i++) {
			times.add(INFINITE_TIME); //no reactant1 = no reaction
			for (int j=1;j<nLevelsReactant1;j++) {
				times.add(computeFormula(j, i));
			}
		}
		for (int j=0;j<nLevelsReactant1;j++) {
			times.add(INFINITE_TIME); //all reactant2 already reacted (active) = no reaction
		}
		return times;
	}
	
	public String[] listVariableParameters() {
		return new String[]{};
	}
	
}
