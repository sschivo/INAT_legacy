package inat.serializer;

import inat.analyser.LevelResult;
import inat.model.Model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/**
 * This class is capable of writing out a {@link LevelResult} to a CSV file
 * 
 * @author Brend Wanders
 * @author Stefano Schivo
 */
public class CsvWriter {
	/**
	 * Outputs the given {@link LevelResult} to a CSV formatted file.
	 * 
	 * @param filename the filename of the file to output to
	 * @param m the model to use as a base
	 * @param r the results to save
	 * @throws IOException if the save failed for some reason
	 */
	public void writeCsv(String filename, Model m, LevelResult r) throws IOException {
		this.writeCsv(new File(filename), m, r);
	}

	/**
	 * Outputs the given {@link LevelResult} to a CSV formatted file.
	 * 
	 * @param file the file to output to
	 * @param m the model to use as a base
	 * @param r the results to save
	 * @throws IOException if the save failed for some reason
	 */
	public void writeCsv(File file, Model m, LevelResult r) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));

		/*
		Object[] zioLupoMannaro = levels.keySet().toArray();
		String[] reactantNames = new String[zioLupoMannaro.length];
		for (int i = 0; i < zioLupoMannaro.length; i++) {
			reactantNames[i] = zioLupoMannaro[i].toString();
		}
		bw.write("Time, ");
		for (int i = 0; i < reactantNames.length - 1; i++) {
			bw.write(reactantNames[i] + ", ");
		}
		bw.write(reactantNames[reactantNames.length - 1]);
		bw.newLine();
		*/

		// walk over all reactants
		Set<String> rids = r.getReactantIds();
		bw.write("Time");
		for (String rid : rids) {
			// determine official name and output it
			String name = m.getReactant(rid).get("alias").as(String.class); //if an alias is set, we prefer it
			if (name == null) {
				name = m.getReactant(rid).get("name").as(String.class);
			}
			bw.write(", " + name);
		}
		bw.newLine();

		/*
		int curTime = levels.get(reactantNames[0]).firstKey();
		while (true) {
			bw.write(curTime + ", ");
			for (int i = 0; i < reactantNames.length - 1; i++) {
				bw.write(levels.get(reactantNames[i]).get(curTime) + ", ");
			}
			bw.write("" + levels.get(reactantNames[reactantNames.length - 1]).get(curTime));
			bw.newLine();
			int nextTime = curTime + 1;
			levels.get(reactantNames[0]).tailMap(nextTime);
			if (levels.get(reactantNames[0]).tailMap(nextTime).isEmpty()) {
				break;
			} else {
				curTime = levels.get(reactantNames[0]).tailMap(nextTime).firstKey();
			}
		}
		bw.close();
		*/
		
		for (int t : r.getTimeIndices()) {
			bw.write("" + t);
			for (String rid : rids) {
				// determine official name and output it
				bw.write(", " + r.getConcentration(rid, t));
			}
			bw.newLine();
		}
		
		bw.close();
	}
}
