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

import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 24, 2008
 * To change this template use File | Settings | File Templates.
 */
public final class Settings {

    private static Settings _instance = null;
    private final PropertyMap auxdataConfig = new PropertyMap();

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
        Config.load(auxdataConfig, new File(SystemUtils.getApplicationHomeDir(), "config" +
                File.separator + SystemUtils.getApplicationContextId() + ".auxdata.config"));
    }

    public static boolean isWindowsOS() {
        final String osName = System.getProperty("os.name");
        return (osName.toLowerCase().contains("win"));
    }

    private static String resolve(final PropertyMap prop, final String value) {
        final int idx1 = value.indexOf("${");
        final int idx2 = value.indexOf('}') + 1;
        final String keyWord = value.substring(idx1 + 2, idx2 - 1);
        final String fullKey = value.substring(idx1, idx2);

        String out;
        final String property = System.getProperty(keyWord);
        if (property != null && property.length() > 0) {
            out = value.replace(fullKey, property);
        } else {
            final String env = null; //System.getenv(keyWord);
            if (env != null && env.length() > 0) {
                out = value.replace(fullKey, env);
            } else {
                final String settingStr = prop.getPropertyString(keyWord);
                if (settingStr != null && settingStr.length() > 0) {
                    out = value.replace(fullKey, settingStr);
                } else {
                    if (keyWord.equalsIgnoreCase(ResourceUtils.getContextID() + ".home") || keyWord.equalsIgnoreCase("NEST_HOME")) {
                        out = value.replace(fullKey, ResourceUtils.findHomeFolder().getAbsolutePath());
                    } else {
                        out = value.replace(fullKey, keyWord);
                    }
                }
            }
        }

        if (out.contains("${"))
            out = resolve(prop, out);

        return out;
    }

    public String get(final String key) {
        String val = auxdataConfig.getPropertyString(key);
        if (val != null && val.contains("${")) {
            val = resolve(auxdataConfig, val);
        }
        return val;
    }

    public PropertyMap getAuxdataProperty() {
        return auxdataConfig;
    }

    public static File getAuxDataFolder() {
        String auxDataPath = Settings.instance().get("AuxDataPath");
        if (auxDataPath == null)
            auxDataPath = Settings.instance().get("dataPath");
        if (auxDataPath == null)
            return new File(SystemUtils.getApplicationDataDir(true), "AuxData");
        return new File(auxDataPath);
    }
}
