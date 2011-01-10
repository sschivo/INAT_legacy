/**
 * 
 */
package inat.model;

/**
 * A vertex is a component in the network.
 * 
 * @author B. Wanders
 */
public class Vertex {
	/**
	 * The unique vertex identifier.
	 */
	private final String id;
	/**
	 * The properties of this vertex.
	 */
	private final PropertyBag properties;

	/**
	 * Constructor.
	 * 
	 * @param id the vertex id
	 */
	public Vertex(String id) {
		this.id = id;
		this.properties = new PropertyBag();
	}

	/**
	 * @return the properties
	 */
	public PropertyBag getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return "Vertex@" + this.getId() + "[" + this.properties + "]";
	}

	/**
	 * Returns the identifier of this vertex.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}
}
