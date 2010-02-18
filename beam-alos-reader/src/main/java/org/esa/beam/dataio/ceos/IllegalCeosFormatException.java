package org.esa.beam.dataio.ceos;
/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 07.12.2005
 * Time: 15:46:33
 */

/**
 * Description of IllegalCeosFormatException
 * <p/>
 * <p>This class is public for the benefit of the implementation of another (internal) class and its API may
 * change in future releases of the software.</p>
 *
 * @author Marco Peters
 */
public class IllegalCeosFormatException extends Exception {

    private long _streamPos;

    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public IllegalCeosFormatException(final String message, final long streamPos) {
        super(message);
        _streamPos = streamPos;
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.  <p>Note that the detail message associated with
     * <code>cause</code> is <i>not</i> automatically incorporated in
     * this exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A <tt>null</tt> value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     *
     * @since 1.4
     */
    public IllegalCeosFormatException(final String message, final long streamPos, final Throwable cause) {
        super(message, cause);
        _streamPos = streamPos;
    }

    public long getStreamPos() {
        return _streamPos;
    }

    /**
     * Returns the detail message string of this throwable.
     *
     * @return the detail message string of this <tt>Throwable</tt> instance
     *         (which may be <tt>null</tt>).
     */
    @Override
    public String getMessage() {
        return super.getMessage() + "; at stream position=" + _streamPos;
    }
}
