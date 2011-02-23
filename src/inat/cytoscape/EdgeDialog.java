package inat.cytoscape;

import giny.model.Edge;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cytoscape.Cytoscape;

/**
 * The edge dialog contains the settings of a edge.
 * 
 * @author Brend Wanders
 * 
 */
public class EdgeDialog extends JFrame {
	/**
	 * Constructor.
	 * 
	 * @param edge the edge to display for.
	 */
	public EdgeDialog(final Edge edge) {
		super("Reaction '" + edge.getIdentifier() + "'");

		this.setLayout(new BorderLayout(2, 2));

		JPanel values = new JPanel(new GridLayout(1, 2, 2, 2));
		values.add(new JLabel("<Nothing yet>"));

		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		controls.add(new JButton(new AbstractAction("Save") {
			@Override
			public void actionPerformed(ActionEvent e) {
				Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

				EdgeDialog.this.dispose();
			}
		}));

		controls.add(new JButton(new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				// discard changes
				EdgeDialog.this.dispose();
			}
		}));

		this.add(controls, BorderLayout.SOUTH);
	}
}
