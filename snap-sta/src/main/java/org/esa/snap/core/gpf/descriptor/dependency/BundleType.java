package org.esa.snap.core.gpf.descriptor.dependency;

/**
 * Bundle types enumeration.
 *
 * @author  Cosmin Cara
 * @since   5.0.4
 */
public enum BundleType {
    /**
     * No bundle
     */
    NONE,
    /**
     * The dependencies are packed as a single zip
     */
    ZIP,
    /**
     * The dependencies are bundled as an installer
     */
    INSTALLER;


    @Override
    public String toString() {
        String ret = null;
        switch (this) {
            case NONE:
                ret = "None";
                break;
            case ZIP:
                ret = "Zip archive";
                break;
            case INSTALLER:
                ret = "Installer";
                break;
        }
        return ret;
    }
}
