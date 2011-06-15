package inat.cytoscape;

import giny.model.Edge;
import giny.model.Node;
import giny.view.EdgeView;
import giny.view.NodeView;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.CyNetworkView;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.calculators.GenericNodeCustomGraphicCalculator;
import cytoscape.visual.mappings.DiscreteMapping;
import ding.view.EdgeContextMenuListener;
import ding.view.NodeContextMenuListener;

/**
 * Augments the current view with INAT capabilities.
 * 
 * @author Brend Wanders
 * 
 */
public class AugmentAction extends CytoscapeAction implements NodeContextMenuListener, EdgeContextMenuListener {

	private static final long serialVersionUID = 4874100514822653655L;
	
	private static final String ENABLED = "enabled";

	/**
	 * Constructor.
	 * 
	 * @param plugin the plugin to use
	 */
	public AugmentAction(InatPlugin plugin) {
		super("Augment view");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Cytoscape.getCurrentNetworkView().addNodeContextMenuListener(this);
		Cytoscape.getCurrentNetworkView().addEdgeContextMenuListener(this);
	
		
		//The following two are my attempts to use visual mapping APIs to set up at least the color scale based on activity levels
		//One is actually present, setting a discrete mapping for the "enabled" attribute (both for node and edge) that reduces the
		//opacity when the object is disabled.
		
		//Tentativo tratto dal sito http://cytoscape.wodaklab.org/wiki/plugin_developer_tutorial#How_to_use_the_VizMapper_programmatically.3F
//		VisualMappingManager vmm = Cytoscape.getVisualMappingManager();
//		Set<String> names = vmm.getCalculatorCatalog().getVisualStyleNames();
//		String vsName = "Example Visual Style";
//		VisualPropertyType type = VisualPropertyType.NODE_FILL_COLOR;
//		final Object defaultObj = type.getDefault(Cytoscape.getVisualMappingManager().getVisualStyle());
//
//		ContinuousMapping cm = new ContinuousMapping(defaultObj, ObjectMapping.NODE_MAPPING);
//		// Set controlling Attribute
//		cm.setControllingAttributeName("concentration", Cytoscape.getCurrentNetwork(), false);
//
//		Interpolator numToColor = new LinearNumberToColorInterpolator();
//		cm.setInterpolator(numToColor);
//
//		Color underColor = Color.GRAY;
//		Color minColor = Color.RED;
//		Color midColor = Color.WHITE;
//		Color maxColor = Color.GREEN;
//		Color overColor = Color.BLUE;
//
//		BoundaryRangeValues bv0 = new BoundaryRangeValues(underColor, minColor, minColor);
//		BoundaryRangeValues bv1 = new BoundaryRangeValues(midColor, midColor, midColor);
//		BoundaryRangeValues bv2 = new BoundaryRangeValues(maxColor, maxColor, overColor);
//
//		// Set the attribute point values associated with the boundary values
//		// The points p1, p2, p3 are the values between (min~max) of the degree
//		cm.addPoint(0.0, bv0); cm.addPoint(7.5, bv1); cm.addPoint(15.0, bv2);
//
//		// Create a calculator
//		BasicCalculator myCalculator = new BasicCalculator("My degree calcualtor", cm, VisualPropertyType.NODE_FILL_COLOR);
//		CyNetwork network = Cytoscape.getCurrentNetwork();
//        CyNetworkView networkView = Cytoscape.getCurrentNetworkView();
//
//        // get the VisualMappingManager and CalculatorCatalog
//        VisualMappingManager manager = Cytoscape.getVisualMappingManager();
//        CalculatorCatalog catalog = manager.getCalculatorCatalog();
//
//        // check to see if a visual style with this name already exists
//        VisualStyle vs = catalog.getVisualStyle(vsName);
//        vs.getNodeAppearanceCalculator().setCalculator(myCalculator);
//        
//        networkView.setVisualStyle(vs.getName()); // not strictly necessary
//
//        // actually apply the visual style
//        manager.setVisualStyle(vs);
//        networkView.redrawGraph(true,true);
		
		
		
		
        
//		//Tentativo fatto a mano
		VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
		NodeAppearanceCalculator nac = vizMap.getVisualStyle().getNodeAppearanceCalculator();
//		System.err.println("Ecco la descrizione del node appearance calculator: " + nac.getDescription());
//		List<Calculator> calculatori = nac.getCalculators();
//		for (Calculator calc : calculatori) {
//			System.err.println("\tCalculatore \"" + calc + "\". Proprieta':");
//			Properties prop = calc.getProperties();
//			for (Object k : prop.keySet()) {
//				System.err.println("\t\t" + k + "[" + k.getClass().getCanonicalName() + "] --> " + prop.get(k) + "[" + prop.get(k).getClass().getCanonicalName() + "]");
//			}
//			System.err.println("\tMappings del calculatore " + calc + ":");
//			Vector<ObjectMapping> mappi = calc.getMappings();
//			for (ObjectMapping m : mappi) {
//				System.err.println("\t\t" + m + " legato all'attributo \"" + m.getControllingAttributeName() + "\"");
//				Properties pp = m.getProperties("tua sorella zoccola");
//				for (Object k : pp.keySet()) {
//					System.err.println("\t\t" + k + " --> " + pp.get(k));
//				}
//				//JFrame frame = new JFrame("Mapping per " + VisualPropertyType.NODE_FILL_COLOR.getPropertyLabel() + " del mapping " + m + " del calculatore " + calc);
//				//frame.getContentPane().add(m.getLegend(VisualPropertyType.NODE_FILL_COLOR));
//				//frame.setBounds(20, 20, 200, 200);
//				//frame.setVisible(true);
//			}
//			System.err.println();
//		}
		DiscreteMapping mapping = new DiscreteMapping(Color.class, "enabled");
		mapping.putMapValue(false, Color.BLACK);
		mapping.putMapValue(true, Color.WHITE);
		Calculator calco = new GenericNodeCustomGraphicCalculator("Nome del basico calculatore di cui poco mi curo", mapping, VisualPropertyType.NODE_LABEL_COLOR);
		nac.setCalculator(calco);
//		//vizMap.applyAppearances();
//		if (Cytoscape.getNetworkAttributes().hasAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), "levels")) {
//			int maxNLivelli = Cytoscape.getNetworkAttributes().getIntegerAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), "levels");
//			
//			//nac.removeCalculator(VisualPropertyType.NODE_FILL_COLOR);
////			Cytoscape.getVisualMappingManager().getCalculatorCatalog().removeMapping("concentration");
////			Cytoscape.getVisualMappingManager().getCalculatorCatalog().removeCalculator(VisualPropertyType.NODE_FILL_COLOR, "concentration");
////			Collection<Calculator> col = Cytoscape.getVisualMappingManager().getCalculatorCatalog().getCalculators(VisualPropertyType.NODE_FILL_COLOR);
////			Calculator[] ararar = (Calculator[])col.toArray(new Calculator[1]);
////			for (Calculator cc : ararar) {
////				System.err.println("Provo a rimuovere " + cc);
////				Cytoscape.getVisualMappingManager().getCalculatorCatalog().removeCalculator(VisualPropertyType.NODE_FILL_COLOR, cc.toString());
////			}
//			
////			vizMap.getVisualStyle().setGlobalAppearanceCalculator(null);
////			ContinuousMapping mc = new ContinuousMapping(Color.class, "concentration");
////			mc.addPoint(0.0, new BoundaryRangeValues(Color.RED, Color.RED, Color.YELLOW));
////			mc.addPoint(maxNLivelli / 2.0, new BoundaryRangeValues(Color.RED, Color.YELLOW, Color.GREEN.darker()));
////			mc.addPoint(maxNLivelli * 1.0, new BoundaryRangeValues(Color.YELLOW, Color.GREEN.darker(), Color.GREEN.darker()));
////			mc.setInterpolator(new LinearNumberToColorInterpolator());
////			nac.setCalculator(new GenericNodeCustomGraphicCalculator("Nome del pit", mc, VisualPropertyType.NODE_FILL_COLOR));
//			
////			for (Calculator calc : nac.getCalculators()) {
////				for (ObjectMapping m : calc.getMappings()) {
////					if (m.getControllingAttributeName().equals("concentration")) {
////						if (m.getClass().equals(ContinuousMapping.class)) {
////							/*System.err.println("Ecco i boundari del simpatico mapping per la concentrazione:");
////							for (ContinuousMappingPoint p : mc.getAllPoints()) {
////								System.err.println(p.getValue());
////							}*/
////							/*List<ContinuousMappingPoint> punti = mc.getAllPoints();
////							if (punti.size() == 3) {
////								punti.get(1).setValue(maxNLivelli / 2.0);
////								punti.get(2).setValue(maxNLivelli * 1.0);
////							}*/
////							nac.removeCalculator(VisualPropertyType.NODE_FILL_COLOR);
//							ContinuousMapping mc = new ContinuousMapping(Color.class, "concentration");
//							mc.addPoint(0.0, new BoundaryRangeValues(Color.RED, Color.RED, Color.YELLOW));
//							mc.addPoint(maxNLivelli / 2.0, new BoundaryRangeValues(Color.RED, Color.YELLOW, Color.GREEN.darker()));
//							mc.addPoint(maxNLivelli * 1.0, new BoundaryRangeValues(Color.YELLOW, Color.GREEN.darker(), Color.GREEN.darker()));
//							nac.setCalculator(new GenericNodeCustomGraphicCalculator("Nome del pit", mc, VisualPropertyType.NODE_FILL_COLOR));
////						}
////					}
////				}
////			}
//			vizMap.applyAppearances();
//		}
	}

	@Override
	public void addNodeContextMenuItems(final NodeView nodeView, JPopupMenu menu) {
		if (menu != null) {
			menu.add(new AbstractAction("[INAT] Edit reactant...") {
				private static final long serialVersionUID = 6233177439944893232L;

				@Override
				public void actionPerformed(ActionEvent e) {
					NodeDialog dialog = new NodeDialog(nodeView.getNode());
					dialog.pack();
					dialog.setLocationRelativeTo(Cytoscape.getDesktop());
					dialog.setVisible(true);
				}
			});
			
			menu.add(new AbstractAction("Enable/disable") {
				private static final long serialVersionUID = 2579035100338148305L;

				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(ActionEvent e) {
					CyNetwork network = Cytoscape.getCurrentNetwork();
					CyNetworkView view = Cytoscape.getCurrentNetworkView();
					CyAttributes nodeAttr = Cytoscape.getNodeAttributes(),
								 edgeAttr = Cytoscape.getEdgeAttributes();
					for (Iterator i = view.getSelectedNodes().iterator(); i.hasNext(); ) {
						NodeView nView = (NodeView)i.next();
						CyNode node = (CyNode)nView.getNode();
						boolean status;
						if (!nodeAttr.hasAttribute(node.getIdentifier(), ENABLED)) {
							status = false;
						} else {
							status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), ENABLED);
						}
						nodeAttr.setAttribute(node.getIdentifier(), ENABLED, status);
						int [] adjacentEdges = network.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(), true, true, true);
						for (int edgeIdx : adjacentEdges) {
							CyEdge edge = (CyEdge)view.getEdgeView(edgeIdx).getEdge();
							edgeAttr.setAttribute(edge.getIdentifier(), ENABLED, status);
						}
					}
					if (view.getSelectedNodes().isEmpty()) { //if the user wanted to change only one node, here we go
						Node node = nodeView.getNode();
						boolean status;
						if (!nodeAttr.hasAttribute(node.getIdentifier(), ENABLED)) {
							status = false;
						} else {
							status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), ENABLED);
						}
						nodeAttr.setAttribute(node.getIdentifier(), ENABLED, status);
						int [] adjacentEdges = network.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(), true, true, true);
						for (int edgeIdx : adjacentEdges) {
							CyEdge edge = (CyEdge)view.getEdgeView(edgeIdx).getEdge();
							edgeAttr.setAttribute(edge.getIdentifier(), ENABLED, status);
						}
					}
					//In order to keep the model consistent, we disable all edges coming from (or going into) disabled nodes
					for (int i : network.getEdgeIndicesArray()) {
						CyEdge edge = (CyEdge)view.getEdgeView(i).getEdge();
						CyNode source = (CyNode)edge.getSource(),
							   target = (CyNode)edge.getTarget();
						if ((nodeAttr.hasAttribute(source.getIdentifier(), ENABLED) && !nodeAttr.getBooleanAttribute(source.getIdentifier(), ENABLED))
							|| (nodeAttr.hasAttribute(target.getIdentifier(), ENABLED) && !nodeAttr.getBooleanAttribute(target.getIdentifier(), ENABLED))) {
							edgeAttr.setAttribute(edge.getIdentifier(), ENABLED, false);
						}
					}
					Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
				}
			});
		}
	}

	@Override
	public void addEdgeContextMenuItems(final EdgeView edgeView, JPopupMenu menu) {
		if (menu != null) {
			menu.add(new AbstractAction("[INAT] Edit reaction...") {
				private static final long serialVersionUID = -5725775462053708399L;

				@Override
				public void actionPerformed(ActionEvent e) {
					EdgeDialog dialog = new EdgeDialog(edgeView.getEdge());
					dialog.pack();
					dialog.setLocationRelativeTo(Cytoscape.getDesktop());
					dialog.setVisible(true);
				}
			});
			
			menu.add(new AbstractAction("Enable/disable") {

				private static final long serialVersionUID = -1078261166495178010L;

				@SuppressWarnings("unchecked")
				@Override
				public void actionPerformed(ActionEvent e) {
					CyNetworkView view = Cytoscape.getCurrentNetworkView();
					CyAttributes edgeAttr = Cytoscape.getEdgeAttributes();
					for (Iterator i = view.getSelectedEdges().iterator(); i.hasNext(); ) {
						EdgeView eView = (EdgeView)i.next();
						CyEdge edge = (CyEdge)eView.getEdge();
						boolean status;
						if (!edgeAttr.hasAttribute(edge.getIdentifier(), ENABLED)) {
							status = false;
						} else {
							status = !edgeAttr.getBooleanAttribute(edge.getIdentifier(), ENABLED);
						}
						edgeAttr.setAttribute(edge.getIdentifier(), ENABLED, status);
					}
					if (view.getSelectedEdges().isEmpty()) { //if the user wanted to change only one edge, here we go
						Edge edge = edgeView.getEdge();
						boolean status;
						if (!edgeAttr.hasAttribute(edge.getIdentifier(), ENABLED)) {
							status = false;
						} else {
							status = !edgeAttr.getBooleanAttribute(edge.getIdentifier(), ENABLED);
						}
						edgeAttr.setAttribute(edge.getIdentifier(), ENABLED, status);
					}
					Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
				}
				
			});
		}
	}
}
