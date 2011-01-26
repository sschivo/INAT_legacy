/**
 * 
 */
package inat;

import inat.exceptions.InatException;
import inat.util.XmlConfiguration;
import inat.util.XmlEnvironment;

import java.io.File;
import java.io.IOException;

import org.xml.sax.SAXException;

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
