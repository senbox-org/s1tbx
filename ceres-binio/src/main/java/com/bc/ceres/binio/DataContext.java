package com.bc.ceres.binio;

/**
 * The context provides the means to read from or write to a random access stream or file.
 * <p>
 * I/O performance my be tuned by setting the {@code ceres.binio.segmentSizeLimit} system property
 * to the size (in bytes) of data segments shared by multiple subsequent compounds members.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.8
 */
public interface DataContext {
    /**
     * @return the file format.
     */
    DataFormat getFormat();

    /**
     * @return the associated I/O-handler used to read from or write to a random access stream or file.
     */
    IOHandler getHandler();

    /**
     * Gets an instance of a compound with I/O starting at the given stream or file position.
     * The type of the compound is the same as the format's compound type.
     * In contrast to {@link #createData()}, the method will always return the same compound instance.
     *
     * @return The instance of a data compound.
     *
     * @see #createData()
     * @see #getFormat()
     */
    CompoundData getData();

    /**
     * Creates an instance of a compound with I/O starting
     * at the beginning of the stream or file.
     *
     * @return An instance of a data compound.
     *
     * @see #getFormat()
     */
    CompoundData createData();

    /**
     * Creates an instance of a compound with the given type and with I/O starting
     * at the given stream or file position.
     *
     * @param position The file position in bytes.
     *
     * @return An instance of a data compound.
     *
     * @see #getFormat()
     */
    CompoundData createData(long position);

    /**
     * Creates an instance of a compound with the given type and with I/O starting
     * at the given stream or file position.
     *
     * @param type     The compound type.
     * @param position The file position in bytes.
     *
     * @return An instance of a data compound.
     */
    CompoundData createData(CompoundType type, long position);

    /**
     * Disposes this context and releases all associated resources.
     */
    void dispose();



    /**
     * Creates an instance of a compound with I/O starting
     * at the given stream or file position.
     * The type of the compound is the same as the format's compound type.
     *
     * @param position The file position in bytes.
     *
     * @return An instance of a data compound.
     *
     * @see #createData()
     * @see #getFormat()
     * @deprecated use {@link #createData(long)} instead
     */
    @Deprecated
    CompoundData getData(long position);

    /**
     * Creates an instance of a compound with the given type and with I/O starting
     * at the given stream or file position.
     *
     * @param type     The compound type.
     * @param position The file position in bytes.
     *
     * @return An instance of a data compound.
     *
     * @see #getFormat()
     * @deprecated use {@link #createData(long)} instead
     */
    @Deprecated
    CompoundData getData(CompoundType type, long position);
}
