/**
 * 
 */
package inat.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A model. This model keeps itself consistent, as long as both {@link Reactant}
 * and {@link Reaction} implementations keep their {@link #equals(Object)}
 * method based on identity the model is automatically consistent.
 * 
 * @author B. Wanders
 */
public class Model {
	/**
	 * The vertices in the model.
	 */
	private final Map<String, Reactant> reactants;
	/**
	 * The edges in the model.
	 */
	private final Map<String, Reaction> reactions;

	/**
	 * The global properties on the model.
	 */
	private final PropertyBag properties;

	/**
	 * Constructor.
	 */
	public Model() {
		this.reactants = new HashMap<String, Reactant>();
		this.reactions = new HashMap<String, Reaction>();
		this.properties = new PropertyBag();
	}

	/**
	 * Puts (adds or replaces) a vertex into the model.
	 * 
	 * @param v the vertex to add
	 */
	public void add(Reactant v) {
		assert v.getModel() == null : "Can't add a reactant that is already part of a model.";

		this.reactants.put(v.getId(), v);
		v.setModel(this);
	}

	/**
	 * Puts (adds or replaces) an edge into the model.
	 * 
	 * @param e the edge to remove
	 */
	public void add(Reaction e) {
		assert e.getModel() == null : "Can't add a reaction that is already part of a model.";

		this.reactions.put(e.getId(), e);
		e.setModel(this);
	}

	/**
	 * Removes an edge.
	 * 
	 * @param e the edge to remove
	 */
	public void remove(Reaction e) {
		assert e.getModel() == this : "Can't remove a reaction that is not part of this model.";
		this.reactions.remove(e.getId());
		e.setModel(null);
	}

	/**
	 * Removes a vertex, this method also cleans all edges connecting to this
	 * vertex.
	 * 
	 * @param v the vertex to remove
	 */
	public void remove(Reactant v) {
		assert v.getModel() == this : "Can't remove a reactant that is not part of this model.";
		this.reactants.remove(v.getId());
		v.setModel(null);
	}

	/**
	 * Returns the edge with the given identifier, or {@code null}.
	 * 
	 * @param id the identifier we are looking for
	 * @return the found {@link Reaction}, or {@code null}
	 */
	public Reaction getReaction(String id) {
		return this.reactions.get(id);
	}

	/**
	 * Returns the vertex with the given identifier, or {@code null}.
	 * 
	 * @param id the identifier we are looking for
	 * @return the found {@link Reactant}, or {@code null}
	 */
	public Reactant getReactant(String id) {
		return this.reactants.get(id);
	}

	/**
	 * Returns the properties for this model.
	 * 
	 * @return the properties of this model
	 */
	public PropertyBag getProperties() {
		return this.properties;
	}

	/**
	 * Returns an unmodifiable view of all vertices in this model.
	 * 
	 * @return all vertices
	 */
	public Collection<Reactant> getReactants() {
		return Collections.unmodifiableCollection(this.reactants.values());
	}

	/**
	 * returns an unmodifiable view of all edges in this model.
	 * 
	 * @return all edges
	 */
	public Collection<Reaction> getReactions() {
		return Collections.unmodifiableCollection(this.reactions.values());
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();

		result.append("Model[\n");
		for (Reactant v : this.reactants.values()) {
			result.append("  " + v + "\n");
		}
		for (Reaction e : this.reactions.values()) {
			result.append("  " + e + "\n");
		}
		result.append("]");

		return result.toString();
	}
}
