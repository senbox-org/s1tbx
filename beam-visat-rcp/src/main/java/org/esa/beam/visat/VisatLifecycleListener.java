/*
 * $Id: VisatLifecycleListener.java,v 1.2 2006/10/12 10:51:11 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.visat;

// todo -use something similar in VisatApp

/**
 * A handler which is interested in lifecycle events of the VISAT application and which can
 * prevent VISAT from shutting down by the {@link #canStop(VisatApp)}.
 */
public interface VisatLifecycleListener {

    /**
     * Called when VISAT is started.
     * All plug-ins have been loaded, core UI components are created, visible and responsive when this method is called.
     * <p>This method is called on the swing event-dispatching thread.
     *
     * @param visatApp the VISAT application
     */
    void started(VisatApp visatApp);

    /**
     * Called before VISAT is shut down.
     * The plug-ins are still loaded, the VISAT UI is still visible and responsive when this method is called.
     * <p>This method is called on the swing event-dispatching thread.
     *
     * @param visatApp the VISAT application
     *
     * @return true, if VISAT is allowed to shut down now
     */
    boolean canStop(VisatApp visatApp);

    /**
     * Called when VISAT is stopped.
     * The plug-ins are still loaded, the VISAT UI is still visible and responsive when this method is called.
     * <p>This method is called on the swing event-dispatching thread.
     *
     * @param visatApp the VISAT application
     */
    void stopped(VisatApp visatApp);
}