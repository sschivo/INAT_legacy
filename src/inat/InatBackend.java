/**
 * 
 */
package inat;

import giny.model.Edge;
import inat.exceptions.InatException;
import inat.util.XmlConfiguration;
import inat.util.XmlEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.xml.sax.SAXException;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.attr.MultiHashMapListener;

/**
 * The INAT backend singleton is used to initialise the INAT backend, and to
 * retrieve configuration.
 * 
 * @author B. Wanders
 */
public class InatBackend {
	/**
	 * The singleton instance.
	 */
	private static InatBackend instance;

	/**
	 * The configuration properties.
	 */
	private XmlConfiguration configuration;

	private static final String NUMBER_OF_LEVELS = "levels",
								INITIAL_LEVEL = "initialConcentration",
								SHOWN_LEVEL = "activityRatio",
								SECONDS_PER_POINT = "seconds per point",
								SCENARIO = "scenario";
		
	/**
	 * Constructor.
	 * 
	 * @param configuration the configuration file location
	 * @throws InatException if the INAT backend could not be initialised
	 */
	private InatBackend(File configuration) throws InatException {

		// read configuration file
		try {
			// initialise the XML environment
			XmlEnvironment.getInstance();

			// read config from file
			this.configuration = new XmlConfiguration(XmlEnvironment.parse(configuration));
			
			//register variable listener
			Cytoscape.getNodeAttributes().getMultiHashMap().addDataListener(new MultiHashMapListener() {

				@Override
				public void allAttributeValuesRemoved(String arg0, String arg1) {
					
				}

				@SuppressWarnings("unchecked")
				@Override
				public void attributeValueAssigned(String objectKey, String attributeName,
						Object[] keyIntoValue, Object oldAttributeValue, Object newAttributeValue) {
					
					
					if (attributeName.equals(NUMBER_OF_LEVELS)) { //we are here to listen for #levels changes in order to update the parameters of reactions in which the affected reactant is involved
						
						if (oldAttributeValue == null) return; //If there was no old value, we can do very little
						
						double newLevel = 0, oldLevel = 0, factor = 0;
						newLevel = Double.parseDouble(newAttributeValue.toString());
						oldLevel = Double.parseDouble(oldAttributeValue.toString());
						factor = newLevel / oldLevel;
						
						CyNetwork network = Cytoscape.getCurrentNetwork();
						CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
						final Iterator<Edge> edges = (Iterator<Edge>) network.edgesIterator();
						for (int i = 0; edges.hasNext(); i++) {
							Edge edge = edges.next();
							
							if (edge.getSource().getIdentifier().equals(objectKey) || edge.getTarget().getIdentifier().equals(objectKey)) {
								//update the parameters for the reaction
								if (edge.getSource().equals(edge.getTarget())) {
									//Double parameter = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "parameter");
									//parameter /= factor;
									//edgeAttributes.setAttribute(edge.getIdentifier(), "parameter", parameter);
								} else {
									Integer scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), SCENARIO);
									if (scenarioIdx == 0) { //Scenario 1-2-3-4
										Double parameter = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "parameter");
										if (edge.getSource().getIdentifier().equals(objectKey)) { //We do something only if the changed reactant was the upstream one
											parameter /= factor;
										} else {
											parameter *= factor;
										}
										edgeAttributes.setAttribute(edge.getIdentifier(), "parameter", parameter);
									} else if (scenarioIdx == 1) { //Scenario 5
										Double stot = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "Stot"),
											   k2km = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "k2/km");
										if (edge.getSource().getIdentifier().equals(objectKey)) { //If the changed reactant is the upstream one, we change only k2/km
											k2km /= factor;
										} else { //If the changed reactant is the downstream one, we change only Stot
											stot *= factor;
										}
										edgeAttributes.setAttribute(edge.getIdentifier(), "Stot", stot);
										edgeAttributes.setAttribute(edge.getIdentifier(), "k2/km", k2km);
									} else if (scenarioIdx == 2) { //Scenario 6
										Double stot = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "Stot"),
												 km = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "km"),
												 k2 = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "k2");
										if (edge.getSource().getIdentifier().equals(objectKey)) { //If the changed reactant is the upstream one, we change only k2
											k2 /= factor;
										} else { //If the changed reactant is the downstream one, we change all three
											stot *= factor;
											km *= factor;
											k2 *= factor;
										}
										edgeAttributes.setAttribute(edge.getIdentifier(), "Stot", stot);
										edgeAttributes.setAttribute(edge.getIdentifier(), "km", km);
										edgeAttributes.setAttribute(edge.getIdentifier(), "k2", k2);
									}
								}
							}
						}
						//Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null); //!!! If you don't advertise the change of property, Cytoscape will never notice it ?!?
						
						//Update the value of "levels" in the network with the maximum of all levels
						int maxLevels = 0;
						java.util.Iterator<giny.model.Node> iter = Cytoscape.getCurrentNetwork().nodesIterator();
						while (iter.hasNext()) {
							giny.model.Node node = iter.next();
							int levels;
							if (Cytoscape.getNodeAttributes().hasAttribute(node.getIdentifier(), NUMBER_OF_LEVELS)) {
								Object val = Cytoscape.getNodeAttributes().getAttribute(node.getIdentifier(), NUMBER_OF_LEVELS);
								levels = Integer.parseInt(val.toString());
							} else {
								levels = 0;
							}
							if (levels > maxLevels) {
								maxLevels = levels;
							}
						}
						Cytoscape.getNetworkAttributes().setAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), NUMBER_OF_LEVELS, maxLevels);
					} else if (attributeName.equals(INITIAL_LEVEL)) {
						CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
						int currentLevel = 0, totalLevels = 0;
						try {
							currentLevel = Integer.parseInt(newAttributeValue.toString());
							totalLevels = nodeAttr.getIntegerAttribute(objectKey, NUMBER_OF_LEVELS);
						} catch (Exception ex) {
							currentLevel = 0;
							totalLevels = 1;
						}
						double activityRatio = (double)currentLevel / totalLevels;
						nodeAttr.setAttribute(objectKey, SHOWN_LEVEL, activityRatio);
						Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
					}
				}

				@Override
				public void attributeValueRemoved(String arg0, String arg1,
						Object[] arg2, Object arg3) {
					
				}
				
			});
			
			
			
			Cytoscape.getNetworkAttributes().getMultiHashMap().addDataListener(new MultiHashMapListener() {

				@Override
				public void allAttributeValuesRemoved(String arg0, String arg1) {
					
				}

				@SuppressWarnings("unchecked")
				@Override
				public void attributeValueAssigned(String objectKey, String attributeName,
						Object[] keyIntoValue, Object oldAttributeValue, Object newAttributeValue) {
					
					if (oldAttributeValue == null) return; //If there was no old value, we can do very little
					
					if (attributeName.equals(SECONDS_PER_POINT)) {
						double newSecondsPerPoint = 0, oldSecondsPerPoint = 0, factor = 0;
						newSecondsPerPoint = Double.parseDouble(newAttributeValue.toString());
						oldSecondsPerPoint = Double.parseDouble(oldAttributeValue.toString());
						factor = oldSecondsPerPoint / newSecondsPerPoint;
						
						CyNetwork network = Cytoscape.getCurrentNetwork();
						CyAttributes edgeAttributes = Cytoscape.getEdgeAttributes();
						final Iterator<Edge> edges = (Iterator<Edge>) network.edgesIterator();
						for (int i = 0; edges.hasNext(); i++) {
							Edge edge = edges.next();
							
							if (edge.getSource().equals(edge.getTarget())) {
								Double parameter = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "parameter");
								parameter /= factor;
								edgeAttributes.setAttribute(edge.getIdentifier(), "parameter", parameter);
							} else {
								Integer scenarioIdx = edgeAttributes.getIntegerAttribute(edge.getIdentifier(), SCENARIO);
								if (scenarioIdx == 0) { //Scenario 1-2-3-4
									Double parameter = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "parameter");
									parameter /= factor;
									edgeAttributes.setAttribute(edge.getIdentifier(), "parameter", parameter);
								} else if (scenarioIdx == 1) { //Scenario 5
									Double k2km = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "k2/km");
									k2km /= factor;
									edgeAttributes.setAttribute(edge.getIdentifier(), "k2/km", k2km);
								} else if (scenarioIdx == 2) { //Scenario 6
									Double k2 = edgeAttributes.getDoubleAttribute(edge.getIdentifier(), "k2");
									k2 /= factor;
									edgeAttributes.setAttribute(edge.getIdentifier(), "k2", k2);
								}
							}
						}
					}
				}

				@Override
				public void attributeValueRemoved(String arg0, String arg1,
						Object[] arg2, Object arg3) {
					
				}
			});
			
		} catch (SAXException e) {
			throw new InatException("Could not parse configuration file '" + configuration + "'", e);
		} catch (IOException e) {
			throw new InatException("Could not read configuration file '" + configuration + "'", e);
		}
	}

	/**
	 * Initialises the INAT backend with the given configuration.
	 * 
	 * @param configuration the location of the configuration file
	 * @throws InatException if the backend could not be initialised
	 */
	public static void initialise(File configuration) throws InatException {
		assert !isInitialised() : "Can not re-initialise INAT backend.";

		InatBackend.instance = new InatBackend(configuration);
	}

	/**
	 * Returns the singleton instance.
	 * 
	 * @return the single instance
	 */
	public static InatBackend get() {
		assert isInitialised() : "INAT Backend not yet initialised.";
		return instance;
	}

	/**
	 * Returns the configuration properties.
	 * 
	 * @return the configuration
	 */
	public XmlConfiguration configuration() {
		return this.configuration;
	}

	/**
	 * Returns whether the INAT backend is initialised.
	 * 
	 * @return whether the backend is initialised
	 */
	public static boolean isInitialised() {
		return instance != null;
	}
}
