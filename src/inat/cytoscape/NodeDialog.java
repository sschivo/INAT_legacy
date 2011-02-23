package inat.cytoscape;

import giny.model.Node;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import cytoscape.Cytoscape;

/**
 * The node dialog contains the settings of a node.
 * 
 * @author Brend Wanders
 * 
 */
public class NodeDialog extends JFrame {
	/**
	 * Constructor.
	 * 
	 * @param node the node to display for.
	 */
	public NodeDialog(final Node node) {
		super("Reactant '" + node.getIdentifier() + "'");

		this.setLayout(new BorderLayout(2, 2));

		JPanel values = new JPanel(new GridLayout(1, 2, 2, 2));
		values.add(new JLabel("Initial Concentration"));

		int levels = Cytoscape.getNetworkAttributes().getIntegerAttribute(
				Cytoscape.getCurrentNetwork().getIdentifier(), "levels");
		final JSlider initialConcentration = new JSlider(0, levels);
		initialConcentration.setValue(Cytoscape.getNodeAttributes().getIntegerAttribute(node.getIdentifier(),
				"initialConcentration"));

		initialConcentration.setMajorTickSpacing(5);
		initialConcentration.setMinorTickSpacing(1);

		initialConcentration.setPaintLabels(true);
		initialConcentration.setPaintTicks(true);
		initialConcentration.setSnapToTicks(true);

		values.add(initialConcentration);

		this.add(values, BorderLayout.CENTER);

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		controls.add(new JButton(new AbstractAction("Save") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Cytoscape.getNodeAttributes().setAttribute(node.getIdentifier(), "initialConcentration",
						initialConcentration.getValue());

				Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

				NodeDialog.this.dispose();
			}
		}));

		controls.add(new JButton(new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// discard changes
				NodeDialog.this.dispose();
			}
		}));

		this.add(controls, BorderLayout.SOUTH);
	}
}
