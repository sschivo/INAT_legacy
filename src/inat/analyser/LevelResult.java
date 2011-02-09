/**
 * 
 */
package inat.analyser;

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
	
	public void toCSV(String fileName);
}
