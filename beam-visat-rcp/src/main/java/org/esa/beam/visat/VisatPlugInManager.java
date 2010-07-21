/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.beam.visat;

import org.esa.beam.util.logging.BeamLogManager;

import java.util.logging.Level;

/**
 * The <code>VisatPlugInManager</code> class loads, initializes and stores all VISAT plug-ins.
 * <p/>
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class VisatPlugInManager {

    private final VisatPlugIn[] plugins;

    /**
     * Constructs a new plug-in manager.
     */
    public VisatPlugInManager(VisatPlugIn[] plugins) {
        this.plugins = plugins;
    }

    /**
     * Initializes all plug-ins registered so far in this manager.
     */
    public void startPlugins() {
        for (VisatPlugIn plugin : plugins) {
            try {
                plugin.start(VisatApp.getApp());
                final String msg = String.format("%s plug-in started: %s",
                                                 VisatApp.getApp().getAppName(),
                                                 plugin.getClass().getName());
                BeamLogManager.getSystemLogger().info(msg);
            } catch (Throwable e) {
                // Note: it is OK in this case to catch a Throwable, because "foreign" code is executed here
                final String msg = String.format("Failed to start %s plug-in: %s",
                                               VisatApp.getApp().getAppName(),
                                               plugin.getClass().getName());
                BeamLogManager.getSystemLogger().log(Level.SEVERE, msg, e);
            }
        }
    }

    /**
     * Initializes all plug-ins registered so far in this manager.
     */
    public void stopPlugins() {
        // reverse order
        for (int i = plugins.length - 1; i >= 0; i--) {
            VisatPlugIn plugin = plugins[i];
            try {
                plugin.stop(VisatApp.getApp());
                final String msg = String.format("%s plug-in stopped: %s",
                                                 VisatApp.getApp().getAppName(),
                                                 plugin.getClass().getName());

                BeamLogManager.getSystemLogger().info(msg);
            } catch (Throwable e) {
                final String msg = String.format("Failed to stop %s plug-in: %s",
                                               VisatApp.getApp().getAppName(),
                                               plugin.getClass().getName());
                BeamLogManager.getSystemLogger().log(Level.SEVERE, msg, e);
            }
        }
    }

    /**
     * Asks all installed plug-ins to update their UI.
     */
    public void updatePluginsComponentTreeUI() {
        for (VisatPlugIn plugin : plugins) {
            try {
                plugin.updateComponentTreeUI();
            } catch (Throwable e) {
                // Ignore!
            }
        }
    }
}
