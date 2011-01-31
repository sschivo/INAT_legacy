/**
 * 
 */
package inat.model;

/**
 * A connection between two components in the network.
 * 
 * @author B. Wanders
 */
public class Reaction extends Entity {
	/**
	 * Constructor.
	 * 
	 * @param id the identifier for this edge
	 */
	public Reaction(String id) {
		super(id);
	}

	@Override
	public String toString() {
		return "Reaction '" + this.getId() + "'";
	}
}
