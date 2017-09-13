package at.knowcenter.mavenplugins.licensecheck.model;

import org.apache.maven.plugin.MojoFailureException;

/**
 * Created by jschneider on 13.09.17.
 *
 * @author Josef Schneider  {@literal <jschneider@know-center.at>}
 */
public class MaximumRecursionDepthReachedException extends MojoFailureException {


    /**
     * Construct a new <code>MojoFailureException</code> exception providing the source and a short and long message:
     * these messages are used to improve the message written at the end of Maven build.
     *
     * @param source
     * @param shortMessage
     * @param longMessage
     */
    public MaximumRecursionDepthReachedException(Object source, String shortMessage, String longMessage) {
        super(source, shortMessage, longMessage);
    }

    /**
     * Construct a new <code>MojoFailureException</code> exception providing a message.
     *
     * @param message
     */
    public MaximumRecursionDepthReachedException(String message) {
        super(message);
    }

    /**
     * Construct a new <code>MojoFailureException</code> exception wrapping an underlying <code>Throwable</code>
     * and providing a <code>message</code>.
     *
     * @param message
     * @param cause
     * @since 2.0.9
     */
    public MaximumRecursionDepthReachedException(String message, Throwable cause) {
        super(message, cause);
    }
}
