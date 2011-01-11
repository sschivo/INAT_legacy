/**
 * 
 */
package inat.serializer;

import inat.model.Model;
import inat.util.XmlEnvironment;

import org.w3c.dom.Document;

/**
 * The XML serializer is used to convert a model from and to XML.
 * 
 * @author B. Wanders
 */
public class XMLSerializer {
	/**
	 * Converts a model to it's XML representation.
	 * 
	 * @param m the model
	 * @return the document
	 */
	public Document serialize(Model m) {
		Document doc = XmlEnvironment.getDocumentBuilder().newDocument();
		// FIXME: add serialization code
		return doc;
	}

	/**
	 * Converts an XML representation to an {@link Model}.
	 * 
	 * @param d the document to deserialize
	 * @return the model
	 */
	public Model deserialize(Document d) {
		Model m = new Model();
		// FIXME: add deserialization code
		return m;
	}
}
