package inat.cytoscape;

import inat.analyser.LevelResult;
import inat.graph.Graph;
import inat.model.Model;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

/**
 * The Inat result panel.
 * 
 * @author Brend Wanders
 */
public class InatResultPanel extends JPanel implements ChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -163756255393221954L;
	private final Model model;
	private final LevelResult result;
	private JSlider slider;

	/**
	 * The panel constructor.
	 * 
	 * @param model the model this panel uses
	 * @param result the results object this panel uses
	 */
	public InatResultPanel(Model model, LevelResult result, double scale) {
		super(new BorderLayout(), true);
		this.model = model;
		this.result = result;

		JPanel sliderPanel = new JPanel(new BorderLayout());
		this.slider = new JSlider();
		this.slider.setOrientation(JSlider.HORIZONTAL);
		this.slider.setMinimum(0);
		this.slider.setMaximum((int)(double)(result.getTimeIndices().get(result.getTimeIndices().size() - 1)));
		this.slider.setValue(0);
		this.slider.getModel().addChangeListener(this);

		sliderPanel.add(this.slider, BorderLayout.CENTER);

		this.add(sliderPanel, BorderLayout.SOUTH);

		Graph g = new Graph();
		/*for (String r : result.getReactantIds()) {
			System.err.println("Sistemo il grafico per il reactante \"" + r + "\"");
			P[] series = new P[result.getTimeIndices().size()];

			int i = 0;
			for (double t : result.getTimeIndices()) {
				series[i++] = new P(t * scale, result.getConcentration(r, t));
			}
			
			String name = null;
			if (model.getReactant(r) != null) { //we can also refer to a name not present in the reactant collection
				name = model.getReactant(r).get("alias").as(String.class); //if an alias is set, we prefer it
				if (name == null) {
					name = model.getReactant(r).get("name").as(String.class);
				}
			} else if (r.contains("_StdDev")) {
				if (model.getReactant(r.substring(0, r.indexOf("_"))).get("alias").as(String.class) != null) {
					name = model.getReactant(r.substring(0, r.indexOf("_"))).get("alias").as(String.class) + "_StdDev";
				} else {
					name = r; //in this case, I simply don't know what we are talking about =)
				}
			}

			g.addSeries(series, name);
		}*/
		Map<String, String> seriesNameMapping = new HashMap<String, String>();
		for (String r : result.getReactantIds()) {
			String name = null;
			if (model.getReactant(r) != null) { //we can also refer to a name not present in the reactant collection
				name = model.getReactant(r).get("alias").as(String.class); //if an alias is set, we prefer it
				if (name == null) {
					name = model.getReactant(r).get("name").as(String.class);
				}
			} else if (r.contains("_StdDev")) {
				if (model.getReactant(r.substring(0, r.indexOf("_"))).get("alias").as(String.class) != null) {
					name = model.getReactant(r.substring(0, r.indexOf("_"))).get("alias").as(String.class) + "_StdDev";
				} else {
					name = r; //in this case, I simply don't know what we are talking about =)
				}
			}
			seriesNameMapping.put(r, name);
		}
		g.parseLevelResult(result, seriesNameMapping, scale);
		g.setXSeriesName("Time (min)");
		if (!model.getProperties().get("levels").isNull()) { //if we find a maximum value for activity levels, we declare it to the graph, so that other added graphs (such as experimental data) will be automatically rescaled to match us
			g.declareMaxYValue(model.getProperties().get("levels").as(Integer.class));
		}
		this.add(g, BorderLayout.CENTER);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		final int t = this.slider.getValue();
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		for (String r : this.result.getReactantIds()) {
			if (this.model.getReactant(r) == null) continue;
			final String id = this.model.getReactant(r).get("name").as(String.class);
			final int level = (int)this.result.getConcentration(r, t);
			nodeAttributes.setAttribute(id, "concentration", level);
		}

		Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

		Cytoscape.getCurrentNetworkView().updateView();
	}
}
