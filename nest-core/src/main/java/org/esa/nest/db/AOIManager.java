/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.db;

import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**

 */
public class AOIManager {

    private final List<AOI> aoiList = new ArrayList<AOI>(5);
    public final static String LAST_INPUT_PATH = "aoi.last_input_path";
    public final static String LAST_OUTPUT_PATH = "aoi.last_output_path";
    public final static String LAST_GRAPH_PATH = "aoi.last_graph_path";

    public AOI[] getAOIList() {
        return aoiList.toArray(new AOI[aoiList.size()]);
    }

    public AOI createAOI(final File aoiFile) {
        final AOI aoi = new AOI(aoiFile);
        aoiList.add(aoi);
        return aoi;
    }

    public void removeAOI(final AOI aoi) {
        aoiList.remove(aoi);
    }

    public File getNewAOIFile() {
        return new File(getAOIFolder(), "aoi_"+(aoiList.size() + 1));
    }

    public AOI getAOIAt(final int index) {
        return aoiList.get(index);
    }

    public static File getAOIFolder() {
        final String homeUrl = System.getProperty(ResourceUtils.getContextID()+".home", ".");
        final File aoiFolder = new File(homeUrl, File.separator + "aoi");
        if(!aoiFolder.exists())
            aoiFolder.mkdirs();
        return aoiFolder;
    }

    public static String getLastInputPath() {
        return VisatApp.getApp().getPreferences().getPropertyString(LAST_INPUT_PATH, "");
    }

    public static void setLastInputPath(final String path) {
        VisatApp.getApp().getPreferences().setPropertyString(LAST_INPUT_PATH, path);
    }

    public static String getLastOutputPath() {
        return VisatApp.getApp().getPreferences().getPropertyString(LAST_OUTPUT_PATH, "");
    }

    public static void setLastOutputPath(final String path) {
        VisatApp.getApp().getPreferences().setPropertyString(LAST_OUTPUT_PATH, path);
    }
}
