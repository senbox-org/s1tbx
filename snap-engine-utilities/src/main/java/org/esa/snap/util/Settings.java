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

import org.esa.beam.util.SystemUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 24, 2008
 * To change this template use File | Settings | File Templates.
 */
public final class Settings {

    private static Settings _instance = null;
    private final String settingsFile;
    private final Map<String, String> settingMap = new HashMap<String, String>(100);

    private Element rootXML = null;
    private Document doc = null;

    public static final String VALUE = "value";
    public static final String LABEL = "label";

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
        settingsFile = Settings.getSettingsFileName();
        Load();
    }

    public static boolean isWindowsOS() {
        final String osName = System.getProperty("os.name");
        return (osName.toLowerCase().contains("win"));
    }

    private static String getSettingsFileName() {
        if (isWindowsOS()) {
            return "settings_win.xml";
        } else {
            return "settings_unix.xml";
        }
    }

    public Element getSettingsRootXML() {
        return rootXML;
    }

    public void Save() {

        final File userHomePath = SystemUtils.getUserHomeDir();
        final String filePath = userHomePath.getAbsolutePath() + File.separator + "."
                + System.getProperty("ceres.context", "nest")
                + File.separator + settingsFile;

        XMLSupport.SaveXML(doc, filePath);
    }

    public void Load() {

        final File filePath = ResourceUtils.findUserAppFile(settingsFile);

        try {
            doc = XMLSupport.LoadXML(filePath.getAbsolutePath());
        } catch (IOException e) {
            return;
        }

        settingMap.clear();

        rootXML = doc.getRootElement();
        recurseElements(rootXML.getContent(), "");
    }

    private void recurseElements(final List children, final String id) {
        for (Object aChild : children) {
            String newId = "";
            if (aChild instanceof Element) {
                final Element child = (Element) aChild;
                try {
                    if (!id.isEmpty())
                        newId = id + '/';
                    newId += child.getName();
                    final Attribute attrib = child.getAttribute(VALUE);
                    if (attrib != null) {

                        String value = attrib.getValue();
                        if (value.contains("${"))
                            value = resolve(value);
                        settingMap.put(newId, value);
                    }
                } catch (Exception e) {
                    System.out.println("Settings error: " + e.getMessage() + " " + newId);
                }

                final List grandChildren = child.getChildren();
                if (!grandChildren.isEmpty()) {
                    recurseElements(grandChildren, newId);
                }
            }
        }
    }

    private String resolve(String value) {
        String out;
        final int idx1 = value.indexOf("${");
        final int idx2 = value.indexOf('}') + 1;
        final String keyWord = value.substring(idx1 + 2, idx2 - 1);
        final String fullKey = value.substring(idx1, idx2);

        final String property = System.getProperty(keyWord);
        if (property != null && property.length() > 0) {
            out = value.replace(fullKey, property);
        } else {

            final String env = System.getenv(keyWord);
            if (env != null && env.length() > 0) {
                out = value.replace(fullKey, env);
            } else {

                final String settingStr = get(keyWord);
                if (settingStr != null) {
                    out = value.replace(fullKey, settingStr);
                } else {
                    out = value.substring(0, idx1) + value.substring(idx2, value.length());
                }
            }
        }

        if (keyWord.equalsIgnoreCase(ResourceUtils.getContextID() + ".home") || keyWord.equalsIgnoreCase("NEST_HOME")) {
            final String valStr = value.substring(0, idx1) + value.substring(idx2, value.length());
            final File file = ResourceUtils.findInHomeFolder(valStr);
            if (file != null)
                return file.getAbsolutePath();
        }

        if (out.contains("${"))
            out = resolve(out);

        return out;
    }

    public String get(String key) {
        return settingMap.get(key);
    }

    public boolean isTrue(String key) {
        final String val = settingMap.get(key);
        return val != null && val.equals("true");
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
