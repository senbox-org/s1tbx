package org.esa.nest.gpf.orbits;

import org.esa.nest.datamodel.Orbits;

import java.io.File;

/**
 * retrieves an orbit file
 */
public interface OrbitFile {

    /**
     * Get orbit information for given time.
     * @param utc The UTC in days.
     * @return The orbit information.
     * @throws Exception The exceptions.
     */
    public Orbits.OrbitData getOrbitData(final double utc) throws Exception;

    /**
     * Get the orbit file used
     * @return the new orbit file
     */
    public File getOrbitFile();
}
