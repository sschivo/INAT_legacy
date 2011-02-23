package inat.cytoscape;

import giny.view.EdgeView;
import giny.view.NodeView;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import cytoscape.Cytoscape;
import cytoscape.util.CytoscapeAction;
import ding.view.EdgeContextMenuListener;
import ding.view.NodeContextMenuListener;

/**
 * Augments the current view with INAT capabilities.
 * 
 * @author Brend Wanders
 * 
 */
public class AugmentAction extends CytoscapeAction implements NodeContextMenuListener, EdgeContextMenuListener {

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
	}

	@Override
	public void addNodeContextMenuItems(final NodeView nodeView, JPopupMenu menu) {
		if (menu != null) {
			menu.add(new AbstractAction("[INAT] Edit reactant...") {
				@Override
				public void actionPerformed(ActionEvent e) {
					NodeDialog dialog = new NodeDialog(nodeView.getNode());
					dialog.pack();
					dialog.setLocationRelativeTo(Cytoscape.getDesktop());
					dialog.setVisible(true);
				}
			});
		}
	}

	@Override
	public void addEdgeContextMenuItems(final EdgeView edgeView, JPopupMenu menu) {
		if (menu != null) {
			menu.add(new AbstractAction("[INAT] Edit reaction...") {
				@Override
				public void actionPerformed(ActionEvent e) {
					EdgeDialog dialog = new EdgeDialog(edgeView.getEdge());
					dialog.pack();
					dialog.setLocationRelativeTo(Cytoscape.getDesktop());
					dialog.setVisible(true);
				}
			});
		}
	}
}
