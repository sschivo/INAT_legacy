package inat.cytoscape;

import inat.analyser.LevelResult;
import inat.graph.Graph;
import inat.graph.P;
import inat.model.Model;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;

public class InatResultPanel extends JPanel implements ChangeListener {

	private final Model model;
	private final LevelResult result;
	private JSlider slider;

	public InatResultPanel(Model model, LevelResult result) {
		super(new BorderLayout(), true);
		this.model = model;
		this.result = result;

		JPanel sliderPanel = new JPanel(new BorderLayout());
		this.slider = new JSlider();
		this.slider.setOrientation(JSlider.HORIZONTAL);
		this.slider.setMinimum(0);
		this.slider.setMaximum(result.getTimeIndices().get(result.getTimeIndices().size() - 1));
		this.slider.setValue(0);
		this.slider.getModel().addChangeListener(this);

		sliderPanel.add(this.slider, BorderLayout.CENTER);

		this.add(sliderPanel, BorderLayout.SOUTH);

		Graph g = new Graph();
		for (String r : result.getReactantIds()) {
			P[] series = new P[result.getTimeIndices().size()];

			int i = 0;
			for (int t : result.getTimeIndices()) {
				series[i++] = new P(t, result.getConcentration(r, t));
			}

			g.addSeries(series, model.getReactant(r).get("name").as(String.class));
		}
		this.add(g, BorderLayout.CENTER);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		final int t = this.slider.getValue();
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		for (String r : result.getReactantIds()) {
			final String id = model.getReactant(r).get("name").as(String.class);
			final int level = result.getConcentration(r, t);
			nodeAttributes.setAttribute(id, "concentration", level);
		}

		Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);

		Cytoscape.getCurrentNetworkView().updateView();
	}
}
