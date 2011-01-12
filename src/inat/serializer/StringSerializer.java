/**
 * 
 */
package inat.serializer;

import inat.exceptions.SerializationException;
import inat.util.AXPathExpression;
import inat.util.XmlEnvironment;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The {@link String} serializer.
 * 
 * @author B. Wanders
 */
public class StringSerializer implements TypeSerializer<String> {
	/**
	 * Value pattern.
	 */
	private final AXPathExpression expression = XmlEnvironment.hardcodedXPath("./value");

	@Override
	public String deserialize(Node root) throws SerializationException {
		try {
			return this.expression.getString(root);
		} catch (XPathExpressionException e) {
			throw new SerializationException("Could not deserialize, expression " + expression.toString()
					+ " did not match.", e);
		}
	}

	@Override
	public Element serialize(Document doc, Object value) {
		Element result = doc.createElement("value");
		result.setTextContent(value.toString());
		return result;
	}

}