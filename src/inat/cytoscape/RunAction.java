package inat.cytoscape;

import giny.model.Edge;
import giny.model.Node;
import inat.analyser.LevelResult;
import inat.analyser.ModelAnalyser;
import inat.analyser.uppaal.UppaalModelAnalyser;
import inat.analyser.uppaal.VariablesInterpreter;
import inat.analyser.uppaal.VariablesModel;
import inat.exceptions.InatException;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
//import inat.serializer.CsvWriter;
import inat.serializer.XMLSerializer;
import inat.util.Table;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
//import cytoscape.data.Semantics;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.cytopanels.CytoPanel;

/**
 * The run action runs the network through the INAT analyser.
 * 
 * @author Brend Wanders
 * 
 */
public class RunAction extends CytoscapeAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5018057013811632477L;
	private int timeTo = 1200;
	private double scale = 0.2;
	
	/**
	 * Constructor.
	 * 
	 * @param plugin the plugin we should use
	 */
	public RunAction(InatPlugin plugin) {
		super("Analyse network");
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
		

		String inputTime = JOptionPane.showInputDialog("Up to which time (in model seconds)?", timeTo);
		if (inputTime != null) {
			try {
				timeTo = Integer.parseInt(inputTime);
			} catch (Exception ex) {
				timeTo = 1200;
			}
		}
		
		String inputScale = JOptionPane.showInputDialog("Scale (1 model second = ? real seconds)", scale);
		if (inputScale != null) {
			try {
				scale = Double.parseDouble(inputScale);
			} catch (Exception ex) {
				scale = 0.2;
			}
		}
		

		// Execute Task in New Thread; pops open JTask Dialog Box.
		TaskManager.executeTask(task, jTaskConfig);
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

				XMLSerializer serializer = new XMLSerializer();
				Document doc = serializer.serializeModel(model);

				Source source = new DOMSource(doc);
				FileWriter stringWriter = new FileWriter("/tmp/test.xml");
				Result streamout = new StreamResult(stringWriter);
				TransformerFactory factory = TransformerFactory.newInstance();
				Transformer transformer = factory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
				transformer.transform(source, streamout);

				this.monitor.setStatus("Analyzing model with UPPAAL");
				this.monitor.setPercentCompleted(-1);

				// composite the analyser (this should be done from
				// configuration)
				ModelAnalyser<LevelResult> analyzer = new UppaalModelAnalyser(new VariablesInterpreter(),
						new VariablesModel());

				
				// analyse model
				final LevelResult result = analyzer.analyze(model, timeTo);

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
							/**
							 * 
							 */
							private static final long serialVersionUID = 4327349309742276633L;

							@Override
							public void actionPerformed(ActionEvent e) {
								p.remove(container);
							}
						});

						buttons.add(close);
						container.add(buttons, BorderLayout.NORTH);

						p.add("INAT Results", container);

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

		private Model getInatModel() throws InatException {
			Map<String, String> nodeNameToId = new HashMap<String, String>();
			Map<String, String> edgeNameToId = new HashMap<String, String>();

			Model model = new Model();

			CyNetwork network = Cytoscape.getCurrentNetwork();

			final int totalWork = network.getNodeCount() + network.getEdgeCount();
			int doneWork = 0;

			final Integer levels = Cytoscape.getNetworkAttributes().getIntegerAttribute(network.getIdentifier(),
					"levels");

			if (levels == null) {
				throw new InatException("Network attribute 'levels' is missing.");
			}

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
				r.let("alias").be(nodeAttributes.getAttribute(node.getIdentifier(), "canonicalName"));

				// set initial concentration level
				final Integer initialConcentration = nodeAttributes.getIntegerAttribute(node.getIdentifier(),
						"initialConcentration");

				if (initialConcentration == null) {
					throw new InatException("Node attribute 'initialConcentration' is missing on '"
							+ node.getIdentifier() + "'");
				}

				r.let("initialConcentration").be(initialConcentration);

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

					@SuppressWarnings("unchecked")
					final List<Integer> times = (List<Integer>) edgeAttributes.getListAttribute(edge.getIdentifier(),
							"times");

					if (times.size() != levels + 1) {
						throw new InatException("Edge attribute 'times' on edge '" + edge.getIdentifier()
								+ "' is not of the correct size.");
					}

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

					@SuppressWarnings("unchecked")
					final List<Integer> times = (List<Integer>) edgeAttributes.getListAttribute(edge.getIdentifier(),
							"times");
					if (times == null || times.size() != (levels + 1) * (levels + 1)) {
						if (times == null) {
							throw new InatException("Edge attribute 'times' is missing on edge '" + edge.getIdentifier() + "'.");
						} else {
							throw new InatException("Edge attribute 'times' on edge '" + edge.getIdentifier()
									+ "' is long " + times.size() + ", and so is not of the correct size (which should be " + ((levels+1)*(levels+1)) + ").");
						}
					}
					final Table timesTable = new Table(levels + 1, levels + 1);
					for (int j = 0; j < levels + 1; j++) {
						for (int k = 0; k < levels + 1; k++) {
							timesTable.set(j, k, times.get(k + j * (levels + 1)));
						}
					}
					r.let("times").be(timesTable);

					r.let("increment").be(getIncrement(network, edge));

				}

				model.add(r);
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
