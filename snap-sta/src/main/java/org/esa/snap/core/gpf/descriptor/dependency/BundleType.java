package org.esa.snap.core.gpf.descriptor.dependency;

/**
 * Created by kraftek on 10/31/2016.
 */
public enum BundleType {
    /**
     * No bundle
     */
    NONE,
    /**
     * The dependencies are bundled as a single archive (zip) file
     */
    ARCHIVE,
    /**
     * The dependencies are bundled as an installer
     */
    INSTALLER

}
