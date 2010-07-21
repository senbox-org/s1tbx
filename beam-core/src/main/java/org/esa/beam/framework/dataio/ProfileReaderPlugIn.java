package org.esa.beam.framework.dataio;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 */
public interface ProfileReaderPlugIn {

    /**
     * Creates a ProductReaderPlugIn for a given profile. The profile is given by the fully classified class name
     * (obtained by <code>.getClass().getName()</code>) of the profile.
     *
     * @param profileClassName the class name
     *
     * @return the ProductReaderPlugIn
     */
    ProductReaderPlugIn createProfileReaderPlugin(String profileClassName);
}
