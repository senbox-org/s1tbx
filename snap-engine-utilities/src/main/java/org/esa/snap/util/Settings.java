/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 24, 2008
 * To change this template use File | Settings | File Templates.
 */
public final class Settings {

    private static Settings _instance = null;
    private final PropertiesMap auxdataConfig = new PropertiesMap();

    /**
     * @return The unique instance of this class.
     */
    public static Settings instance() {
        if (_instance == null) {
            _instance = new Settings();
        }
        return _instance;
    }

    private Settings() {
        Config.load(auxdataConfig, new File(SystemUtils.getApplicationDataDir(), "config" +
                File.separator + SystemUtils.getApplicationContextId() + ".auxdata.config"));
    }

    public static boolean isWindowsOS() {
        final String osName = System.getProperty("os.name");
        return (osName.toLowerCase().contains("win"));
    }

    public String get(final String key) {
        return auxdataConfig.getPropertyPath(key);
    }

    public PropertyMap getAuxdataProperty() {
        return auxdataConfig;
    }

    public static String getPath(final String tag) {
        String path = instance().get(tag);
        path = path.replace("\\", "/");
        if (!path.endsWith("/"))
            path += "/";
        return path;
    }

    public static File getAuxDataFolder() {
        String auxDataPath = Settings.instance().get("AuxDataPath");
        if (auxDataPath == null || auxDataPath.isEmpty()) {
            if (isWindowsOS()) {
                auxDataPath = "c:\\AuxData";
            } else {
                auxDataPath = SystemUtils.getUserHomeDir() + File.separator + "AuxData";
            }
        }
        return new File(auxDataPath);
    }
}
