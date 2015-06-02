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
import java.nio.file.Path;
import java.util.prefs.Preferences;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 24, 2008
 * To change this template use File | Settings | File Templates.
 */
public final class Settings {

    private static Settings _instance = new Settings();
    private static final String SNAP_AUXDATA = "snap.auxdata";
    private final Preferences auxdataPreferences;

    /**
     * @return The unique instance of this class.
     */
    public static Settings instance() {
        return _instance;
    }

    private Settings() {
        auxdataPreferences = Config.instance(SNAP_AUXDATA).load().preferences();
        final Path auxDataPath = SystemUtils.getApplicationDataDir().toPath().resolve("snap-core").resolve("auxdata");
        auxdataPreferences.put("AuxDataPath", auxDataPath.toString());
    }

    public String get(final String key) {
        return auxdataPreferences.get(key, "");
    }

    public static String getPath(final String tag) {
        String path = instance().get(tag);
        path = path.replace("\\", "/");
        if (!path.endsWith("/"))
            path += "/";
        return path;
    }

    public File getAuxDataFolder() {
        return new File(auxdataPreferences.get("AuxDataPath", ""));
    }

}
