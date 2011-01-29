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
		a.let("initialConcentration").be(4);
		model.add(a);

		Reaction deg = new Reaction("aDeg");
		deg.let("type").be("reaction1");
		deg.let("reactant").be("a");
		deg.let("increment").be(-1);
		Table degredationTable = new Table(4 + 1, 1);
		degredationTable.set(0, 0, -1);
		degredationTable.set(1, 0, 60);
		degredationTable.set(2, 0, 30);
		degredationTable.set(3, 0, 20);
		degredationTable.set(4, 0, 10);
		deg.let("times").be(degredationTable);
		model.add(deg);

		Reactant b = new Reactant("b");
		b.let("name").be("B");
		b.let("initialConcentration").be(2);
		model.add(b);

		Reaction degb = new Reaction("bDeg");
		degb.let("type").be("reaction1");
		degb.let("reactant").be("b");
		degb.let("increment").be(-1);
		degb.let("times").be(degredationTable);
		model.add(degb);

		// composite the analyser (this should be done from configuration)
		ModelAnalyzer<LevelResult> analyzer = new UppaalModelAnalyzer(new VariablesInterpreter(), new VariablesModel());

		// analyse model
		LevelResult result = analyzer.analyze(model);

		// output result
		System.out.println(result);
	}
}
