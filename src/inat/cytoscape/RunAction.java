package inat.cytoscape;

import giny.model.Edge;
import giny.model.Node;
import inat.analyser.LevelResult;
import inat.analyser.SMCResult;
import inat.analyser.uppaal.ResultAverager;
import inat.analyser.uppaal.UppaalModelAnalyserFaster;
import inat.analyser.uppaal.VariablesModel;
import inat.exceptions.InatException;
import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.model.Scenario;
import inat.model.ScenarioMono;
import inat.network.UPPAALClient;
import inat.util.Table;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelImp;
import cytoscape.view.cytopanels.CytoPanelState;

/**
 * The run action runs the network through the INAT analyser.
 * 
 * @author Brend Wanders
 * 
 */
public class RunAction extends CytoscapeAction {
	private static final long serialVersionUID = -5018057013811632477L;
	private static final String NUMBER_OF_LEVELS = "levels",
								SECONDS_PER_POINT = "seconds per point",
								SCENARIO = "scenario",
								CANONICAL_NAME = "canonicalName",
								UNCERTAINTY = "uncertainty",
								ENABLED = "enabled";
	private int timeTo = 1200;
	private double scale = 0.2;
	private JRadioButton remoteUppaal, smcUppaal;
	private JCheckBox computeStdDev;
	private JFormattedTextField timeToFormula, nSimulationRuns;
	private JTextField serverName, serverPort, smcFormula;
	
	/**
	 * Constructor.
	 * 
	 * @param plugin the plugin we should use
	 */
	public RunAction(InatPlugin plugin, JRadioButton remoteUppaal, JTextField serverName, JTextField serverPort, JRadioButton smcUppaal, JFormattedTextField timeToFormula, JFormattedTextField nSimulationRuns, JCheckBox computeStdDev, JTextField smcFormula) {
		super("Analyse network");
		this.remoteUppaal = remoteUppaal;
		this.serverName = serverName;
		this.serverPort = serverPort;
		this.smcUppaal = smcUppaal;
		this.timeToFormula = timeToFormula;
		this.nSimulationRuns = nSimulationRuns;
		this.computeStdDev = computeStdDev;
		this.smcFormula = smcFormula;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		RunTask task = new RunTask();

		// Configure JTask Dialog Pop-Up Box
		JTaskConfig jTaskConfig = new JTaskConfig();
		jTaskConfig.setOwner(Cytoscape.getDesktop());
		// jTaskConfig.displayCloseButton(true);
		// jTaskConfig.displayCancelButton(true);

		jTaskConfig.displayStatus(true);
		jTaskConfig.setAutoDispose(true);
		jTaskConfig.displayCancelButton(true);
		jTaskConfig.displayTimeElapsed(true);
		
		// Execute Task in New Thread; pops open JTask Dialog Box.
		TaskManager.executeTask(task, jTaskConfig);
	}

	private class RunTask implements Task {

		private static final String TIMES_U = "timesU";
		private static final String TIMES_L = "timesL";
		private static final String REACTION_TYPE = "type";
		private static final String REACTANT_NAME = "name";
		private static final String REACTANT_ALIAS = "alias";
		private TaskMonitor monitor;

		@Override
		public String getTitle() {
			return "INAT analysis";
		}

		@Override
		public void halt() {
			// not implemented
		}

		@Override
		public void run() {
			try {
				
				this.monitor.setStatus("Creating model representation");
				this.monitor.setPercentCompleted(0);

				final Model model = this.getInatModel();
				
				if (smcUppaal.isSelected()) {
					performSMCAnalysis(model);
				} else {
					performNormalAnalysis(model);
				}
				
			} catch (InterruptedException e) {
				this.monitor.setException(e, "Analysis cancelled by the user.");
			} catch (Exception e) {
				this.monitor.setException(e, "An error occurred while analysing the network.");
			}
		}
		
		private void performSMCAnalysis(final Model model) throws Exception {
			//TODO: "understand" the formula and correctly change time values and reagent names
			String probabilisticFormula = smcFormula.getText();
			for (Reactant r : model.getReactants()) {
				String name = r.get(REACTANT_ALIAS).as(String.class);
				if (probabilisticFormula.contains(name)) {
					probabilisticFormula = probabilisticFormula.replace(name, r.getId());
				}
			}
			if (probabilisticFormula.contains("Pr[<")) {
	            String[] parts = probabilisticFormula.split("Pr\\[<");
	            StringBuilder sb = new StringBuilder();
	            for (String p : parts) {
	               if (p.length() < 1) continue;
	               String timeS;
	               if (p.startsWith("=")) {
	                  timeS = p.substring(1, p.indexOf("]"));
	               } else {
	                  timeS = p.substring(0, p.indexOf("]"));
	               }
	               int time;
	               try {
	                  time = Integer.parseInt(timeS);
	               } catch (Exception ex) {
	                  throw new Exception("Problems with the identification of time string \"" + timeS + "\"");
	               }
	               time = (int)(time * 60.0 / model.getProperties().get(SECONDS_PER_POINT).as(Double.class));
	               sb.append("Pr[<");
	               if (p.startsWith("=")) {
	                  sb.append("=");
	               }
	               sb.append(time);
	               sb.append(p.substring(p.indexOf("]")));
	            }
	            probabilisticFormula = sb.toString();
	         }

			
			this.monitor.setStatus("Analyzing model with UPPAAL");
			this.monitor.setPercentCompleted(-1);

			// analyse model
			final SMCResult result;
			
			if (remoteUppaal.isSelected()) {
				UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
				result = client.analyzeSMC(model, probabilisticFormula);
			} else {
				result = new UppaalModelAnalyserFaster(monitor).analyzeSMC(model, probabilisticFormula);
			}
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), result.toString(), "Result", JOptionPane.INFORMATION_MESSAGE);
				}
			});
			
		}
		
		private void performNormalAnalysis(final Model model) throws Exception {

			int nMinutesToSimulate = 0;
			try {
				nMinutesToSimulate = Integer.parseInt(timeToFormula.getValue().toString());
			} catch (Exception ex) {
				throw new Exception("Unable to understand the number of minutes requested for the simulation.");
			}
				/*(int)(timeTo * model.getProperties().get(SECONDS_PER_POINT).as(Double.class) / 60);
			String inputTime = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Up to which time (in real-life MINUTES)?", nMinutesToSimulate);
			if (inputTime != null) {
				try {
					nMinutesToSimulate = Integer.parseInt(inputTime);
				} catch (Exception ex) {
					//the default value is still there, so nothing to change
				}
			} else {
				return;
			}*/
			
			timeTo = (int)(nMinutesToSimulate * 60.0 / model.getProperties().get(SECONDS_PER_POINT).as(Double.class));
			scale = (double)nMinutesToSimulate / timeTo;
			
			//this.monitor.setStatus("Analyzing model with UPPAAL");
			this.monitor.setPercentCompleted(-1);

			// composite the analyser (this should be done from
			// configuration)
			//ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(), new VariablesModel());

			// analyse model
			final LevelResult result;
			
			if (remoteUppaal.isSelected()) {
				UPPAALClient client = new UPPAALClient(serverName.getText(), Integer.parseInt(serverPort.getText()));
				int nSims = 1;
				if (nSimulationRuns.isEnabled()) {
					try {
						nSims = Integer.parseInt(nSimulationRuns.getText());
					} catch (Exception e) {
						throw new Exception("Unable to understand the number of requested simulations.");
					}
				} else {
					nSims = 1;
				}
				result = client.analyze(model, timeTo, nSims, computeStdDev.isSelected());
			} else {
				//ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(), new VariablesModel());
				//result = analyzer.analyze(model, timeTo);
				if (nSimulationRuns.isEnabled()) {
					int nSims = 0;
					try {
						nSims = Integer.parseInt(nSimulationRuns.getText());
					} catch (Exception e) {
						throw new Exception("Unable to understand the number of requested simulations.");
					}
					result = new ResultAverager(monitor).analyzeAverage(model, timeTo, nSims, computeStdDev.isSelected());
				} else {
					result = new UppaalModelAnalyserFaster(monitor).analyze(model, timeTo);
				}
			}

			/*CsvWriter csvWriter = new CsvWriter();
			csvWriter.writeCsv("/tmp/test.csv", model, result);*/

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					final CytoPanel p = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);

					// JFrame frame = new JFrame("Inat result viewer");
					// frame.setLayout(new BorderLayout());
					InatResultPanel resultViewer = new InatResultPanel(model, result, scale);
					// frame.add(resultViewer, BorderLayout.CENTER);
					// frame.setLocationRelativeTo(Cytoscape.getDesktop());
					// frame.pack();
					// frame.setSize(new Dimension(800, 600));
					// frame.setVisible(true);

					final JPanel container = new JPanel(new BorderLayout(2, 2));
					container.add(resultViewer, BorderLayout.CENTER);
					JPanel buttons = new JPanel(new GridLayout(1, 4, 2, 2));

					JButton close = new JButton(new AbstractAction("Close") {
						private static final long serialVersionUID = 4327349309742276633L;

						@Override
						public void actionPerformed(ActionEvent e) {
							p.remove(container);
						}
					});

					buttons.add(close);
					container.add(buttons, BorderLayout.NORTH);

					p.add("INAT Results", container);

					if (p.getState().equals(CytoPanelState.HIDE)) {
						CytoPanelImp p1 = (CytoPanelImp)Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
						CyNetworkView p2 = Cytoscape.getCurrentNetworkView();
						CytoPanelImp p3 = (CytoPanelImp)Cytoscape.getDesktop().getCytoPanel(SwingConstants.SOUTH);
						Dimension d = Cytoscape.getDesktop().getSize();
						if (!p1.getState().equals(CytoPanelState.HIDE)) {
							d.width -= p1.getWidth();
						}
						if (p2 != null) {
							d.width -= Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(p2).getWidth();
						}
						if (!p3.getState().equals(CytoPanelState.HIDE)) {
							d.height -= p3.getHeight();
						}
						((CytoPanelImp)p).setPreferredSize(d);
						((CytoPanelImp)p).setMaximumSize(d);
						((CytoPanelImp)p).setSize(d);
						p.setState(CytoPanelState.DOCK);
					}
				}
			});
		}

		@Override
		public void setTaskMonitor(TaskMonitor monitor) throws IllegalThreadStateException {
			this.monitor = monitor;
		}

		@SuppressWarnings("unchecked")
		private Model getInatModel() throws InatException {
			Map<String, String> nodeNameToId = new HashMap<String, String>();
			Map<String, String> edgeNameToId = new HashMap<String, String>();
			
			Model model = new Model();
			
			CyNetwork network = Cytoscape.getCurrentNetwork();
			
			final int totalWork = network.getNodeCount() + network.getEdgeCount();
			int doneWork = 0;
			
			CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
			
			
			if (!networkAttributes.hasAttribute(network.getIdentifier(), NUMBER_OF_LEVELS)) {
				//throw new InatException("Network attribute '" + NUMBER_OF_LEVELS + "' is missing.");
				int defaultNLevels = 15;
				String inputLevels = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Missing number of levels for the network. Please insert the max number of levels", defaultNLevels);
				Integer nLvl;
				if (inputLevels != null) {
					try {
						nLvl = new Integer(inputLevels);
					} catch (Exception ex) {
						nLvl = defaultNLevels;
					}
				} else {
					nLvl = defaultNLevels;
				}
				networkAttributes.setAttribute(network.getIdentifier(), NUMBER_OF_LEVELS, nLvl);
			}
			model.getProperties().let(NUMBER_OF_LEVELS).be(networkAttributes.getAttribute(network.getIdentifier(), NUMBER_OF_LEVELS));
			
			if (!networkAttributes.hasAttribute(network.getIdentifier(), SECONDS_PER_POINT)) {
				//throw new InatException("Network attribute '" + SECONDS_PER_POINT + "' is missing.");
				double defaultSecondsPerPoint = 12;
				String inputSecs = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Missing number of seconds per point for the network.\nPlease insert the number of real-life seconds a simulation point will represent", defaultSecondsPerPoint);
				Double nSecPerPoint;
				if (inputSecs != null) {
					try {
						nSecPerPoint = new Double(inputSecs);
					} catch (Exception ex) {
						nSecPerPoint = defaultSecondsPerPoint;
					}
				} else {
					nSecPerPoint = defaultSecondsPerPoint;
				}
				networkAttributes.setAttribute(network.getIdentifier(), SECONDS_PER_POINT, nSecPerPoint);
			}
			model.getProperties().let(SECONDS_PER_POINT).be(networkAttributes.getAttribute(network.getIdentifier(), SECONDS_PER_POINT));
			
			
			final Integer MaxNLevels = networkAttributes.getIntegerAttribute(network.getIdentifier(), NUMBER_OF_LEVELS);
			final Double nSecondsPerPoint = networkAttributes.getDoubleAttribute(network.getIdentifier(), SECONDS_PER_POINT);
			
			model.getProperties().let(NUMBER_OF_LEVELS).be(MaxNLevels);
			model.getProperties().let(SECONDS_PER_POINT).be(nSecondsPerPoint);
			
			// do nodes first
			CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
			final Iterator<Node> nodes = (Iterator<Node>) network.nodesIterator();
			for (int i = 0; nodes.hasNext(); i++) {
				this.monitor.setPercentCompleted((100 * doneWork++) / totalWork);
				Node node = nodes.next();
				
				final String reactantId = "reactant" + i;
				Reactant r = new Reactant(reactantId);
				nodeNameToId.put(node.getIdentifier(), reactantId);
				
				// name the node
				r.let(REACTANT_NAME).be(node.getIdentifier());
				r.let(REACTANT_ALIAS).be(nodeAttributes.getAttribute(node.getIdentifier(), CANONICAL_NAME));
				r.let(NUMBER_OF_LEVELS).be(nodeAttributes.getAttribute(node.getIdentifier(), NUMBER_OF_LEVELS));
				if (nodeAttributes.hasAttribute(node.getIdentifier(), ENABLED)) {
					r.let(ENABLED).be(nodeAttributes.getAttribute(node.getIdentifier(), ENABLED));
				} else {
					r.let(ENABLED).be(true);
				}
				r.let("cytoscape id").be(node.getIdentifier());
				
				// set initial concentration level
				final Integer initialConcentration = nodeAttributes.getIntegerAttribute(node.getIdentifier(), "initialConcentration");
				
				if (initialConcentration == null) {
					throw new InatException("Node attribute 'initialConcentration' is missing on '" + node.getIdentifier() + "'");
				}
				
				r.let("initialConcentration").be(initialConcentration);
				
				model.add(r);
			}
			
			// do edges next
			CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
			final Iterator<Edge> edges = (Iterator<Edge>) network.edgesIterator();
			for (int i = 0; edges.hasNext(); i++) {
				this.monitor.setPercentCompleted((100 * doneWork++) / totalWork);
				Edge edge = edges.next();

				String reactionId = "reaction" + i;
				Reaction r = new Reaction(reactionId);
				edgeNameToId.put(edge.getIdentifier(), reactionId);
				
				if (edgeAttributes.hasAttribute(edge.getIdentifier(), ENABLED)) {
					r.let(ENABLED).be(edgeAttributes.getAttribute(edge.getIdentifier(), ENABLED));
				} else {
					r.let(ENABLED).be(true);
				}

				if (edge.getSource() == edge.getTarget()) {
					r.let(REACTION_TYPE).be("reaction1");

					final String reactant = nodeNameToId.get(edge.getTarget().getIdentifier());
					r.let("reactant").be(reactant);
					
					int nLevels;
					
					if (!model.getReactant(reactant).get(NUMBER_OF_LEVELS).isNull()) {
						nLevels = model.getReactant(reactant).get(NUMBER_OF_LEVELS).as(Integer.class);
					} else {
						nLevels = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
					}
					
					ScenarioMono scenario = new ScenarioMono();
					
					String[] parameters = scenario.listVariableParameters();
					for (int j = 0;j < parameters.length;j++) {
						scenario.setParameter(parameters[j], edgeAttributes.getDoubleAttribute(edge.getIdentifier(), parameters[j]));
					}
					
					Integer value = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY);
					int uncertainty;
					if (value == null) {
						uncertainty = 0;
					} else {
						uncertainty = value;
					}
					
					List<Integer> times = scenario.generateTimes(1 + nLevels);
					Table timesLTable = new Table(nLevels + 1, 1);
					Table timesUTable = new Table(nLevels + 1, 1);
					
					for (int j = 0; j < nLevels + 1; j++) {
						Integer t = times.get(j);
						if (t.equals(VariablesModel.INFINITE_TIME) || uncertainty == 0) {
							timesLTable.set(j, 0, t);
							timesUTable.set(j, 0, t);
						} else {
							timesLTable.set(j, 0, Math.max(1, (int)(t * (100.0 - uncertainty) / 100.0))); //we use Math.max because we do not want to put 0 as a time
							timesUTable.set(j, 0, Math.max(1, (int)(t * (100.0 + uncertainty) / 100.0)));
						}
					}
					r.let(TIMES_L).be(timesLTable);
					r.let(TIMES_U).be(timesUTable);

				} else {
					r.let(REACTION_TYPE).be("reaction2");

					final String reactant = nodeNameToId.get(edge.getTarget().getIdentifier());
					r.let("reactant").be(reactant);

					final String catalyst = nodeNameToId.get(edge.getSource().getIdentifier());
					r.let("catalyst").be(catalyst);
					
					int nLevelsR1,
						nLevelsR2;
					
					if (!model.getReactant(catalyst).get(NUMBER_OF_LEVELS).isNull()) {
						nLevelsR1 = model.getReactant(catalyst).get(NUMBER_OF_LEVELS).as(Integer.class);
					} else {
						nLevelsR1 = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
					}
					if (!model.getReactant(reactant).get(NUMBER_OF_LEVELS).isNull()) {
						nLevelsR2 = model.getReactant(reactant).get(NUMBER_OF_LEVELS).as(Integer.class);
					} else {
						nLevelsR2 = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
					}

					Scenario[] scenarios = Scenario.sixScenarios;
					Integer scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), SCENARIO);
					if (scenarioIdx == null) {
						scenarioIdx = 0;
					}
					Scenario scenario = scenarios[scenarioIdx];
					
					String[] parameters = scenario.listVariableParameters();
					for (int j = 0;j < parameters.length;j++) {
						scenario.setParameter(parameters[j], edgeAttributes.getDoubleAttribute(edge.getIdentifier(), parameters[j]));
					}
					
					Integer value = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), UNCERTAINTY);
					int uncertainty;
					if (value == null) {
						uncertainty = 0;
					} else {
						uncertainty = value;
					}
					
					List<Integer> times = scenario.generateTimes(1 + nLevelsR1, 1 + nLevelsR2);
					Table timesLTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
					Table timesUTable = new Table(nLevelsR2 + 1, nLevelsR1 + 1);
					
					for (int j = 0; j < nLevelsR2 + 1; j++) {
						for (int k = 0; k < nLevelsR1 + 1; k++) {
							Integer t = times.get(j * (nLevelsR1 + 1) + k);
							if (t.equals(VariablesModel.INFINITE_TIME) || uncertainty == 0) {
								timesLTable.set(j, k, t);
								timesUTable.set(j, k, t);
							} else {
								timesLTable.set(j, k, Math.max(1, (int)(t * (100.0 - uncertainty) / 100.0)));
								timesUTable.set(j, k, Math.max(1, (int)(t * (100.0 + uncertainty) / 100.0)));
							}
						}
					}
					r.let(TIMES_L).be(timesLTable);
					r.let(TIMES_U).be(timesUTable);
				}

				r.let("increment").be(getIncrement(network, edge));


				model.add(r);
			}
			
			
			//check that the number of levels is present in each reactant
			Integer defNumberOfLevels = model.getProperties().get(NUMBER_OF_LEVELS).as(Integer.class);
			for (Reactant r : model.getReactants()) {
				Integer nLvl = r.get(NUMBER_OF_LEVELS).as(Integer.class);
				if (nLvl == null) {
					Property nameO = r.get(REACTANT_ALIAS);
					String name;
					if (nameO == null) {
						name = r.getId();
					} else {
						name = nameO.as(String.class);
					}
					String inputLevels = JOptionPane.showInputDialog("Missing number of levels for reactant \"" + name + "\" (" + r.getId() + ").\nPlease insert the max number of levels for \"" + name + "\"", defNumberOfLevels);
					if (inputLevels != null) {
						try {
							nLvl = new Integer(inputLevels);
						} catch (Exception ex) {
							nLvl = defNumberOfLevels;
						}
					} else {
						nLvl = defNumberOfLevels;
					}
					r.let(NUMBER_OF_LEVELS).be(nLvl);
					System.err.println("Numbero di livelli di " + r.get("cytoscape id").as(String.class) + " = " + nLvl);
					nodeAttributes.setAttribute(r.get("cytoscape id").as(String.class), NUMBER_OF_LEVELS, nLvl);
				}
			}
			
			
			return model;
		}

		private int getIncrement(CyNetwork network, Edge edge) {
			/*final String interaction = Semantics.getInteractionType(network, edge);
			if (interaction.equals("activates")) {
				return +1;
			} else {
				return -1;
			}*/
			CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
			return (Integer) edgeAttributes.getAttribute(edge.getIdentifier(), "increment");
		}
	}
}
