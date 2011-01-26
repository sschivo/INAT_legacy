/**
 * 
 */
package inat.model;

/**
 * A vertex is a component in the network.
 * 
 * @author B. Wanders
 */
public class Reactant extends Entity {
	/**
	 * Constructor.
	 * 
	 * @param id the vertex id
	 */
	public Reactant(String id) {
		super(id);
	}

	@Override
	public String toString() {
		return "Species@" + this.getId() + "[" + this.properties + "]";
	}
}
