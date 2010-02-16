/**
 * 
 */
package org.esa.beam.dataio.landsat;

import org.esa.beam.dataio.landsat.LandsatConstants.ConstBand;
import org.esa.beam.framework.datamodel.Band;

import java.io.IOException;
import java.util.List;


/**
 * The interface <code>LandsatTMDataFormat</code> is used to give API user a template for implementing
 * a Landsat data object
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public interface LandsatTMData {

    /**
     * @return the name of the Landsat TM product
     */
    String getProductName();

    /**
     * @return the header information
     */
    LandsatHeader getHeader();

    /**
     * @return the collection
     */
    List getMetadata();

    /**
     * @param idx
     *
     * @return the LandsatBand at the given Bandnumber
     */
    LandsatTMBand getBandAt(final int idx);

    /**
     * @return the format value
     */
    int getFormat();

    /**
     * closes a landsat product
     *
     * @throws IOException
     */
    void close() throws
                 IOException;

    /**
     * @param band
     *
     * @return band reader for the band passed in.
     */
    LandsatBandReader getBandReader(final Band band);

    /**
     * @param band
     *
     * @return band reader for the constant passed in
     */
    LandsatBandReader getBandReader(final ConstBand band);
}
