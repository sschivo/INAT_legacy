/**
 * 
 */
package inat.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A model. This model keeps itself consistent, as long as both {@link Vertex}
 * and {@link Edge} implementations keep their {@link #equals(Object)} method
 * based on identity the model is automatically consistent.
 * 
 * @author B. Wanders
 */
public class Model {
	/**
	 * The vertices in the model.
	 */
	private final Map<String, Vertex> vertices;
	/**
	 * The edges in the model.
	 */
	private final Map<String, Edge> edges;

	/**
	 * The incoming edges on a vertex.
	 */
	private final Map<String, Set<Edge>> incoming;
	/**
	 * The outgoing edges on a vertex.
	 */
	private final Map<String, Set<Edge>> outgoing;
	/**
	 * The global properties on the model.
	 */
	private final PropertyBag properties;

	/**
	 * Constructor.
	 */
	public Model() {
		this.vertices = new HashMap<String, Vertex>();
		this.edges = new HashMap<String, Edge>();

		this.incoming = new HashMap<String, Set<Edge>>();
		this.outgoing = new HashMap<String, Set<Edge>>();

		this.properties = new PropertyBag();
	}

	/**
	 * Puts (adds or replaces) a vertex into the model.
	 * 
	 * @param v the vertex to add
	 */
	public void putVertex(Vertex v) {
		if (this.vertices.put(v.getId(), v) == null) {
			this.incoming.put(v.getId(), new HashSet<Edge>());
			this.outgoing.put(v.getId(), new HashSet<Edge>());
		}
	}

	/**
	 * Puts (adds or replaces) an edge into the model.
	 * 
	 * @param e the edge to remove
	 */
	public void putEdge(Edge e) {
		assert this.vertices.containsKey(e.getTargetId()) : "The target identifier is not a valid vertex.";
		assert this.vertices.containsKey(e.getOriginId()) : "The origin identifier is not a valid vertex.";

		this.edges.put(e.getId(), e);
		this.incoming.get(e.getTargetId()).add(e);
		this.outgoing.get(e.getOriginId()).add(e);
	}

	/**
	 * Removes an edge.
	 * 
	 * @param e the edge to remove
	 */
	public void removeEdge(Edge e) {
		this.edges.remove(e.getId());
		this.incoming.get(e.getTargetId()).remove(e);
		this.outgoing.get(e.getOriginId()).remove(e);
	}

	/**
	 * Removes a vertex, this method also cleans all edges connecting to this
	 * vertex.
	 * 
	 * @param v the vertex to remove
	 */
	public void removeVertex(Vertex v) {
		for (Edge e : this.incoming.remove(v.getId())) {
			this.edges.remove(e.getId());
		}

		for (Edge e : this.outgoing.remove(v.getId())) {
			this.edges.remove(e.getId());
		}

		this.vertices.remove(v.getId());
	}

	/**
	 * Returns all incoming edges for the given vertex.
	 * 
	 * @param v the vertex for which all incoming edges are needed
	 * @return a set of {@link Edge}s
	 */
	public Set<Edge> getIncomingEdges(Vertex v) {
		return Collections.unmodifiableSet(this.incoming.get(v.getId()));
	}

	/**
	 * Returns all outgoing edges for the given vertex.
	 * 
	 * @param v the vertext for which all outgoing edges are needed
	 * @return a set of {@link Edge}s
	 */
	public Set<Edge> getOutgoingEdges(Vertex v) {
		return Collections.unmodifiableSet(this.outgoing.get(v.getId()));
	}

	/**
	 * Returns the edge with the given identifier, or {@code null}.
	 * 
	 * @param id the identifier we are looking for
	 * @return the found {@link Edge}, or {@code null}
	 */
	public Edge getEdge(String id) {
		return this.edges.get(id);
	}

	/**
	 * Returns the vertex with the given identifier, or {@code null}.
	 * 
	 * @param id the identifier we are looking for
	 * @return the found {@link Vertex}, or {@code null}
	 */
	public Vertex getVertex(String id) {
		return this.vertices.get(id);
	}

	/**
	 * Returns the properties for this model.
	 * 
	 * @return the properties of this model
	 */
	public PropertyBag getProperties() {
		return properties;
	}

	/**
	 * Returns an unmodifiable view of all vertices in this model.
	 * 
	 * @return all vertices
	 */
	public Collection<Vertex> getVertices() {
		return Collections.unmodifiableCollection(this.vertices.values());
	}

	/**
	 * returns an unmodifiable view of all edges in this model.
	 * 
	 * @return all edges
	 */
	public Collection<Edge> getEdges() {
		return Collections.unmodifiableCollection(this.edges.values());
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append("Model[\n");
		for (Vertex v : this.vertices.values()) {
			result.append("  " + v + "\n");
		}
		for (Edge e : this.edges.values()) {
			result.append("  " + e + "\n");
		}
		result.append("]");

		return result.toString();
	}
}
