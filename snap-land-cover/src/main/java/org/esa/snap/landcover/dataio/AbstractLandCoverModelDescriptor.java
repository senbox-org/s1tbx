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
package org.esa.snap.landcover.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.Unit;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The <code>AbstractLandCoverModelDescriptor</code> offers a default implementation for the installFiles
 * method. It uses the properties values returned {@link #getArchiveUrl()} and {@link #getInstallDir()} methods
 * in order to download and install the model.
 */
public abstract class AbstractLandCoverModelDescriptor implements LandCoverModelDescriptor {

    protected String name;
    protected double NO_DATA_VALUE = -9999d;
    protected String unit = Unit.CLASS;
    protected File installDir = null;
    protected String remotePath = null;
    protected Path colourIndexFile = null;
    protected String metadataFileName = null;
    protected Path metadataSrcPath = null;
    protected boolean isInstalled = false;
    private final List<ColorPaletteDef.Point> colorPalettePoints = new ArrayList<>(50);

    protected AbstractLandCoverModelDescriptor() {
    }

    public String getName() {
        return name;
    }

    public int getNumXTiles() {
        return 0;
    }

    public int getNumYTiles() {
        return 0;
    }

    public double getNoDataValue() {
        return NO_DATA_VALUE;
    }

    @Override
    public String getUnit() {
        return unit;
    }

    @Override
    public int getDegreeRes() {
        return 0;
    }

    @Override
    public int getPixelRes() {
        return 0;
    }

    @Override
    public boolean isInstalled() {
        return isInstalled;
    }

    public URL getArchiveUrl() {
        try {
            return new URL(remotePath);
        } catch (Exception e) {
            return null;
        }
    }

    public File getInstallDir() {
        return installDir;
    }

    public boolean isInstalling() {
        return false;
    }

    public synchronized boolean installFiles() {
        installMetadata();

        if (installDir != null) {
            final File[] files = installDir.listFiles();
            isInstalled = files != null && files.length > 0;
        }
        return isInstalled;
    }

    public ImageInfo getImageInfo() {
        return new ImageInfo(new ColorPaletteDef(
                colorPalettePoints.toArray(new ColorPaletteDef.Point[colorPalettePoints.size()]), colorPalettePoints.size()));
    }

    public int getDataType() {
        return ProductData.TYPE_INT16;
    }

    public IndexCoding getIndexCoding() {
        if (colourIndexFile != null) {
            if (colorPalettePoints.isEmpty())
                readColorIndex();
            if (!colorPalettePoints.isEmpty())
                return createIndexCoding();
        }
        return null;
    }

    private IndexCoding createIndexCoding() {
        final IndexCoding indexCoding = new IndexCoding("Crop Indices");
        for (ColorPaletteDef.Point p : colorPalettePoints) {
            indexCoding.addIndex(p.getLabel(), (int) p.getSample(), "");

        }
        return indexCoding;
    }

    private void readColorIndex() {
        final PropertyMap prop = new DefaultPropertyMap();
        try {
            prop.load(colourIndexFile);
        } catch (IOException e) {
            SystemUtils.LOG.warning("Unable to read color file " + colourIndexFile);
            return;
        }

        final int size = prop.getProperties().size();
        final List<String> classNameList = new ArrayList<>(size);
        final Map<String, ColorPaletteDef.Point> classMap = new HashMap<>(size);

        final Set<String> keys = prop.getPropertyKeys();
        for (String key : keys) {
            try {
                final Integer value = Integer.parseInt(key);
                final String str = prop.getPropertyString(key);
                final int c = str.indexOf("Color(");
                final int c1 = c + 6;
                final int c2 = str.lastIndexOf(')');
                final String name = StringUtils.padNum(value, 4, ' ') + ' ' + str.substring(0, c).trim();
                final String colStr = str.substring(c1, c2);
                final Color col = StringUtils.parseColor(colStr);

                classNameList.add(name);
                classMap.put(name, new ColorPaletteDef.Point(value, col, name));

            } catch (Exception e) {
                SystemUtils.LOG.warning("Unable to read color file " + colourIndexFile);
            }
        }

        Collections.sort(classNameList);
        for (String name : classNameList) {
            colorPalettePoints.add(classMap.get(name));
        }
    }

    protected void installMetadata() {
        try {
            if (metadataFileName != null) {
                final File file = new File(installDir, metadataFileName);
                if (file.exists())
                    return;

                final ResourceInstaller resourceInstaller = new ResourceInstaller(metadataSrcPath, installDir.toPath());
                resourceInstaller.install(".*", ProgressMonitor.NULL);
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning("Unable to install land cover metadata " + installDir);
        }
    }
}
