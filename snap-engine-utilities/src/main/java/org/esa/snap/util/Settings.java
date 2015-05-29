/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.util;

import org.esa.snap.runtime.Config;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 24, 2008
 * To change this template use File | Settings | File Templates.
 */
public final class Settings {

    private static Settings _instance = new Settings();
    private static final String SNAP_AUXDATA = "snap.auxdata";

    /**
     * @return The unique instance of this class.
     */
    public static Settings instance() {
        return _instance;
    }

    private Settings() {
        Config.instance().preferences(SNAP_AUXDATA).put("AuxDataPath", SystemUtils.getApplicationDataDir() + File.separator + "snap-core"+File.separator+"auxdata");
    }

    public static boolean isWindowsOS() {
        final String osName = System.getProperty("os.name");
        return (osName.toLowerCase().contains("win"));
    }

    public String get(final String key) {
        return Config.instance().preferences(SNAP_AUXDATA).get(key, "");
    }

    public static String getPath(final String tag) {
        String path = instance().get(tag);
        path = path.replace("\\", "/");
        if (!path.endsWith("/"))
            path += "/";
        return path;
    }

    public static File getAuxDataFolder() {
        return new File(Config.instance().preferences().get("AuxDataPath", ""));
    }

    public static PropertiesMap getAutomatedTestConfigPropertyMap(final String name) {
        return null;
    }
}
