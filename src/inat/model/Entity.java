package inat.model;

/**
 * A base entity in the data model. This class exists to provide a base of
 * functionality for all entities stored within an INAT data model.
 * 
 * @author Brend Wanders
 */
public abstract class Entity {

	/**
	 * The unique edge identifier.
	 */
	protected final String id;
	/**
	 * The property bag for this edge.
	 */
	protected final PropertyBag properties;
	/**
	 * The owning model.
	 */
	private Model model;

	/**
	 * Constructor with identifier.
	 * 
	 * @param id the identifier of this entity
	 */
	public Entity(String id) {
		this.id = id;
		this.properties = new PropertyBag();
	}

	/**
	 * Sets the owning model of this species.
	 * 
	 * @param m the owning model
	 */
	protected void setModel(Model m) {
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

	/**
	 * Retrieves a property of this entity.
	 * 
	 * @param name the name of the proeprty to retrieve
	 * @return the property, or <code>null</code> if the property was not
	 *         available
	 */
	public Property get(String name) {
		return this.getProperties().get(name);
	}

	/**
	 * Checks for the availability of a specific property.
	 * 
	 * @param name the name of the property to check for
	 * @return {@code true} if the property is available, {@code false}
	 *         otherwise
	 */
	public boolean has(String name) {
		return this.getProperties().has(name);
	}

	/**
	 * Retrieves a property, creating it if necessary.
	 * 
	 * @param name the name of the property
	 * @return a {@link Property}
	 */
	public Property let(String name) {
		return this.properties.let(name);
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