package inat.analyzer.uppaal;

import inat.analyzer.LevelResult;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

/**
 * A very simple data container for the concentration/time data.
 * 
 * @author Brend Wanders
 * 
 */
public class SimpleLevelResult implements LevelResult {
	Map<String, SortedMap<Integer, Integer>> levels;

	/**
	 * @param levels the levels to enter
	 */
	public SimpleLevelResult(Map<String, SortedMap<Integer, Integer>> levels) {
		this.levels = levels;
	}

	@Override
	public int getConcentration(String id, int time) {
		assert this.levels.containsKey(id) : "Can not retrieve level for unknown identifier.";

		SortedMap<Integer, Integer> data = this.levels.get(id);

		// determine level at requested moment in time:
		// it is either the level set at the requested moment, or the one set
		// before that
		assert !data.headMap(time + 1).isEmpty() : "Can not retrieve data from any moment before the start of time.";
		int exactTime = data.headMap(time + 1).firstKey();

		// use exact time to get value
		return data.get(exactTime);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();

		for (Entry<String, SortedMap<Integer, Integer>> r : this.levels.entrySet()) {
			b.append(r.getKey() + ": " + r.getValue() + "\n");
		}

		return b.toString();
	}
	
	public void toCSV(String csvFile) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile));
			Object[] zioLupoMannaro = levels.keySet().toArray();
			String[] reactantNames = new String[zioLupoMannaro.length];
			for (int i=0;i<zioLupoMannaro.length;i++) {
				reactantNames[i] = zioLupoMannaro[i].toString();
			}
			bw.write("Time, ");
			for (int i=0;i<reactantNames.length-1;i++) {
				bw.write(reactantNames[i] + ", ");
			}
			bw.write(reactantNames[reactantNames.length - 1]);
			bw.newLine();
			int curTime = levels.get(reactantNames[0]).firstKey();
			while (true) {
				bw.write(curTime + ", ");
				for (int i=0;i<reactantNames.length-1;i++) {
					bw.write(levels.get(reactantNames[i]).get(curTime) + ", ");
				}
				bw.write("" + levels.get(reactantNames[reactantNames.length-1]).get(curTime));
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
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}
}
