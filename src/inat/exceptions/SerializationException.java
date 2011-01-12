/**
 * 
 */
package inat.exceptions;

import inat.model.Model;

/**
 * This exception is thrown if the serialization or deserialization of a
 * {@link Model} failed.
 * 
 * @author B. Wanders
 */
public class SerializationException extends InatException {

	/**
	 * Constructor with detail message and cause.
	 * 
	 * @param message the detail message
	 * @param cause the cause
	 */
	public SerializationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor with detail message.
	 * 
	 * @param message the detail message
	 */
	public SerializationException(String message) {
		super(message);
	}
}
