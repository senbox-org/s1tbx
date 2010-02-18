/*
 * $Id: AbstractVisatPlugIn.java,v 1.4 2006/11/01 10:27:34 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.visat;

import javax.swing.ImageIcon;
import java.net.URL;

/**
 * An abstract implementation of the {@link VisatPlugIn} interface.
 */
public abstract class AbstractVisatPlugIn implements VisatPlugIn {
    protected AbstractVisatPlugIn() {
    }

    /**
     * Called the application shuts down.
     *
     * <p>Clients shall override this method in order to clean-up the resources they have allocated.
     * This default implementation does nothing.</p>
     *
     * @param visatApp a reference to the application instance.
     */
    public void stop(VisatApp visatApp) {
    }

    /**
     * Load an image icon from the given resource path.
     *
     * @param resourcePath name of the desired resource
     *
     * @return the image icon object or null, if no such can be found
     */
    public ImageIcon loadImageIcon(String resourcePath) {
        final URL resource = getResource(resourcePath);
        return resource != null ? new ImageIcon(resource) : null;
    }

    /**
     * Finds a resource with a given path.  This method returns null if no
     * resource with this name is found.  The rules for searching resources
     * associated with a given class are implemented by the defining class
     * loader of this plug-in.
     *
     * @param resourcePath name of the desired resource
     *
     * @return a <code>java.net.URL</code> object or null, if no such can be found
     */
    public URL getResource(String resourcePath) {
        return getClass().getResource(resourcePath);
    }


    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     * <p>The default implementation does nothing.</p>
     */
    public void updateComponentTreeUI() {
    }
}
