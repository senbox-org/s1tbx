/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.core.datamodel.quicklooks;

import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Timer;
import java.util.TimerTask;

/**
 * If quicklooks cannot be stored with the products then they are stored in the .snap folder and managed by this database
 * Todo This should be replaced with an actual database to avoid keep all in memory
 * Created by luis on 25/01/2016.
 */
public class QuicklookDB {

    public static final String PREFERENCE_KEY_QUICKLOOKS_CACHE_DIR = "quicklooks.cachedir";
    public static final int QL_NOT_FOUND = -1;
    private static final String QUICKLOOK_PROPERTY_MAP_FILENAME = "qlPropertyMap.db";
    private static final String QUICKLOOK_PROPERTY_MAP_LAST_INDEX = "last.index";

    private static QuicklookDB theInstance = null;
    private final static Path qlCacheDir = getQuicklookCacheDir();
    private final Path propertyMapFile;
    private final PropertyMap propertyMap;
    private boolean needsSaving;
    private int index;
    private Timer timer;

    public static QuicklookDB instance() {
        if (theInstance == null) {
            theInstance = new QuicklookDB();
        }
        return theInstance;
    }

    private QuicklookDB() {
        final File cacheDir = qlCacheDir.toFile();
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            SystemUtils.LOG.severe("Unable to create quicklook cache folder: " + qlCacheDir);
        }

        propertyMap = new DefaultPropertyMap();
        propertyMapFile = qlCacheDir.resolve(QUICKLOOK_PROPERTY_MAP_FILENAME);
        try {
            if (propertyMapFile.toFile().exists()) {
                propertyMap.load(propertyMapFile);
                index = propertyMap.getPropertyInt(QUICKLOOK_PROPERTY_MAP_LAST_INDEX);
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to load quicklook property map: " + propertyMapFile);
        }

        timer = new Timer();
        timer.schedule(new SavePropertiesTask(), 30000, 30000);
    }

    public static Path getQuicklookCacheDir() {
        String cacheDirPath = Config.instance().preferences().get(PREFERENCE_KEY_QUICKLOOKS_CACHE_DIR, null);
        if (cacheDirPath != null) {
            return new File(cacheDirPath).toPath();
        }
        return getDefaultQuicklookCacheDir();
    }

    private static Path getDefaultQuicklookCacheDir() {
        return SystemUtils.getApplicationDataDir().toPath().resolve("var").resolve("quicklooks_cache");
    }

    public int getQuicklookId(final File file) {
        return propertyMap.getPropertyInt(file.getAbsolutePath(), QL_NOT_FOUND);
    }

    public synchronized int addQuickLookId(final File file) {
        ++index;
        propertyMap.setPropertyInt(file.getAbsolutePath(), index);
        propertyMap.setPropertyInt(QUICKLOOK_PROPERTY_MAP_LAST_INDEX, index);
        needsSaving = true;

        return index;
    }

    private synchronized void saveProperties() {
        try {
            propertyMap.store(propertyMapFile, "");
            needsSaving = false;
        } catch (IOException e) {
            SystemUtils.LOG.severe("Unable to save quicklook property map: " + propertyMapFile);
        }
    }

    private class SavePropertiesTask extends TimerTask {
        public void run() {
            if (needsSaving) {
                saveProperties();
            }
        }
    }
}
