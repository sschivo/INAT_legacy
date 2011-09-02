package inat.cytoscape;

import giny.model.Node;

import inat.model.Model;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

/**
 * The node dialog contains the settings of a node.
 * 
 * @author Brend Wanders
 * 
 */
public class NodeDialog extends JFrame {
	
	private static final long serialVersionUID = 1498730989498413815L;

	/**
	 * Constructor.
	 * 
	 * @param node the node to display for.
	 */
	@SuppressWarnings("unchecked")
	public NodeDialog(final Node node) {
		super("Reactant '" + node.getIdentifier() + "'");
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes(),
					 nodeAttributes = Cytoscape.getNodeAttributes();
		this.setTitle("Edit reactant");
		Object res = nodeAttributes.getAttribute(node.getIdentifier(), Model.Properties.CANONICAL_NAME);
		String name;
		if (res != null) {
			//this.setTitle("Reactant " + res.toString());
			name = res.toString();
		} else {
			name = null;
		}
		if (!nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL)) {
			nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL, 0);
		}

		this.setLayout(new BorderLayout(2, 2));

		//JPanel values = new JPanel(new GridLayout(3, 2, 2, 2));
		JPanel values = new JPanel(new GridBagLayout()); //You REALLY don't want to know how GridBagLayout works...
		
		int levels;
		if (nodeAttributes.hasAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
			levels = nodeAttributes.getIntegerAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
		} else if (networkAttributes.hasAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
			levels = networkAttributes.getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
		} else {
			levels = 15;
		}
		
		JLabel nameLabel = new JLabel("Reactant name:");
		final JTextField nameField = new JTextField(name);
		values.add(nameLabel, new GridBagConstraints(0, 0, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		values.add(nameField, new GridBagConstraints(1, 0, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		final JLabel totalLevelsLabel = new JLabel("Total activity levels: " + levels);
		values.add(totalLevelsLabel, new GridBagConstraints(0, 1, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		final JSlider totalLevels = new JSlider(1, 100);
		totalLevels.setValue(levels);
		totalLevels.setMajorTickSpacing(20);
		totalLevels.setMinorTickSpacing(10);
		
		totalLevels.setPaintLabels(true);
		totalLevels.setPaintTicks(true);
		if (totalLevels.getMaximum() == 100) {
			Dictionary labelTable = totalLevels.getLabelTable();
			labelTable.put(totalLevels.getMaximum(), new JLabel("" + totalLevels.getMaximum()));
			totalLevels.setLabelTable(labelTable);
		}
		values.add(totalLevels, new GridBagConstraints(1, 1, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		
		final JSlider initialConcentration = new JSlider(0, levels);
		initialConcentration.setValue(nodeAttributes.getIntegerAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL));
		
		final JLabel initialConcentrationLabel = new JLabel("Initial activity level: " + initialConcentration.getValue());
		values.add(initialConcentrationLabel, new GridBagConstraints(0, 2, 1, 1, 0.3, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));


		initialConcentration.setMajorTickSpacing(levels / 5);
		initialConcentration.setMinorTickSpacing(levels / 10);
		
		initialConcentration.setPaintLabels(true);
		initialConcentration.setPaintTicks(true);

		values.add(initialConcentration, new GridBagConstraints(1, 2, 1, 1, 1, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

		this.add(values, BorderLayout.CENTER);

		//When the user changes the total number of levels, we automatically update the "current activity level" slider, adapting maximum and current values in a sensible way
		totalLevels.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				totalLevelsLabel.setText("Total activity levels: " + totalLevels.getValue());
				if (totalLevels.getValueIsAdjusting()) return;
				double prevMax = initialConcentration.getMaximum(),
					   currMax = totalLevels.getValue();
				int currValue = (int)((initialConcentration.getValue()) / prevMax * currMax);
				initialConcentration.setMaximum(totalLevels.getValue());
				initialConcentration.setValue(currValue);
				int space = (initialConcentration.getMaximum() - initialConcentration.getMinimum() + 1) / 5;
				if (space < 1) space = 1;
				initialConcentration.setMajorTickSpacing(space);
				initialConcentration.setMinorTickSpacing(space / 2);
				Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
				for (int i=initialConcentration.getMinimum();i<=initialConcentration.getMaximum();i+=space) {
					labelTable.put(i, new JLabel("" + i));
				}
				initialConcentration.setLabelTable(labelTable);
				initialConcentrationLabel.setText("Initial activity level: " + initialConcentration.getValue());
				initialConcentration.setValue(currValue);
			}
			
		});
		
		
		initialConcentration.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				//if (initialConcentration.getValueIsAdjusting()) return;
				initialConcentrationLabel.setText("Initial activity level: " + initialConcentration.getValue());
			}
			
		});
		
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		controls.add(new JButton(new AbstractAction("Save") {
			private static final long serialVersionUID = -6179643943409321939L;

			@Override
			public void actionPerformed(ActionEvent e) {
				CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.INITIAL_LEVEL,
						initialConcentration.getValue());
				
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS, totalLevels.getValue());
				
				double activityRatio = (double)initialConcentration.getValue() / totalLevels.getValue();
				nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.SHOWN_LEVEL, activityRatio);
				
				if (nameField.getText() != null && nameField.getText().length() > 0) {
					nodeAttributes.setAttribute(node.getIdentifier(), Model.Properties.CANONICAL_NAME, nameField.getText());
				}

				Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

				NodeDialog.this.dispose();
			}
		}));

		controls.add(new JButton(new AbstractAction("Cancel") {
			private static final long serialVersionUID = -2038333013177775241L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// discard changes
				NodeDialog.this.dispose();
			}
		}));

		this.add(controls, BorderLayout.SOUTH);
	}
}
