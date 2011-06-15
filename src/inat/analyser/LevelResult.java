/**
 * 
 */
package inat.analyser;

import java.util.List;
import java.util.Set;

/**
 * The concentrations result contains information about the analysis of the
 * activation levels of each substrate in a model.
 * 
 * @author B. Wanders
 */
public interface LevelResult {
	/**
	 * This method retrieves the level of activation for the given substrate
	 * 
	 * @param id the id of the substrate
	 * @param time the time index to do a look up for
	 * @return the level of concentration
	 */
	public int getConcentration(String id, int time);

	/**
	 * Determines the reactant ID's of substrates of which result are known.
	 * 
	 * @return a set of IDs
	 */
	public Set<String> getReactantIds();

	/**
	 * Returns a list of all time indices at which we have a real data point.
	 * 
	 * @return the list of data point time indices
	 */
	public List<Integer> getTimeIndices();
	
	public boolean isEmpty();
}
