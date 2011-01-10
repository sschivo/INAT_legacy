/**
 * 
 */
package inat.model;

/**
 * A connection between two components in the network.
 * 
 * @author B. Wanders
 */
public class Edge {
	/**
	 * The unique edge identifier.
	 */
	private final String id;

	/**
	 * The origin {@link Vertex} identifier.
	 */
	private final String originId;
	/**
	 * The target {@link Vertex} identifier.
	 */
	private final String targetId;

	/**
	 * The property bag for this edge.
	 */
	private final PropertyBag properties;

	/**
	 * Constructor.
	 * 
	 * @param id the identifier for this edge
	 * 
	 * @param originId the origin {@link Vertex} identifier
	 * @param targetId the target {@link Vertex} identifier
	 * 
	 */
	public Edge(String id, final String originId, final String targetId) {
		this.id = id;
		this.originId = originId;
		this.targetId = targetId;
		this.properties = new PropertyBag();
	}

	/**
	 * Returns the origin of this edge.
	 * 
	 * @param m the model for which we should retrieve the origin
	 * 
	 * @return the origin
	 */
	public Vertex getOrigin(Model m) {
		return m.getVertex(originId);
	}

	/**
	 * Returns the target of this edge.
	 * 
	 * @param m the mode for which we should retrieve the origin
	 * 
	 * @return the target
	 */
	public Vertex getTarget(Model m) {
		return m.getVertex(targetId);
	}

	/**
	 * Returns the properties of this Edge.
	 * 
	 * @return the properties
	 */
	public PropertyBag getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return "Edge@" + this.getId() + "[ " + this.originId + " -> " + this.targetId + " : " + this.properties + "]";
	}

	/**
	 * Returns the identifier of this edge.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the target identifier.
	 * 
	 * @return the targetId
	 */
	public String getTargetId() {
		return targetId;
	}

	/**
	 * Returns the origin identifier.
	 * 
	 * @return the originId
	 */
	public String getOriginId() {
		return originId;
	}
}
