package inat.cytoscape;

import giny.model.Edge;
import inat.model.Model;
import inat.model.Scenario;
import inat.model.ScenarioMono;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
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
								SCENARIO = Model.Properties.SCENARIO,
								CANONICAL_NAME = Model.Properties.CANONICAL_NAME,
								INCREMENT = Model.Properties.INCREMENT,
								UNCERTAINTY = Model.Properties.UNCERTAINTY;
	
	private Scenario[] scenarios = Scenario.sixScenarios;
	private int previouslySelectedScenario = 0;

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
		final CyAttributes edgeAttrib = Cytoscape.getEdgeAttributes();
		int increment;
		if (edgeAttrib.hasAttribute(edge.getIdentifier(), INCREMENT)) {
			increment = edgeAttrib.getIntegerAttribute(edge.getIdentifier(), INCREMENT);
		} else {
			if (edge.getSource().equals(edge.getTarget())) {
				increment = -1;
			} else {
				increment = 1;
			}
		}
		String res;
		if (nodeAttrib.hasAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME)) {
			res = nodeAttrib.getStringAttribute(edge.getSource().getIdentifier(), CANONICAL_NAME);
			title.append(res);
			
			if (increment >= 0) {
				title.append(" --> ");
			} else {
				title.append(" --| ");
			}
			if (nodeAttrib.hasAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME)) {
				res = nodeAttrib.getStringAttribute(edge.getTarget().getIdentifier(), CANONICAL_NAME);
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
			if (edgeAttrib.hasAttribute(edge.getIdentifier(), UNCERTAINTY)) {
				value = edgeAttrib.getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY);
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
			final JRadioButton positiveIncrement = new JRadioButton("Positive"),
							   negativeIncrement = new JRadioButton("Negative");
			ButtonGroup incrementGroup = new ButtonGroup();
			incrementGroup.add(positiveIncrement);
			incrementGroup.add(negativeIncrement);
			if (increment >= 0) {
				positiveIncrement.setSelected(true);
				negativeIncrement.setSelected(false);
			} else {
				positiveIncrement.setSelected(false);
				negativeIncrement.setSelected(true);
			}
			Box incrementBox = new Box(BoxLayout.X_AXIS);
			incrementBox.add(positiveIncrement);
			incrementBox.add(Box.createHorizontalStrut(50));
			incrementBox.add(negativeIncrement);
			allParametersBox.add(new LabelledField("Influence", incrementBox));
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
							Double paramValue;
							if (paramField.getField() instanceof JFormattedTextField) {
								paramValue = new Double(((JFormattedTextField)(paramField).getField()).getValue().toString());
							} else if (paramField.getField() instanceof Box) {
								paramValue = new Double(((JFormattedTextField)(((Box)paramField.getField()).getComponents()[0])).getValue().toString());
							} else {
								paramValue = scenario.getParameter(paramName);
							}
							scenario.setParameter(paramName, paramValue);
							edgeAttrib.setAttribute(edge.getIdentifier(), paramName, paramValue);
						}
					}
					int uncert = uncertainty.getValue();
					edgeAttrib.setAttribute(edge.getIdentifier(), SCENARIO, 0);
					edgeAttrib.setAttribute(edge.getIdentifier(), UNCERTAINTY, uncert);
					edgeAttrib.setAttribute(edge.getIdentifier(), INCREMENT, ((positiveIncrement.isSelected())?1:-1));
					
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
			
			int scenarioIdx;
			if (edgeAttrib.hasAttribute(edge.getIdentifier(), SCENARIO)) {
				scenarioIdx = edgeAttrib.getIntegerAttribute(edge.getIdentifier(), SCENARIO); 
			} else {
				scenarioIdx = 0;
			}
			
			previouslySelectedScenario = scenarioIdx;
			comboScenario.setSelectedIndex(scenarioIdx);
			boxScenario.add(comboScenario);
			boxScenario.add(Box.createGlue());
			
			Box boxScenarioAllParameters = new Box(BoxLayout.Y_AXIS);
			boxScenarioAllParameters.add(boxScenarioParameters);
			Integer value;
			if (edgeAttrib.hasAttribute(edge.getIdentifier(), UNCERTAINTY)) {
				value = edgeAttrib.getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY);
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
			final JRadioButton positiveIncrement = new JRadioButton("Positive"),
			   				   negativeIncrement = new JRadioButton("Negative");
			ButtonGroup incrementGroup = new ButtonGroup();
			incrementGroup.add(positiveIncrement);
			incrementGroup.add(negativeIncrement);
			if (increment >= 0) {
			positiveIncrement.setSelected(true);
			negativeIncrement.setSelected(false);
			} else {
			positiveIncrement.setSelected(false);
			negativeIncrement.setSelected(true);
			}
			Box incrementBox = new Box(BoxLayout.X_AXIS);
			incrementBox.add(positiveIncrement);
			incrementBox.add(Box.createHorizontalStrut(50));
			incrementBox.add(negativeIncrement);
			boxScenarioAllParameters.add(new LabelledField("Influence", incrementBox));
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
							Double paramValue;
							if (paramField.getField() instanceof JFormattedTextField) {
								paramValue = new Double(((JFormattedTextField)(paramField).getField()).getValue().toString());
							} else if (paramField.getField() instanceof Box) {
								paramValue = new Double(((JFormattedTextField)(((Box)paramField.getField()).getComponents()[0])).getValue().toString());
							} else {
								paramValue = selectedScenario.getParameter(paramName);
							}
							selectedScenario.setParameter(paramName, paramValue);
							edgeAttrib.setAttribute(edge.getIdentifier(), paramName, paramValue);
						}
					}
					int uncert = uncertainty.getValue();
					
					edgeAttrib.setAttribute(edge.getIdentifier(), UNCERTAINTY, uncert);
					edgeAttrib.setAttribute(edge.getIdentifier(), SCENARIO, comboScenario.getSelectedIndex());
					edgeAttrib.setAttribute(edge.getIdentifier(), INCREMENT, ((positiveIncrement.isSelected())?1:-1));
					
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
	
	/**
	 * Update the list of parameters shown when a differnt scenario is chosen
	 * @param edge The edge representing the reaction of which we display the parameter
	 * @param parametersBox The box in which to put the list of parameters for the new scenario
	 * @param selectedScenario The newly selected scenario
	 */
	private void updateParametersBox(Edge edge, Box parametersBox, Scenario selectedScenario) {
		parametersBox.removeAll();
		
		//Look for the index of the currently selected scenario, so that we can compare it with the previously selected one, and thus correctly convert the parameters between the two
		int currentlySelectedScenario = 0;
		for (int i=0;i<scenarios.length;i++) {
			if (scenarios[i].equals(selectedScenario)) {
				currentlySelectedScenario = i;
				break;
			}
		}
		
		String[] parameters = selectedScenario.listVariableParameters();
		CyAttributes edgeAttrib = Cytoscape.getEdgeAttributes();
		CyAttributes nodeAttrib = Cytoscape.getNodeAttributes();
		for (int i=0;i<parameters.length;i++) {
			DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_STRING);
			format.setMinimumFractionDigits(8);
			final JFormattedTextField param = new JFormattedTextField(format);
			if (currentlySelectedScenario != previouslySelectedScenario) {
				if (previouslySelectedScenario == 0 && currentlySelectedScenario == 1) { //1234 -> 5
					if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_STOT)) {
						if (nodeAttrib.hasAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
							param.setValue(nodeAttrib.getIntegerAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS));
						}
					} else if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_K2_KM)) {
						Double par = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_ONLY_PARAMETER);
						Double Stot;
						if (nodeAttrib.hasAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
							Stot = (double)nodeAttrib.getIntegerAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
						} else if (edgeAttrib.hasAttribute(edge.getIdentifier(), parameters[i])) {
							Stot = edgeAttrib.getDoubleAttribute(edge.getIdentifier(), parameters[i]);
						} else {
							Stot = selectedScenario.getParameter(parameters[i]);
						}
						Double k2km = par / Stot;
						param.setValue(k2km);
					}
				} else if (previouslySelectedScenario == 1 && currentlySelectedScenario == 0) { //5 -> 1234
					Double k2km, Stot;
					k2km = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_K2_KM);
					Stot = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_STOT);
					param.setValue(k2km * Stot);
				} else if (previouslySelectedScenario == 1 && currentlySelectedScenario == 2) { //5 -> 6
					if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_STOT)) {
						param.setValue(scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_STOT));
					} else if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_KM)) {
						Double Stot = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_STOT);
						Double k2km = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_K2_KM);
						Double k2 = (Stot * Stot - Stot) * k2km; //this allows us to have a difference of 0.001 between scenarios 5 and 6
						Double km = k2 / k2km;
						param.setValue(km);
					} else if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_K2)) {
						Double Stot = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_STOT);
						Double k2km = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_K2_KM);
						Double k2 = (Stot * Stot - Stot) * k2km; //this allows us to have a difference of 0.001 between scenarios 5 and 6
						param.setValue(k2);
					}
				} else if (previouslySelectedScenario == 2 && currentlySelectedScenario == 1) { //6 -> 5
					if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_STOT)) {
						param.setValue(scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_STOT));
					} else if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_K2_KM)) {
						Double k2 = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_K2);
						Double km = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_KM);
						Double k2km = k2 / km;
						param.setValue(k2km);
					}
				} else if (previouslySelectedScenario == 0 && currentlySelectedScenario == 2) { //1234 -> 6
					if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_STOT)) {
						if (nodeAttrib.hasAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
							param.setValue(nodeAttrib.getIntegerAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS));
						}
					} else if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_KM)) {
						Double par = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_ONLY_PARAMETER);
						Double Stot;
						if (nodeAttrib.hasAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
							Stot = (double)nodeAttrib.getIntegerAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
						} else if (edgeAttrib.hasAttribute(edge.getIdentifier(), parameters[i])) {
							Stot = edgeAttrib.getDoubleAttribute(edge.getIdentifier(), parameters[i]);
						} else {
							Stot = selectedScenario.getParameter(parameters[i]);
						}
						Double k2km = par / Stot;
						Double k2 = (Stot * Stot - Stot) * k2km; //this allows us to have a difference of 0.001 between scenarios 5 and 6
						Double km = k2 / k2km;
						param.setValue(km);
					} else if (parameters[i].equals(Model.Properties.SCENARIO_PARAMETER_K2)) {
						Double par = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_ONLY_PARAMETER);
						Double Stot;
						if (nodeAttrib.hasAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
							Stot = (double)nodeAttrib.getIntegerAttribute(edge.getTarget().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
						} else if (edgeAttrib.hasAttribute(edge.getIdentifier(), parameters[i])) {
							Stot = edgeAttrib.getDoubleAttribute(edge.getIdentifier(), parameters[i]);
						} else {
							Stot = selectedScenario.getParameter(parameters[i]);
						}
						Double k2km = par / Stot;
						Double k2 = (Stot * Stot - Stot) * k2km; //this allows us to have a difference of 0.001 between scenarios 5 and 6
						param.setValue(k2);
					}
				} else if (previouslySelectedScenario == 2 && currentlySelectedScenario == 0) { //6 -> 1234
					Double Stot = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_STOT);
					Double k2 = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_K2);
					Double km = scenarios[previouslySelectedScenario].getParameter(Model.Properties.SCENARIO_PARAMETER_KM);
					Double k2km = k2 / km;
					param.setValue(Stot * k2km);
				}
			} else if (edgeAttrib.hasAttribute(edge.getIdentifier(), parameters[i])) {
				Double value = edgeAttrib.getDoubleAttribute(edge.getIdentifier(), parameters[i]);
				param.setValue(value);
				scenarios[currentlySelectedScenario].setParameter(parameters[i], value);
			} else {
				param.setValue(selectedScenario.getParameter(parameters[i]));
			}
			Dimension prefSize = param.getPreferredSize();
			prefSize.width *= 1.5;
			param.setPreferredSize(prefSize);
			if (parameters.length == 1) { //If we have only one parameter, we show a slider for the parameter
				final JSlider parSlider = new JSlider(1, 5, 3);
				Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
				labelTable.put(new Integer(1), new JLabel("v. slow"));
				labelTable.put(new Integer(2), new JLabel("slow"));
				labelTable.put(new Integer(3), new JLabel("medium"));
				labelTable.put(new Integer(4), new JLabel("fast"));
				labelTable.put(new Integer(5), new JLabel("v. fast"));
				parSlider.setLabelTable(labelTable);
				parSlider.setPaintLabels(true);
				parSlider.setMajorTickSpacing(1);
				parSlider.setPaintTicks(true);
				parSlider.setSnapToTicks(true);
				prefSize = parSlider.getPreferredSize();
				prefSize.width *= 1.5;
				parSlider.setPreferredSize(prefSize);
				Box parSliBox = new Box(BoxLayout.Y_AXIS);
				param.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						param.setEnabled(true);
						parSlider.setEnabled(false);
					}
				});
				parSlider.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						parSlider.setEnabled(true);
						param.setEnabled(false);
						double multiplicator = (Double)(param.getValue()) / 0.004;
						double exp = Math.log(multiplicator) / Math.log(2);
						int sliderValue = (int)(exp + 3);
						if (sliderValue < 1) {
							parSlider.setValue(1);
						} else if (sliderValue > 5) {
							parSlider.setValue(5);
						} else {
							parSlider.setValue(sliderValue);
						}
					}
				});
				parSlider.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent e) {
						int exp = parSlider.getValue() - 3;
						double multiplicator = Math.pow(2.0, exp);
						param.setValue(0.004 * multiplicator);
					}
				});
				parSliBox.add(param);
				parSliBox.add(parSlider);
				param.setEnabled(true);
				parSlider.setEnabled(false);
				parametersBox.add(new LabelledField(parameters[i], parSliBox));
			} else {
				parametersBox.add(new LabelledField(parameters[i], param));
			}
		}
		parametersBox.validate();
		
		//Update the index of the currently selected scenario, so that if the user changes scenario we will be able to know which we were using before, and thus correctly convert the parameters between the two
		previouslySelectedScenario = 0;
		for (int i=0;i<scenarios.length;i++) {
			if (scenarios[i].equals(selectedScenario)) {
				previouslySelectedScenario = i;
				break;
			}
		}
	}
}
