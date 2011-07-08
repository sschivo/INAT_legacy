package inat.model;
import java.util.*;

/**
 * Represents a scenario for a reaction in the model.
 * There are 3 predefined scenarios, each one with its set of parameters.
 */
public class Scenario {
	private static final String SCENARIO_PARAMETER_KM = Model.Properties.SCENARIO_PARAMETER_KM,
								SCENARIO_PARAMETER_K2 = Model.Properties.SCENARIO_PARAMETER_K2,
								SCENARIO_PARAMETER_STOT = Model.Properties.SCENARIO_PARAMETER_STOT,
								SCENARIO_PARAMETER_K2_KM = Model.Properties.SCENARIO_PARAMETER_K2_KM,
								SCENARIO_ONLY_PARAMETER = Model.Properties.SCENARIO_ONLY_PARAMETER;
	protected HashMap<String, Double> parameters = new HashMap<String, Double>(); //Parameter name -> value
	public static final int INFINITE_TIME = -1; //The constant to mean that the reaction will not happen
	
	public static final Scenario[] sixScenarios = new Scenario[3]; //The default predefined scenarios
	
	static {
		sixScenarios[0] = new Scenario() {
			public int computeFormula(int r1Level, int r2Level) {
				double par = parameters.get(SCENARIO_ONLY_PARAMETER),
					   E = r1Level;
				double rate = par * E;
				if (rate != 0) {
					return Math.max(1, (int)Math.round(1 / rate));
				} else {
					return Scenario.INFINITE_TIME;
				}
			}
			
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_ONLY_PARAMETER};
			}
			
			public String toString() {
				return "Scenario 1-2-3-4";
			}
		};
		sixScenarios[0].setParameter(SCENARIO_ONLY_PARAMETER, 0.01);
		
		sixScenarios[1] = new Scenario() {
			public int computeFormula(int r1Level, int r2Level) {
				double par1 = parameters.get(SCENARIO_PARAMETER_K2_KM),
					   Stot = parameters.get(SCENARIO_PARAMETER_STOT),
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
				return new String[]{SCENARIO_PARAMETER_K2_KM, SCENARIO_PARAMETER_STOT};
			}
			
			public String toString() {
				return "Scenario 5";
			}
		};
		sixScenarios[1].setParameter(SCENARIO_PARAMETER_K2_KM, 0.001);
		sixScenarios[1].setParameter(SCENARIO_PARAMETER_STOT, 15.0);
		
		sixScenarios[2] = new Scenario() {
			public int computeFormula(int r1Level, int r2Level) {
				double k2 = parameters.get(SCENARIO_PARAMETER_K2),
					   E = (double)r1Level,
					   Stot = parameters.get(SCENARIO_PARAMETER_STOT),
					   S = Stot - r2Level,
					   km = parameters.get(SCENARIO_PARAMETER_KM);
				double rate = k2 * E * S / (km + S);
				if (rate != 0) {
					return Math.max(1, (int)Math.round(1 / rate));
				} else {
					return Scenario.INFINITE_TIME;
				}
			}
			
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_PARAMETER_K2, SCENARIO_PARAMETER_KM, SCENARIO_PARAMETER_STOT};
			}
			
			public String toString() {
				return "Scenario 6";
			}
		};
		sixScenarios[2].setParameter(SCENARIO_PARAMETER_K2, 0.01);
		sixScenarios[2].setParameter(SCENARIO_PARAMETER_KM, 10.0);
		sixScenarios[2].setParameter(SCENARIO_PARAMETER_STOT, 15.0);
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
		double k2 = parameters.get(SCENARIO_PARAMETER_K2),
			   E = r1Level,
			   km = parameters.get(SCENARIO_PARAMETER_KM),
			   Stot = parameters.get(SCENARIO_PARAMETER_STOT),
			   S = Stot - r2Level;
		double rate = k2 * E * S / (km + S);
		if (rate > 1e-8) {
			return Math.max(1, (int)Math.round(1 / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
		} else {
			return INFINITE_TIME;
		}
	}

	/**
	 * Generate the times table based on the scenario formula and parameters.
	 * @param nLevelsReactant1 The total number of levels of reactant1 (the enzyme or catalyst)
	 * @param nLevelsReactant2 The total number of levels of reactant2 (the substrate)
	 * @return Output is a list for no particular reason anymore. It used to be
	 * set as a Cytoscape property, but these tables can become clumsy to be stored
	 * inside the model, slowing down the loading/saving processes in the Cytoscape
	 * interface. We now compute the tables on the fly and output them directly as
	 * reference constants in the UPPPAAL models, saving also memory.
	 */
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
