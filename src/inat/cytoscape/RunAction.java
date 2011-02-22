package inat.cytoscape;

import giny.model.Edge;
import giny.model.Node;
import inat.analyser.LevelResult;
import inat.analyser.ModelAnalyser;
import inat.analyser.uppaal.UppaalModelAnalyser;
import inat.analyser.uppaal.VariablesInterpreter;
import inat.analyser.uppaal.VariablesModel;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.serializer.CsvWriter;
import inat.util.Table;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.util.CytoscapeAction;

/**
 * The run action runs the network through the INAT analyser.
 * 
 * @author Brend Wanders
 * 
 */
public class RunAction extends CytoscapeAction {

	/**
	 * The plugin we were created for
	 */
	private final InatPlugin plugin;

	/**
	 * Constructor.
	 * 
	 * @param plugin the plugin we should use
	 */
	public RunAction(InatPlugin plugin) {
		super("Analyse network");
		this.plugin = plugin;
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

		// Execute Task in New Thread; pops open JTask Dialog Box.
		boolean succes = TaskManager.executeTask(task, jTaskConfig);
	}

	private class RunTask implements Task {

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

				this.monitor.setStatus("Analyzing model with UPPAAL");
				this.monitor.setPercentCompleted(-1);
				// composite the analyser (this should be done from
				// configuration)
				ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(),
						new VariablesModel());

				// analyse model
				final LevelResult result = analyzer.analyze(model);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						JFrame frame = new JFrame("Inat result viewer");
						frame.setLayout(new BorderLayout());
						InatResultPanel resultViewer = new InatResultPanel(model, result);
						frame.add(resultViewer, BorderLayout.CENTER);
						frame.setLocationRelativeTo(Cytoscape.getDesktop());
						frame.pack();
						frame.setSize(new Dimension(800, 600));
						frame.setVisible(true);
					}
				});

			} catch (Exception e) {
				this.monitor.setException(e, "An error occurred while analysing the network.");
			}
		}

		@Override
		public void setTaskMonitor(TaskMonitor monitor) throws IllegalThreadStateException {
			this.monitor = monitor;
		}

		private Model getInatModel() {
			Map<String, String> nodeNameToId = new HashMap<String, String>();
			Map<String, String> edgeNameToId = new HashMap<String, String>();

			Model model = new Model();

			CyNetwork network = Cytoscape.getCurrentNetwork();

			final int totalWork = network.getNodeCount() + network.getEdgeCount();
			int doneWork = 0;

			final int levels = Cytoscape.getNetworkAttributes().getIntegerAttribute(network.getIdentifier(), "levels");
			model.getProperties().let("levels").be(levels);

			// do nodes first
			CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
			@SuppressWarnings("unchecked")
			final Iterator<Node> nodes = (Iterator<Node>) network.nodesIterator();
			for (int i = 0; nodes.hasNext(); i++) {
				this.monitor.setPercentCompleted((100 * doneWork++) / totalWork);
				Node node = nodes.next();

				final String reactantId = "reactant" + i;
				Reactant r = new Reactant(reactantId);
				nodeNameToId.put(node.getIdentifier(), reactantId);

				// name the node
				r.let("name").be(node.getIdentifier());

				// set initial concentration level
				r.let("initialConcentration").be(
						nodeAttributes.getIntegerAttribute(node.getIdentifier(), "initialConcentration"));

				model.add(r);
			}

			// do edges next
			CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
			@SuppressWarnings("unchecked")
			final Iterator<Edge> edges = (Iterator<Edge>) network.edgesIterator();
			for (int i = 0; edges.hasNext(); i++) {
				this.monitor.setPercentCompleted((100 * doneWork++) / totalWork);
				Edge edge = edges.next();

				String reactionId = "reaction" + i;
				Reaction r = new Reaction(reactionId);
				edgeNameToId.put(edge.getIdentifier(), reactionId);

				if (edge.getSource() == edge.getTarget()) {
					r.let("type").be("reaction1");

					final String reactant = nodeNameToId.get(edge.getTarget().getIdentifier());
					r.let("reactant").be(reactant);

					final List<Integer> times = edgeAttributes.getListAttribute(edge.getIdentifier(), "times");
					final Table timesTable = new Table(levels + 1, 1);
					for (int j = 0; j < levels + 1; j++) {
						timesTable.set(j, 0, times.get(j));
					}
					r.let("times").be(timesTable);

					r.let("increment").be(getIncrement(network, edge));
				} else {
					r.let("type").be("reaction2");

					final String reactant = nodeNameToId.get(edge.getTarget().getIdentifier());
					r.let("reactant").be(reactant);

					final String catalyst = nodeNameToId.get(edge.getSource().getIdentifier());
					r.let("catalyst").be(catalyst);

					final List<Integer> times = edgeAttributes.getListAttribute(edge.getIdentifier(), "times");
					final Table timesTable = new Table(levels + 1, levels + 1);
					for (int j = 0; j < levels + 1; j++) {
						for (int k = 0; k < levels + 1; k++) {
							timesTable.set(j, k, times.get(k + j * (levels + 1)));
						}
					}
					r.let("times").be(timesTable);

					r.let("increment").be(getIncrement(network, edge));

					model.add(r);
				}

			}

			return model;
		}

		private int getIncrement(CyNetwork network, Edge edge) {
			final String interaction = Semantics.getInteractionType(network, edge);
			if (interaction.equals("activates")) {
				return +1;
			} else {
				return -1;
			}
		}
	}
}
