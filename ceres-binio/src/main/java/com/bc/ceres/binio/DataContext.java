package com.bc.ceres.binio;

/**
 * The context provides the means to read from or write to a random access stream.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.8
 */
public interface DataContext {
    DataFormat getFormat();

    IOHandler getHandler();

    CompoundData getData();

    CompoundData getData(long position);

    CompoundData getData(CompoundType type, long position);

    void dispose();
}
