/**
 * 
 */
package inat.util;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An XML configuration file.
 * 
 * @author B. Wanders
 */
public class XmlConfiguration {
	/**
	 * The document that backs this configuration.
	 */
	private final Document document;

	/**
	 * Constructor.
	 * 
	 * @param doc the configuration document
	 */
	public XmlConfiguration(Document doc) {
		this.document = doc;
	}

	/**
	 * Evaluates the given XPath expression in the context of this document.
	 * 
	 * @param expression the expression to evaluate
	 * @param resultType the result type
	 * @return an object or {@code null}
	 */
	private Object evaluate(String expression, QName resultType) {
		try {
			AXPathExpression xpath = XmlEnvironment.hardcodedXPath(expression);
			return xpath.evaluate(this.document, resultType);
		} catch (XPathExpressionException e) {
			return null;
		}
	}

	/**
	 * Returns a node from this document.
	 * 
	 * @param xpath the selection expression
	 * @return a node
	 */
	public Node getNode(String xpath) {
		return (Node) this.evaluate(xpath, XPathConstants.NODE);
	}

	/**
	 * Returns a set of nodes from this document.
	 * 
	 * @param xpath the selection expression
	 * @return a set of nodes
	 */
	public ANodeList getNodes(String xpath) {
		return new ANodeList((NodeList) this.evaluate(xpath, XPathConstants.NODESET));
	}

	/**
	 * Returns a string from this document.
	 * 
	 * @param xpath the selection expression
	 * @return the string, or {@code null}
	 */
	public String get(String xpath) {
		return (String) this.evaluate(xpath, XPathConstants.STRING);
	}

	/**
	 * Returns a string from this document, or the default value if the string
	 * is not present.
	 * 
	 * @param xpath the selection expression
	 * @param defaultValue the default value
	 * @return the string from the document or the default value
	 */
	public String get(String xpath, String defaultValue) {
		if (this.has(xpath)) {
			return this.get(xpath);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Checks to see whether this document matches the given expression.
	 * 
	 * @param xpath the expression to test
	 * @return {@code true} if the document matches, {@code false} otherwise
	 */
	public boolean has(String xpath) {
		return (Boolean) this.evaluate(xpath, XPathConstants.BOOLEAN);
	}
}
