package inat.model;
import inat.exceptions.InatException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class UserFormula extends Scenario {
	public static final String UPSTREAM_REACTANT_ACTIVITY = "Upstream reactant activity",
							   DOWNSTREAM_REACTANT_ACTIVITY = "Downstream reactant activity";
	
	private String name,
				   javaScriptFormula = null;
	private Vector<FormulaVariable> variables = null;
	
	public UserFormula() {
		this.name = "(Formula name)";
		this.javaScriptFormula = "";
		this.variables = new Vector<FormulaVariable>();
	}
	
	public UserFormula(String name, String javaScriptFormula, Vector<FormulaVariable> variables) {
		this.name = name;
		this.javaScriptFormula = javaScriptFormula;
		this.variables = variables;
		for (FormulaVariable v : variables) {
			if (v.isParameter()) {
				defaultParameterValues.put(v.getName(), v.getDefaultParameterValue());
			}
		}
	}
	
	/**
	 * Read the user-defined formulae from the current network.
	 * Format for the user-defined formulae (string list): (name, formula, parameterVariables[], ";", linkedVariables[], ";", )+ "."
	 * @param formulaList The parameter taken from the network property Model.Properties.USER_DEFINED_FORMULAE
	 * @return A vector containing the user-defined formulae read from the list
	 */
	public static Scenario[] readFormulae(List<String> formulaList) {
		Vector<UserFormula> result = new Vector<UserFormula>();
		ListIterator<String> it = formulaList.listIterator();
		String s = it.next();
		while (it.hasNext() && !s.equals(".")) {
			String name = s;
			String formula = it.next();
			Vector<FormulaVariable> vars = new Vector<FormulaVariable>();
			s = it.next();
			while (it.hasNext() && !s.equals(";")) {
				vars.add(new FormulaVariable(s)); //This constructor is done so that it reads the correct format
				s = it.next();
			}
			result.add(new UserFormula(name, formula, vars));
			s = it.next();
		}
		return result.toArray(new Scenario[] {});
	}
	
	/**
	 * Insert a new UserFormula into the given list. For the format, see readFormulae
	 * @param formulaList The list of user-defined formulae stored in the network property Model.Properties.USER_DEFINED_FORMULAE
	 * @param formula The formula to be inserted into the list
	 */
	public static List<String> addNewUserFormula(List<String> formulaList, UserFormula formula) {
		ListIterator<String> it;
		if (formulaList == null) {
			formulaList = new ArrayList<String>();
			formulaList.add(".");
			it = formulaList.listIterator();
			it.next(); //so that in every case we have already read the "."
		} else {
			it = formulaList.listIterator();
			String s = it.next();
			while (it.hasNext() && !s.equals(".")) {
				s = it.next();
			}
		}
		it.previous(); //go before the "."
		it.add(formula.getName());
		it.add(formula.getFormula());
		for (FormulaVariable v : formula.getVariables()) {
			it.add(v.toString()); //The toString method is done so that it outputs the correct format
		}
		it.add(";");
		return formulaList;
	}
	
	/**
	 * Update an existing UserFormula in the given list. For the format, see readFormulae
	 * @param formulaList The list of user-defined formulae stored in the network property Model.Properties.USER_DEFINED_FORMULAE
	 * @param formula The formula to be updated
	 */
	public static List<String> updateUserFormula(List<String> formulaList, UserFormula oldFormula, UserFormula editedFormula) {
		ListIterator<String> it = formulaList.listIterator();
		String s = it.next();
		while (it.hasNext() && !s.equals(".")) {
			if (s.equals(oldFormula.getName())) {
				while (it.hasNext() && !s.equals(";")) {
					it.remove();
					s = it.next();
				}
				it.previous();
				it.add(editedFormula.getName());
				it.add(editedFormula.getFormula());
				for (FormulaVariable v : editedFormula.getVariables()) {
					it.add(v.toString()); //The toString method is done so that it outputs the correct format
				}
				return formulaList;
			}
			s = it.next();
		}
		//If we arrive here we did not find the formula: report error
		return null;
	}
	
	/**
	 * Remove a selected formula from the list
	 * @param formulaList The list from which to remove the formula (for the format, check readFormulae)
	 * @param condemnedFormula The formula to be deleted (the name will be used to find the target)
	 * @return The changed list
	 */
	public static List<String> deleteUserFormula(List<String> formulaList, UserFormula condemnedFormula) {
		ListIterator<String> it = formulaList.listIterator();
		String s = it.next();
		boolean notFound = true;
		while (it.hasNext() && !s.equals(".")) {
			if (s.equals(condemnedFormula.getName())) {
				notFound = false;
				while (it.hasNext() && !s.equals(";")) {
					it.remove();
					s = it.next();
				}
			}
			s = it.next();
		}
		it.remove(); //to remove the ";"
		if (notFound) {
			return null;
		} else {
			return formulaList;
		}
	}
	
	@Override
	public double computeRate(int r1Level, int nLevelsR1, int r2Level, int nLevelsR2, boolean activatingReaction) throws InatException {
		ScriptEngineManager factory = new ScriptEngineManager();
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		Bindings bindi = engine.createBindings();
		for (FormulaVariable v : variables) {
			if (v.isParameter()) {
				bindi.put(v.getName(), parameters.get(v.getName()));
			} else {
				if (v.getLinkedValue() == null) {
					throw new InatException("In user-defined formula \"" + getName() + "\", the value linked to the variable \"" + v.getName() + "\" is invalid (null).");
				} else if (v.getLinkedValue().equals(UPSTREAM_REACTANT_ACTIVITY)) {
					bindi.put(v.getName(), r1Level);
				} else if (v.getLinkedValue().equals(DOWNSTREAM_REACTANT_ACTIVITY)) {
					bindi.put(v.getName(), r2Level);
				} else {
					//UNKNOWN PARAMETER
					System.err.println("Unknown linked variable name: " + v.getName());
				}
			}
		}
		Object result;
		try {
			result = engine.eval(javaScriptFormula, bindi);
		} catch (Exception ex) {
			throw new InatException("Error while evaluating formula \"" + getName() + "\".", ex);
		}
		return Double.parseDouble(result.toString());
	}
	
	@Override
	public String[] listVariableParameters() {
		Vector<String> variableParameters = new Vector<String>();
		for (FormulaVariable v : variables) {
			if (v.isParameter()) {
				variableParameters.add(v.getName());
			}
		}
		return variableParameters.toArray(new String[]{});
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFormula() {
		return javaScriptFormula;
	}
	
	public Vector<FormulaVariable> getVariables() {
		return variables;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setFormula(String javaScriptFormula) {
		this.javaScriptFormula = javaScriptFormula;
	}
	
	public void setVariables(Vector<FormulaVariable> variables) {
		this.variables = variables;
	}
	
}
