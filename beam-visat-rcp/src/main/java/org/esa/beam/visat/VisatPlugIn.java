/*
 * $Id: VisatPlugIn.java,v 1.3 2006/10/20 13:47:32 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat;


/**
 * The VISAT plug-in interface. VISAT searches for implementing classes on startup and instantiates
 * each plug-in using its default no-argument constructor.
 * <p>After loading all plug-ins, their {@link #start(VisatApp)} method is called once in the lifetime
 * of a plug-in.
 * <p/>
 * <p>Clients shall use the {@link AbstractVisatPlugIn} as the base class for their implementations of this interface.
 */
public interface VisatPlugIn {

    /**
     * Called by VISAT after the plug-in instance has been registered in VISAT's plug-in manager.
     *
     * @param visatApp a reference to the VISAT application instance.
     */
    void start(VisatApp visatApp);

    /**
     * Called by VISAT before the application shuts down.
     *
     * @param visatApp a reference to the VISAT application instance.
     */
    void stop(VisatApp visatApp);

    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     */
    void updateComponentTreeUI();
}

