package inat.cytoscape;

import java.awt.event.ActionEvent;

import javax.swing.AbstractButton;
import javax.swing.JOptionPane;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.util.CytoscapeAction;

public class ChangeSecondsAction extends CytoscapeAction {

	private static final long serialVersionUID = -9023326560269020342L;

	private static final String SECONDS_PER_POINT = "seconds per point";
	
	@SuppressWarnings("unused")
	private AbstractButton associatedButton;
	
	public ChangeSecondsAction(InatPlugin plugin, AbstractButton associatedButton) {
		this.associatedButton = associatedButton;
		associatedButton.setAction(this);
		CyNetwork network = Cytoscape.getCurrentNetwork();
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		
		if (!networkAttributes.hasAttribute(network.getIdentifier(), SECONDS_PER_POINT)) {
			associatedButton.setText("Choose seconds/step");
		} else {
			associatedButton.setText("" + networkAttributes.getDoubleAttribute(network.getIdentifier(), SECONDS_PER_POINT) + " seconds/step");
		}
	}

	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		CyNetwork network = Cytoscape.getCurrentNetwork();
		CyAttributes networkAttributes = Cytoscape.getNetworkAttributes();
		
		String message;
		double currentSecondsPerPoint;
		if (!networkAttributes.hasAttribute(network.getIdentifier(), SECONDS_PER_POINT)) {
			message = "Missing number of seconds per point for the network.\nPlease insert the number of real-life seconds a simulation point will represent";
			currentSecondsPerPoint = 12;
		} else {
			message = "Please insert the number of real-life seconds a simulation point will represent";
			currentSecondsPerPoint = networkAttributes.getDoubleAttribute(network.getIdentifier(), SECONDS_PER_POINT);
		}
		String inputSecs = JOptionPane.showInputDialog(message, currentSecondsPerPoint);
		Double nSecPerPoint;
		if (inputSecs != null) {
			try {
				nSecPerPoint = new Double(inputSecs);
			} catch (Exception ex) {
				nSecPerPoint = currentSecondsPerPoint;
			}
		} else {
			nSecPerPoint = currentSecondsPerPoint;
		}
		networkAttributes.setAttribute(network.getIdentifier(), SECONDS_PER_POINT, nSecPerPoint);
	}

}
