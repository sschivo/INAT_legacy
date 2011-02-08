package nl.utwente.exbio.brend;

import inat.InatBackend;
import inat.analyzer.LevelResult;
import inat.analyzer.ModelAnalyzer;
import inat.analyzer.uppaal.UppaalModelAnalyzer;
import inat.analyzer.uppaal.VariablesInterpreter;
import inat.analyzer.uppaal.VariablesModel;
import inat.exceptions.InatException;
import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.Table;

import java.io.File;

/**
 * The UPPAAL test class.
 * 
 * @author Brend Wanders
 * 
 */
public class UPPAALTest {
	/**
	 * Program entry point.
	 * 
	 * @param args the command line arguments
	 * @throws InatException if the system could not be initialised
	 */
	public static void main(String[] args) throws InatException {
		InatBackend.initialise(new File("inat-configuration.xml"));

		// create model
		Model model = new Model();
		model.getProperties().let("levels").be(4);

		Reactant a = new Reactant("a");
		a.let("name").be("A");
		a.let("initialConcentration").be(3);
		model.add(a);

		Reactant b = new Reactant("b");
		b.let("name").be("B");
		b.let("initialConcentration").be(1);
		model.add(b);

		Reaction deg = new Reaction("aDeg");
		deg.let("type").be("reaction1");
		deg.let("reactant").be("a");
		deg.let("increment").be(-1);
		Table deactivationTable = new Table(4 + 1, 1);
		deactivationTable.set(0, 0, -1);
		deactivationTable.set(1, 0, 150);
		deactivationTable.set(2, 0, 130);
		deactivationTable.set(3, 0, 120);
		deactivationTable.set(4, 0, 110);
		deg.let("times").be(deactivationTable);
		model.add(deg);

		Reaction degb = new Reaction("bDeg");
		degb.let("type").be("reaction1");
		degb.let("reactant").be("b");
		degb.let("increment").be(-1);
		degb.let("times").be(deactivationTable);
		model.add(degb);

		Reaction r = new Reaction("foo");
		r.let("type").be("reaction2");
		r.let("reactant").be("b");
		r.let("catalyst").be("a");
		r.let("increment").be(+1);
		Table reactionTable = new Table(4 + 1, 4 + 1);
		for (int i = 0; i < reactionTable.getColumnCount(); i++) {
			reactionTable.set(0, i, -1);
		}

		for (int i = 0; i < reactionTable.getColumnCount(); i++) {
			for (int j = 1; j < reactionTable.getRowCount(); j++) {
				reactionTable.set(j, i, 70 - j * 10);
			}
		}
		r.let("times").be(reactionTable);
		model.add(r);

		// composite the analyser (this should be done from configuration)
		ModelAnalyzer<LevelResult> analyzer = new UppaalModelAnalyzer(new VariablesInterpreter(), new VariablesModel());

		// analyse model
		LevelResult result = analyzer.analyze(model);

		// output result
		System.out.println(result);
	}
}
