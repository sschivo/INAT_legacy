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
	protected HashMap<String, Double> defaultParameterValues = new HashMap<String, Double>(); //The defaul values of parameters
	public static final int INFINITE_TIME = -1; //The constant to mean that the reaction will not happen
	
	public static final Scenario[] sixScenarios = new Scenario[3]; //The default predefined scenarios
	
	public Scenario() {
	}
	
	static {
		sixScenarios[0] = new Scenario() {
			@Override
			public double computeRate(int r1Level, int r2Level, boolean activatingReaction) {
				double par = parameters.get(SCENARIO_ONLY_PARAMETER),
					   E = r1Level;
				double rate = par * E;
				return rate;
			}
			
			@Override
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_ONLY_PARAMETER};
			}
			
			@Override
			public String toString() {
				return "Scenario 1-2-3-4";
			}
		};
		//sixScenarios[0].setParameter(SCENARIO_ONLY_PARAMETER, 0.01);
		sixScenarios[0].setDefaultParameterValue(SCENARIO_ONLY_PARAMETER, 0.01);
		
		sixScenarios[1] = new Scenario() {
			@Override
			public double computeRate(int r1Level, int r2Level, boolean activatingReaction) {
				double par1 = parameters.get(SCENARIO_PARAMETER_K2_KM),
					   Stot = parameters.get(SCENARIO_PARAMETER_STOT),
					   S,
					   E = (double)r1Level;
				if (activatingReaction) {
					S = Stot - r2Level;
				} else {
					//S = Stot - r2Level;
					S = r2Level;
				}
				double rate = par1 * E * S;
				return rate;
			}
			
			@Override
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_PARAMETER_K2_KM, SCENARIO_PARAMETER_STOT};
			}
			
			@Override
			public String toString() {
				return "Scenario 5";
			}
		};
		//sixScenarios[1].setParameter(SCENARIO_PARAMETER_K2_KM, 0.001);
		//sixScenarios[1].setParameter(SCENARIO_PARAMETER_STOT, 15.0);
		sixScenarios[1].setDefaultParameterValue(SCENARIO_PARAMETER_K2_KM, 0.001);
		sixScenarios[1].setDefaultParameterValue(SCENARIO_PARAMETER_STOT, 15.0);
		
		sixScenarios[2] = new Scenario() {
			@Override
			public double computeRate(int r1Level, int r2Level, boolean activatingReaction) {
				double k2 = parameters.get(SCENARIO_PARAMETER_K2),
					   E = (double)r1Level,
					   Stot = parameters.get(SCENARIO_PARAMETER_STOT),
					   S,
					   km = parameters.get(SCENARIO_PARAMETER_KM);
				if (activatingReaction) {
					S = Stot - r2Level;
				} else {
					//S = Stot - r2Level;
					S = r2Level;
				}
				double rate = k2 * E * S / (km + S);
				return rate;
			}
			
			@Override
			public String[] listVariableParameters() {
				return new String[]{SCENARIO_PARAMETER_K2, SCENARIO_PARAMETER_KM, SCENARIO_PARAMETER_STOT};
			}
			
			@Override
			public String toString() {
				return "Scenario 6";
			}
		};
		//sixScenarios[2].setParameter(SCENARIO_PARAMETER_K2, 0.01);
		//sixScenarios[2].setParameter(SCENARIO_PARAMETER_KM, 10.0);
		//sixScenarios[2].setParameter(SCENARIO_PARAMETER_STOT, 15.0);
		sixScenarios[2].setDefaultParameterValue(SCENARIO_PARAMETER_K2, 0.01);
		sixScenarios[2].setDefaultParameterValue(SCENARIO_PARAMETER_KM, 10.0);
		sixScenarios[2].setDefaultParameterValue(SCENARIO_PARAMETER_STOT, 15.0);
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
	
	public void setDefaultParameterValue(String name, Double value) {
		defaultParameterValues.put(name, value);
		parameters.put(name, value);
	}
	
	public Double getDefaultParameterValue(String name) {
		return defaultParameterValues.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public HashMap<String, Double> getDefaultParameterValues() {
		return (HashMap<String, Double>)defaultParameterValues.clone();
	}
	
	public double computeRate(int r1Level, int r2Level, boolean activatingReaction) {
		double k2 = parameters.get(SCENARIO_PARAMETER_K2),
		   E = r1Level,
		   km = parameters.get(SCENARIO_PARAMETER_KM),
		   Stot = parameters.get(SCENARIO_PARAMETER_STOT),
		   S;
		if (activatingReaction) { //The quantity of unreacted substrate is different depending on the point of view of the reaction: if we are activating, the unreacted substrate is inactive. But if we are inhibiting the unreacted substrate is ACTIVE.
			S = Stot - r2Level;
		} else {
			S = r2Level;
		}
		double rate = k2 * E * S / (km + S);
		return rate;
	}
	
	public Double computeFormula(int r1Level, int r2Level, boolean activatingReaction) {
		double rate = computeRate(r1Level, r2Level, activatingReaction);
		if (rate > 1e-8) {
			//return Math.max(1, (int)Math.round(1 / rate)); //We need to put at least 1 because otherwise the reaction will keep happening forever (it is not very nice not to let time pass..)
			return 1.0 / rate;
		} else {
			//return INFINITE_TIME;
			return Double.POSITIVE_INFINITY;
		}
	}

	/**
	 * Generate the times table based on the scenario formula and parameters.
	 * @param nLevelsReactant1 The total number of levels of reactant1 (the enzyme or catalyst)
	 * @param nLevelsReactant2 The total number of levels of reactant2 (the substrate)
	 * @param activatingReaction True if the reaction has activating effect, false if it is inhibiting
	 * @return Output is a list for no particular reason anymore. It used to be
	 * set as a Cytoscape property, but these tables can become clumsy to be stored
	 * inside the model, slowing down the loading/saving processes in the Cytoscape
	 * interface. We now compute the tables on the fly and output them directly as
	 * reference constants in the UPPPAAL models, saving also memory.
	 */
	public List<Double> generateTimes(int nLevelsReactant1, int nLevelsReactant2, boolean activatingReaction) {
		List<Double> times = new LinkedList<Double>();
		if (!activatingReaction) {
			for (int j=0;j<nLevelsReactant1;j++) {
				times.add(Double.POSITIVE_INFINITY); //all reactant2 already reacted (inactive) = no reaction
			}
		}
		int i, limit;
		if (activatingReaction) {
			i = 0;
			limit = nLevelsReactant2 - 1; //the last line will be done after the others (! "Begin at the beginning and go on till you come to the end: then stop."..)
		} else {
			i = 1; //the first line was already done before, with all infinites
			limit = nLevelsReactant2;
		}
		for (;i<limit;i++) {
			times.add(Double.POSITIVE_INFINITY); //no reactant1 = no reaction
			for (int j=1;j<nLevelsReactant1;j++) {
				times.add(computeFormula(j, i, activatingReaction));
			}
		}
		if (activatingReaction) {
			for (int j=0;j<nLevelsReactant1;j++) {
				times.add(Double.POSITIVE_INFINITY); //all reactant2 already reacted (active) = no reaction
			}
		}
		return times;
	}
	
	public String[] listVariableParameters() {
		return new String[]{};
	}
	
}
