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
 * The {@link Integer} serializer.
 * 
 * @author B. Wanders
 */
public class IntegerSerializer implements TypeSerializer<Integer> {
	/**
	 * Value pattern.
	 */
	private final AXPathExpression expression = XmlEnvironment.hardcodedXPath("value");

	@Override
	public Integer deserialize(Node root) throws SerializationException {
		String value = null;
		try {
			value = this.expression.getString(root);
			return new Integer(value);
		} catch (XPathExpressionException e) {
			throw new SerializationException("Could not deserialize, expression " + expression.toString()
					+ " did not match.", e);
		} catch (NumberFormatException e) {
			throw new SerializationException("Could not interpret value '" + value + "' as an integer.", e);
		}
	}

	@Override
	public Element serialize(Document doc, Object value) {
		Element result = doc.createElement("value");
		result.setTextContent(value.toString());
		return result;
	}

}
