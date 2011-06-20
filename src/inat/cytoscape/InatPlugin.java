package inat.cytoscape;

import inat.InatBackend;
import inat.exceptions.InatException;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.attr.MultiHashMapListener;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;

/**
 * The INAT Cytoscape plugin main class.
 * 
 * @author Brend Wanders
 */
public class InatPlugin extends CytoscapePlugin {
	private final File configurationFile;
	private static final String SECONDS_PER_POINT = "seconds per point";

	/**
	 * Mandatory constructor.
	 */
	public InatPlugin() {
		this.configurationFile = CytoscapeInit.getConfigFile("inat-configuration.xml");

		try {
			InatBackend.initialise(this.configurationFile);
			System.setProperty("java.security.policy", CytoscapeInit.getConfigFile("inat-security.policy").getAbsolutePath());

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
		JPanel buttons = new JPanel(new GridLayout(5, 1, 2, 2));
		
		Box uppaalBox = new Box(BoxLayout.Y_AXIS);
		final JRadioButton localUppaal = new JRadioButton("Local"),
					 remoteUppaal = new JRadioButton("Remote");
		ButtonGroup uppaalGroup = new ButtonGroup();
		uppaalGroup.add(localUppaal);
		uppaalGroup.add(remoteUppaal);
		final Box serverBox = new Box(BoxLayout.X_AXIS);
		final JTextField serverName = new JTextField("ewi1735.ewi.utwente.nl"),
				   serverPort = new JFormattedTextField("1234");
		remoteUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean sel = remoteUppaal.isSelected();
				serverName.setEnabled(sel);
				serverPort.setEnabled(sel);
			}
		});
		localUppaal.setSelected(true);
		remoteUppaal.setSelected(false);
		serverName.setEnabled(false);
		serverPort.setEnabled(false);
		serverBox.add(serverName);
		serverBox.add(serverPort);
		Box localUppaalBox = new Box(BoxLayout.X_AXIS);
		localUppaalBox.add(Box.createHorizontalStrut(5));
		localUppaalBox.add(localUppaal);
		localUppaalBox.add(Box.createGlue());
		uppaalBox.add(localUppaalBox);
		//uppaalBox.add(remoteUppaal);
		remoteUppaal.setOpaque(true);
		final ComponentTitledBorder border = new ComponentTitledBorder(remoteUppaal, serverBox, BorderFactory.createEtchedBorder());
		localUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				serverBox.repaint();
			}
		});
		serverBox.setBorder(border);
		uppaalBox.add(serverBox);
		buttons.add(uppaalBox);
		
		final JRadioButton normalUppaal = new JRadioButton("Simulation"),
						   smcUppaal = new JRadioButton("SMC");
		ButtonGroup modelCheckingGroup = new ButtonGroup();
		modelCheckingGroup.add(normalUppaal);
		modelCheckingGroup.add(smcUppaal);
		
		final JCheckBox computeAverage = new JCheckBox("Average of");
		
		final JFormattedTextField timeTo = new JFormattedTextField(240);
		final JFormattedTextField nSimulationRuns = new JFormattedTextField(10);
		final JTextField smcFormula = new JTextField("Pr[<=50](<> MK2 > 50)");
		timeTo.setToolTipText("Plot activity levels up to this time point (real-life MINUTES).");
		nSimulationRuns.setToolTipText("Number of simulations of which to show the average. NO statistical guarantees!");
		smcFormula.setToolTipText("Give an answer to this probabilistic query (times in real-life MINUTES).");
		normalUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (normalUppaal.isSelected()) {
					timeTo.setEnabled(true);
					computeAverage.setEnabled(true);
					nSimulationRuns.setEnabled(computeAverage.isSelected());
					smcFormula.setEnabled(false);
				} else {
					timeTo.setEnabled(false);
					computeAverage.setEnabled(false);
					nSimulationRuns.setEnabled(false);
					smcFormula.setEnabled(true);
				}
			}
		});
		computeAverage.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (computeAverage.isSelected() && normalUppaal.isSelected()) {
					nSimulationRuns.setEnabled(true);
				} else {
					nSimulationRuns.setEnabled(false);
				}
			}
		});
		normalUppaal.setSelected(true);
		smcUppaal.setSelected(false);
		timeTo.setEnabled(true);
		computeAverage.setEnabled(true);
		computeAverage.setSelected(false);
		nSimulationRuns.setEnabled(false);
		smcFormula.setEnabled(false);
		Box modelCheckingBox = new Box(BoxLayout.Y_AXIS);
		final Box normalBox = new Box(BoxLayout.Y_AXIS);
		Box smcBox = new Box(BoxLayout.X_AXIS);
		//Box normalUppaalBox = new Box(BoxLayout.X_AXIS);
		//normalUppaalBox.add(normalUppaal);
		//normalUppaalBox.add(Box.createGlue());
		//normalBox.add(normalUppaalBox);
		Box timeToBox = new Box(BoxLayout.X_AXIS);
		timeToBox.add(new JLabel("until"));
		timeToBox.add(timeTo);
		timeToBox.add(new JLabel("minutes"));
		normalBox.add(timeToBox);
		Box averageBox = new Box(BoxLayout.X_AXIS);
		averageBox.add(computeAverage);
		averageBox.add(nSimulationRuns);
		averageBox.add(new JLabel("runs"));
		normalBox.add(averageBox);
		smcBox.add(smcUppaal);
		smcBox.add(smcFormula);
		normalUppaal.setOpaque(true);
		final ComponentTitledBorder border2 = new ComponentTitledBorder(normalUppaal, normalBox, BorderFactory.createEtchedBorder());
		smcUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				normalBox.repaint();
			}
		});
		normalBox.setBorder(border2);
		modelCheckingBox.add(normalBox);
		modelCheckingBox.add(smcBox);
		buttons.add(modelCheckingBox);
		
		final JButton changeSecondsPerPointbutton = new JButton();
		changeSecondsPerPointbutton.setToolTipText("Click here to change the number of seconds to which a single simulation step corresponds");
		new ChangeSecondsAction(plugin, changeSecondsPerPointbutton);
		buttons.add(changeSecondsPerPointbutton);
		Cytoscape.getNetworkAttributes().getMultiHashMap().addDataListener(new MultiHashMapListener() {

			@Override
			public void allAttributeValuesRemoved(String arg0, String arg1) {
				
			}

			@Override
			public void attributeValueAssigned(String objectKey, String attributeName,
					Object[] keyIntoValue, Object oldAttributeValue, Object newAttributeValue) {
				
				if (attributeName.equals(SECONDS_PER_POINT)) {
					if (newAttributeValue != null) {
						changeSecondsPerPointbutton.setText(newAttributeValue + " seconds/step");
					} else {
						changeSecondsPerPointbutton.setText("Choose seconds/step");
					}
				}
			}

			@Override
			public void attributeValueRemoved(String arg0, String arg1,
					Object[] arg2, Object arg3) {
				
			}
		});

		JButton runButton = new JButton(new RunAction(plugin, remoteUppaal, serverName, serverPort, smcUppaal, timeTo, nSimulationRuns, smcFormula));
		buttons.add(runButton);
		
		JButton augmentButton = new JButton(new AugmentAction(plugin));
		buttons.add(augmentButton);

		panel.add(buttons, BorderLayout.NORTH);

		return panel;
	}
}
