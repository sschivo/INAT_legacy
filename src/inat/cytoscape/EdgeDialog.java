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

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

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
								CANCEL = "Cancel",
								SCENARIO = "scenario",
								CANONICAL_NAME = "canonicalName",
								INCREMENT = "increment",
								UNCERTAINTY = "uncertainty";
	
	private Scenario[] scenarios = Scenario.sixScenarios;

	/**
	 * Constructor.
	 * 
	 * @param edge the edge to display for.
	 */
	public EdgeDialog(final Edge edge) {
		super("Reaction '" + edge.getIdentifier() + "'");
		//super("Reaction " + Cytoscape.getNodeAttributes().getAttribute(edge.getSource().getIdentifier(), "canonicalName") + ((Integer.parseInt(Cytoscape.getEdgeAttributes().getAttribute(edge.getIdentifier(), "increment").toString()) >= 0)?" --> ":" --| ") + Cytoscape.getNodeAttributes().getAttribute(edge.getTarget().getIdentifier(), "canonicalName"));
		StringBuilder title = new StringBuilder();
		title.append("Reaction ");
		CyAttributes nodeAttrib = Cytoscape.getNodeAttributes();
		CyAttributes edgeAttrib = Cytoscape.getEdgeAttributes();
		String res;
		res = nodeAttrib.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME);
		if (res != null) {
			title.append(res);
			Integer val = edgeAttrib.getIntegerAttribute(edge.getIdentifier(), INCREMENT);
			if (val == null) {
				val = 0;
			}
			if (val >= 0) {
				title.append(" --> ");
			} else {
				title.append(" --| ");
			}
			res = nodeAttrib.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME);
			if (res != null) {
				title.append(res);
				this.setTitle(title.toString());
			}
		}

		this.setLayout(new BorderLayout(2, 2));

		JPanel values = new JPanel(new GridLayout(1, 2, 2, 2));
		final Box boxScenario = new Box(BoxLayout.X_AXIS);
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		if (edge.getSource() == edge.getTarget()) { //if it is an auto-arc, the reaction this edge represents is a mono-reaction (one reactant only). Thus, we use the mono-reaction scenario
			final ScenarioMono scenario = new ScenarioMono();
			final Box parameterBox = new Box(BoxLayout.X_AXIS);
			updateParametersBox(edge, parameterBox, scenario);
			Box allParametersBox = new Box(BoxLayout.Y_AXIS);
			allParametersBox.add(parameterBox);
			Integer value;
			if (Cytoscape.getEdgeAttributes().hasAttribute(edge.getIdentifier(), UNCERTAINTY)) {
				value = Cytoscape.getEdgeAttributes().getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY);
			} else {
				value = 0;
			}
			final JSlider uncertainty = new JSlider(0, 100, value);
			uncertainty.setPaintTicks(true);
			uncertainty.setMinorTickSpacing(5);
			uncertainty.setMajorTickSpacing(10);
			uncertainty.setPaintLabels(true);
			final LabelledField incertaintyField = new LabelledField("Uncertainty = " + uncertainty.getValue() + "%", uncertainty);
			uncertainty.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					incertaintyField.setTitle("Uncertainty = " + uncertainty.getValue() + "%");
					repaint();
				}
			});
			allParametersBox.add(incertaintyField);
			boxScenario.add(allParametersBox);
			
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
					int uncert = uncertainty.getValue();
					/*int nLevels = Cytoscape.getNetworkAttributes().getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), NUMBER_OF_LEVELS);
					List<Integer> times = scenario.generateTimes(1 + nLevels);
					List<Integer> timesL = new ArrayList<Integer>(), timesU = new ArrayList<Integer>();
					for (Integer i : times) {
						if (i.equals(VariablesModel.INFINITE_TIME)) {
							timesL.add(i);
							timesU.add(i);
						} else {
							timesL.add(Math.max(1, (int)(i * (100.0 - uncert) / 100.0))); //we use Math.max because we do not want to put 0 as a time
							timesU.add(Math.max(1, (int)(i * (100.0 + uncert) / 100.0)));
						}
					}
					Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), TIMES, times);
					Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), TIMES_LOWER, timesL);
					Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), TIMES_UPPER, timesU);*/
					Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), SCENARIO, 0);
					Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), UNCERTAINTY, uncert);
					
					Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

					EdgeDialog.this.dispose();
				}
			}));
		} else {
			final Box boxScenarioParameters = new Box(BoxLayout.X_AXIS);
			final JComboBox comboScenario = new JComboBox(scenarios);
			comboScenario.setMaximumSize(new Dimension(200, 20));
			comboScenario.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					updateParametersBox(edge, boxScenarioParameters, (Scenario)comboScenario.getSelectedItem());
					EdgeDialog.this.validate();
					EdgeDialog.this.pack();
				}
			});
			
			Integer scenarioIdx = Cytoscape.getEdgeAttributes().getIntegerAttribute(edge.getIdentifier(), SCENARIO);
			if (scenarioIdx == null) {
				scenarioIdx = 0;
			}
			
			comboScenario.setSelectedIndex(scenarioIdx);
			boxScenario.add(comboScenario);
			boxScenario.add(Box.createGlue());
			
			Box boxScenarioAllParameters = new Box(BoxLayout.Y_AXIS);
			boxScenarioAllParameters.add(boxScenarioParameters);
			Integer value;
			if (Cytoscape.getEdgeAttributes().hasAttribute(edge.getIdentifier(), UNCERTAINTY)) {
				value = Cytoscape.getEdgeAttributes().getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY);
			} else {
				value = 0;
			}
			final JSlider uncertainty = new JSlider(0, 100, value);
			uncertainty.setPaintTicks(true);
			uncertainty.setMinorTickSpacing(5);
			uncertainty.setMajorTickSpacing(10);
			uncertainty.setPaintLabels(true);
			final LabelledField uncertaintyField = new LabelledField("Uncertainty = " + uncertainty.getValue() + "%", uncertainty);
			uncertainty.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					uncertaintyField.setTitle("Uncertainty = " + uncertainty.getValue() + "%");
					repaint();
				}
			});
			boxScenarioAllParameters.add(uncertaintyField);
			boxScenario.add(boxScenarioAllParameters);
			
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
					int uncert = uncertainty.getValue();
					
					/*List<Integer> times = selectedScenario.generateTimes(1 + Cytoscape.getNetworkAttributes().getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), NUMBER_OF_LEVELS));
					List<Integer> timesL = new ArrayList<Integer>(), timesU = new ArrayList<Integer>();
					for (Integer i : times) {
						if (i.equals(VariablesModel.INFINITE_TIME)) {
							timesL.add(i);
							timesU.add(i);
						} else {
							timesL.add(Math.max(1, (int)(i * (100.0 - uncert) / 100.0))); //Math.max is used because we cannot put 0 as a time
							timesU.add(Math.max(1, (int)(i * (100.0 + uncert) / 100.0)));
						}
					}
					Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), TIMES_LOWER, timesL);
					Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), TIMES_UPPER, timesU);*/
					Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), UNCERTAINTY, uncert);
					
					/*Cytoscape.getEdgeAttributes().setListAttribute(edge.getIdentifier(), TIMES, times);*/
					Cytoscape.getEdgeAttributes().setAttribute(edge.getIdentifier(), SCENARIO, comboScenario.getSelectedIndex());
					
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
			format.setMinimumFractionDigits(8);
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
