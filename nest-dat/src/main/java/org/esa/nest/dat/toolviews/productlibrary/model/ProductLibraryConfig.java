/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.toolviews.productlibrary.model;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.VisatApp;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class handles the configuration of the ProductGrabber.
 */
public class ProductLibraryConfig {

    private static final String WINDOW_LOCATION_X_KEY = "productLibrary.window.locationX";
    private static final String WINDOW_LOCATION_Y_KEY = "productLibrary.window.locationY";
    private static final String WINDOW_WIDTH_KEY = "productLibrary.window.width";
    private static final String WINDOW_HEIGHT_KEY = "productLibrary.window.height";
    private static final String BASE_DIR = "BaseDir_";

    private final PropertyMap properties;

    /**
     * Creates a new instance with the given {@link org.esa.beam.util.PropertyMap}.
     * The property map which is used to load and store the configuration.
     *
     * @param configuration the {@link org.esa.beam.util.PropertyMap}.
     */
    public ProductLibraryConfig(final PropertyMap configuration) {
        Guardian.assertNotNull("configuration", configuration);
        properties = configuration;
    }

    /**
     * Sets the repositories.
     *
     * @param baseDir the repository base directory.
     */
    public void addBaseDir(final File baseDir) {
        properties.setPropertyString(BASE_DIR+baseDir.getAbsolutePath(), baseDir.getAbsolutePath());
        VisatApp.getApp().savePreferences();
    }

    /**
     * removes the repositories.
     *
     * @param baseDir the repository base directory.
     */
    public void removeBaseDir(final File baseDir) {
        properties.setPropertyString(BASE_DIR+baseDir.getAbsolutePath(), null);
        VisatApp.getApp().savePreferences();
    }

    /**
     * Retrieves the stored repositories.
     *
     * @return the stored repositories.
     */
    public File[] getBaseDirs() {
        final List<File> dirList = new ArrayList<File>();
        final Set keys = properties.getProperties().keySet();
        for(Object o : keys) {
            if( o instanceof String) {
                final String key = (String)o;
                if(key.startsWith(BASE_DIR)) {
                    final String path = properties.getPropertyString(key);
                    if(path != null) {
                        final File file = new File(path);
                        if(file.exists()) {
                            dirList.add(file);
                        }
                    }
                }
            }
        }
        return dirList.toArray(new File[dirList.size()]);
    }

    /**
     * Sets the window bounds of the ProductGrabber dialog.
     *
     * @param windowBounds the window bounds.
     */
    public void setWindowBounds(final Rectangle windowBounds) {
        properties.setPropertyInt(WINDOW_LOCATION_X_KEY, windowBounds.x);
        properties.setPropertyInt(WINDOW_LOCATION_Y_KEY, windowBounds.y);
        properties.setPropertyInt(WINDOW_WIDTH_KEY, windowBounds.width);
        properties.setPropertyInt(WINDOW_HEIGHT_KEY, windowBounds.height);
    }

    /**
     * Retrieves the window bounds of the ProductGrabber dialog.
     *
     * @return the window bounds.
     */
    public Rectangle getWindowBounds() {
        final int x = properties.getPropertyInt(WINDOW_LOCATION_X_KEY, 50);
        final int y = properties.getPropertyInt(WINDOW_LOCATION_Y_KEY, 50);
        final int width = properties.getPropertyInt(WINDOW_WIDTH_KEY, 700);
        final int height = properties.getPropertyInt(WINDOW_HEIGHT_KEY, 450);

        return new Rectangle(x, y, width, height);
    }

}