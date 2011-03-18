package inat.cytoscape;

import giny.model.Edge;

import inat.model.Scenario;
import inat.model.ScenarioMono;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;

import cytoscape.Cytoscape;

/**
 * The edge dialog contains the settings of a edge.
 * 
 * @author Brend Wanders
 * 
 */
public class EdgeDialog extends JFrame {
	private static final long serialVersionUID = 6630154220142970079L;
	private static final String DECIMAL_FORMAT_STRING = "##.####",
								SAVE = "Save",
								CANCEL = "Cancel";

	/**
	 * Constructor.
	 * 
	 * @param edge the edge to display for.
	 */
	public EdgeDialog(final Edge edge) {
		super("Reaction '" + edge.getIdentifier() + "'");

		this.setLayout(new BorderLayout(2, 2));

		JPanel values = new JPanel(new GridLayout(1, 2, 2, 2));
		final Box boxScenario = new Box(BoxLayout.X_AXIS);
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		if (edge.getSource() == edge.getTarget()) { //if it is an auto-arc, the reaction this edge represents is a mono-reaction (one reactant only). Thus, we use the mono-reaction scenario
			final ScenarioMono scenario = new ScenarioMono();
			final Box parameterBox = new Box(BoxLayout.X_AXIS);
			updateParametersBox(edge, parameterBox, scenario);
			boxScenario.add(parameterBox);
			
			controls.add(new JButton(new AbstractAction(SAVE) {
				private static final long serialVersionUID = 1435389753489L;

				@Override
				public void actionPerformed(ActionEvent e) {
					Component[] paramFields = parameterBox.getComponents();
					for (int i=0;i<paramFields.length;i++) {
						if (paramFields[i] instanceof LabelledField) {
							LabelledField paramField = (LabelledField)paramFields[i];
							String paramName = paramField.getTitle();
							Double paramValue = new Double(((JFormattedTextField)(paramField).getField()).getValue().toString());
							scenario.setParameter(paramName, paramValue);
							Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), paramName, paramValue);
						}
					}
					int nLevels = Cytoscape.getNetworkAttributes().getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(),"levels");
					List<Integer> times = scenario.generateTimes(1 + nLevels, nLevels);
					Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), "times", times);
					Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), "scenario", 0);
					
					Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

					EdgeDialog.this.dispose();
				}
			}));
		} else {
			Scenario[] scenarios = new Scenario[3];
			
			final Box boxScenarioParameters = new Box(BoxLayout.X_AXIS);
			
			scenarios[0] = new Scenario() {
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
			scenarios[0].setParameter("parameter", 0.01);
			
			scenarios[1] = new Scenario() {
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
			scenarios[1].setParameter("k2/km", 0.001);
			scenarios[1].setParameter("Stot", 15.0);
			
			scenarios[2] = new Scenario() {
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
			scenarios[2].setParameter("k2", 0.01);
			scenarios[2].setParameter("km", 10.0);
			scenarios[2].setParameter("Stot", 15.0);
			
			final JComboBox comboScenario = new JComboBox(scenarios);
			comboScenario.setMaximumSize(new Dimension(200, 20));
			comboScenario.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateParametersBox(edge, boxScenarioParameters, (Scenario)comboScenario.getSelectedItem());
					EdgeDialog.this.validate();
					EdgeDialog.this.pack();
				}
			});
			
			Integer scenarioIdx = Cytoscape.getEdgeAttributes().getIntegerAttribute(edge.getIdentifier(), "scenario");
			if (scenarioIdx == null) {
				scenarioIdx = 0;
			}
			
			comboScenario.setSelectedIndex(scenarioIdx);
			boxScenario.add(comboScenario);
			boxScenario.add(Box.createGlue());
			boxScenario.add(boxScenarioParameters);
			boxScenario.add(Box.createGlue());
			

			controls.add(new JButton(new AbstractAction(SAVE) {
				private static final long serialVersionUID = -6920908627164931058L;

				@Override
				public void actionPerformed(ActionEvent e) {
					Scenario selectedScenario = (Scenario)comboScenario.getSelectedItem();
					Component[] paramFields = boxScenarioParameters.getComponents();
					for (int i=0;i<paramFields.length;i++) {
						if (paramFields[i] instanceof LabelledField) {
							LabelledField paramField = (LabelledField)paramFields[i];
							String paramName = paramField.getTitle();
							Double paramValue = new Double(((JFormattedTextField)(paramField).getField()).getValue().toString());
							selectedScenario.setParameter(paramName, paramValue);
							Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), paramName, paramValue);
						}
					}
					List<Integer> times = selectedScenario.generateTimes(1 + Cytoscape.getNetworkAttributes().getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(),"levels"));
					Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), "times", times);
					Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), "scenario", comboScenario.getSelectedIndex());
					
					Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

					EdgeDialog.this.dispose();
				}
			}));
		}
		
		
		values.add(boxScenario);


		controls.add(new JButton(new AbstractAction(CANCEL) {
			private static final long serialVersionUID = 3103827646050457714L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// discard changes
				EdgeDialog.this.dispose();
			}
		}));

		this.add(values, BorderLayout.CENTER);
		this.add(controls, BorderLayout.SOUTH);
	}
	
	private void updateParametersBox(Edge edge, Box parametersBox, Scenario selectedScenario) {
		parametersBox.removeAll();
		String[] parameters = selectedScenario.listVariableParameters();
		for (int i=0;i<parameters.length;i++) {
			DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_STRING);
			format.setMinimumFractionDigits(5);
			JFormattedTextField param = new JFormattedTextField(format);
			Double value = Cytoscape.getEdgeAttributes().getDoubleAttribute(edge.getIdentifier(), parameters[i]);
			if (value == null) {
				param.setValue(selectedScenario.getParameter(parameters[i]));
			} else {
				param.setValue(value);
			}
			Dimension prefSize = param.getPreferredSize();
			prefSize.width *= 1.5;
			param.setPreferredSize(prefSize);
			parametersBox.add(new LabelledField(parameters[i], param));
		}
		parametersBox.validate();
	}
}
