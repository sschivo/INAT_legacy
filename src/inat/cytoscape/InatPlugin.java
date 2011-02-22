package inat.cytoscape;

import inat.InatBackend;
import inat.exceptions.InatException;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;

/**
 * The INAT Cytoscape plugin main class.
 * 
 * @author Brend Wanders
 */
public class InatPlugin extends CytoscapePlugin {
	private final File configurationFile;

	/**
	 * Mandatory constructor.
	 */
	public InatPlugin() {
		this.configurationFile = CytoscapeInit.getConfigFile("inat-configuration.xml");

		try {
			InatBackend.initialise(this.configurationFile);

			CytoPanel p = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
			p.add("INAT", this.setupPanel(this));

		} catch (InatException e) {
			// show error panel
			CytoPanel p = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
			JPanel errorPanel = new JPanel(new BorderLayout());
			JTextArea message = new JTextArea(e.getMessage());
			message.setEditable(false);
			JScrollPane viewport = new JScrollPane(message);
			errorPanel.add(viewport, BorderLayout.CENTER);
			p.add("INAT", errorPanel);
		}
	}

	/**
	 * Constructs the panel
	 * 
	 * @param plugin the plugin for which the control panel is constructed.
	 * @return
	 */
	private JPanel setupPanel(InatPlugin plugin) {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout(2, 2));

		// the button container
		JPanel buttons = new JPanel(new GridLayout(3, 1, 2, 2));

		JButton runButton = new JButton(new RunAction(plugin));
		buttons.add(runButton);

		panel.add(buttons, BorderLayout.NORTH);

		return panel;
	}
}
