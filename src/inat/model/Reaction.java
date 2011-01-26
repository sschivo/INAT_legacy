/**
 * 
 */
package inat.model;

/**
 * A connection between two components in the network.
 * 
 * @author B. Wanders
 */
public class Reaction {
	/**
	 * The unique edge identifier.
	 */
	private final String id;

	/**
	 * The property bag for this edge.
	 */
	private final PropertyBag properties;

	/**
	 * The owning model.
	 */
	private Model model;

	/**
	 * Constructor.
	 * 
	 * @param id the identifier for this edge
	 */
	public Reaction(String id) {
		this.id = id;
		this.properties = new PropertyBag();
	}

	/**
	 * Sets the owning model of this species.
	 * 
	 * @param m the owning model
	 */
	void setModel(Model m) {
		this.model = m;
	}

	/**
	 * Returns the model in which this reaction is defined.
	 * 
	 * @return the model that owns this reaction
	 */
	public Model getModel() {
		return this.model;
	}

	/**
	 * Returns the properties of this reaction.
	 * 
	 * @return the properties
	 */
	public PropertyBag getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "Reaction@" + this.getId() + "[" + this.properties + "]";
	}

	/**
	 * Returns the identifier of this edge.
	 * 
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}

}
