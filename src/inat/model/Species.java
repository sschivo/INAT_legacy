/**
 * 
 */
package inat.model;

/**
 * A vertex is a component in the network.
 * 
 * @author B. Wanders
 */
public class Species {
	/**
	 * The unique vertex identifier.
	 */
	private final String id;

	/**
	 * The owning model.
	 */
	private Model model;

	/**
	 * The properties of this vertex.
	 */
	private final PropertyBag properties;

	/**
	 * Constructor.
	 * 
	 * @param id the vertex id
	 */
	public Species(String id) {
		this.id = id;
		this.properties = new PropertyBag();
		this.model = null;
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
	 * Return the properties of this species.
	 * 
	 * @return the properties
	 */
	public PropertyBag getProperties() {
		return this.properties;
	}

	@Override
	public String toString() {
		return "Species@" + this.getId() + "[" + this.properties + "]";
	}

	/**
	 * Returns the identifier of this vertex.
	 * 
	 * @return the id
	 */
	public String getId() {
		return this.id;
	}
}
