/**
 * 
 */
package inat.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A property bag is a set of unordered properties.
 * 
 * @author B. Wanders
 */
public class PropertyBag implements Iterable<Property>, Serializable {
	private static final long serialVersionUID = -3325378476006458874L;
	/**
	 * A map containing the actual properties.
	 */
	private final Map<String, Property> properties;

	/**
	 * Constructor.
	 */
	public PropertyBag() {
		this.properties = new HashMap<String, Property>();
	}

	/**
	 * Gets a named property from the bag. If the property did not exist it is
	 * created.
	 * 
	 * @param name the name of the proeprty to retrieve or create
	 * 
	 * @return a named property
	 */
	public Property let(String name) {
		if (!this.has(name)) {
			this.properties.put(name, new Property(name));
		}

		return this.get(name);
	}

	/**
	 * Retrieves a property by name.
	 * 
	 * @param name the name of the property
	 * @return the property itself, or {@code null} if no such property is
	 *         available
	 */
	public Property get(String name) {
		return this.properties.get(name);
	}

	/**
	 * Determines whether a property with the given name is available in this
	 * bag.
	 * 
	 * @param name the name of the property
	 * @return {@code true} if this property bag contains the requested
	 *         property, {@code false} otherwise
	 */
	public boolean has(String name) {
		return this.properties.containsKey(name);
	}

	/**
	 * Removes a property from this property bag.
	 * 
	 * @param name the name of the property to remove
	 */
	public void remove(String name) {
		this.properties.remove(name);
	}

	/**
	 * Removes the property with the same name as the given property.
	 * 
	 * @param p the property who's name should be used
	 */
	public void remove(Property p) {
		this.remove(p.getName());
	}

	@Override
	public Iterator<Property> iterator() {
		return this.properties.values().iterator();
	}

	@Override
	public String toString() {
		return "PropertyBag" + this.properties.values();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.properties == null) ? 0 : this.properties.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PropertyBag other = (PropertyBag) obj;
		if (this.properties == null) {
			if (other.properties != null)
				return false;
		} else if (!this.properties.equals(other.properties))
			return false;
		return true;
	}
}
