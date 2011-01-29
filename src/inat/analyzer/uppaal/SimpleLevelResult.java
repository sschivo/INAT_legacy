package inat.analyzer.uppaal;

import inat.analyzer.LevelResult;

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
}
