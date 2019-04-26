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
package org.esa.snap.core.dataio;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.util.Guardian;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The <code>ProductSubsetDef</code> class describes a subset or portion of a remote sensing data product.
 * <p> Subsets can be spatial or spectral or both. A spatial subset is given through a rectangular region in pixels. The
 * spectral subset as a list of band (or channel) names.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class ProductSubsetDef {

    /**
     * The optional name of the subset
     */
    private String subsetName = null;

    /**
     * The spatial subset.
     */
    private Rectangle region = null;

    /**
     * The spatial subset for each RasterDataNode
     */
    private HashMap<String,Rectangle> regionMap = null;

    /**
     * Subsampling in X direction.
     */
    private int subSamplingX = 1;

    /**
     * Subsampling in Y direction.
     */
    private int subSamplingY = 1;

    /**
     * The band subset.
     */
    private List nodeNameList = null;

    /**
     * ignores or not ignores Metadata at writing or reading a product
     */
    private boolean ignoreMetadata = false;

    private boolean treatVirtualBandsAsRealBands = false;

    /**
     * Constructs a new and empty subset info.
     */
    public ProductSubsetDef() {
        this(null);
    }

    /**
     * Constructs a new and empty subset info.
     * @param subsetName The name of the subset to be created.
     */
    public ProductSubsetDef(String subsetName) {
        this.subsetName = subsetName;
    }

    public String getSubsetName() {
        return subsetName;
    }

    public void setSubsetName(String subsetName) {
        this.subsetName = subsetName;
    }

    public void setTreatVirtualBandsAsRealBands(boolean flag) {
        treatVirtualBandsAsRealBands = flag;
    }

    public boolean getTreatVirtualBandsAsRealBands() {
        return treatVirtualBandsAsRealBands;
    }

    /**
     * Gets the names of all product nodes contained in this subset. A return value of <code>null</code> means all nodes
     * are selected.
     *
     * @return an array of names, or <code>null</code> if the no node subset is given
     */
    public String[] getNodeNames() {
        if (nodeNameList == null) {
            return null;
        }
        String[] result = new String[nodeNameList.size()];
        for (int i = 0; i < nodeNameList.size(); i++) {
            result[i] = (String) nodeNameList.get(i);
        }
        return result;
    }

    /**
     * Sets the names of all product nodes contained in this subset. A value of <code>null</code> means all nodes are
     * selected.
     *
     * @param names the band names, can be <code>null</code> in order to reset the node subset
     */
    public void setNodeNames(String[] names) {
        if (names != null) {
            if (nodeNameList != null) {
                nodeNameList.clear();
            }
            addNodeNames(names);
        } else {
            nodeNameList = null;
        }
    }

    /**
     * Adds a new product node name to this subset.
     *
     * @param name the node's name, must not be empty or <code>null</code>
     */
    public void addNodeName(String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        if (containsNodeName(name)) {
            return;
        }
        if (nodeNameList == null) {
            nodeNameList = new ArrayList();
        }
        nodeNameList.add(name);
    }

    /**
     * Adds the given product node names to this subset.
     *
     * @param names the nodename's to be added
     */
    public void addNodeNames(String[] names) {
        if (names == null) {
            return;
        }
        for (int i = 0; i < names.length; i++) {
            addNodeName(names[i]);
        }
    }

    /**
     * Removes a band from the spectral subset. If the band is not contained in this subset, the method returns
     * <code>false</code>.
     *
     * @param name the band's name
     *
     * @return <code>true</code> for success, <code>false</code> otherwise
     */
    public boolean removeNodeName(String name) {
        int index = getNodeNameIndex(name);
        if (index < 0) {
            return false;
        }
        nodeNameList.remove(index);
        if (nodeNameList.size() == 0) {
            nodeNameList = null;
        }
        return true;
    }

    /**
     * Checks whether or not a node name is already contained in this subset.
     *
     * @param name the node name
     *
     * @return true if so
     */
    public boolean containsNodeName(String name) {
        return getNodeNameIndex(name) >= 0;
    }

    /**
     * Checks whether or not a node (a band, a tie-point grid or metadata element) with the given name will be part of
     * the product subset.
     *
     * @param name the node name
     *
     * @return true if so
     */
    public boolean isNodeAccepted(String name) {
        return nodeNameList == null || containsNodeName(name);
    }

    /**
     * Gets the spatial subset as a rectangular region. Creates a new rectangle each time it is called. This prevents
     * from modifying this subset by modifying the returned region.
     *
     * @return the spatial subset as a rectangular region, or <code>null</code> if no spatial region was defined
     */
    public Rectangle getRegion() {
        return region != null ? new Rectangle(region) : null;
    }

    public HashMap<String,Rectangle> getRegionMap() {
        return regionMap;
    }
    /**
     * Sets the spatial subset as a rectangular region.
     *
     * @param region the spatial subset as a rectangular region, <code>null</code> if no spatial region shall be
     *               defined
     */
    public void setRegion(Rectangle region) {
        if (region == null) {
            this.region = null;
        } else {
            setRegion(region.x, region.y, region.width, region.height);
        }
    }

    public void setRegionMap(HashMap<String,Rectangle> regionMap) {
        this.regionMap = regionMap;
    }

    /**
     * Sets the spatial subset as a rectangular region.
     *
     * @param x the X-offset in pixels
     * @param y the Y-offset in pixels
     * @param w the width of the subset in pixels
     * @param h the height of the subset in pixels
     */
    public void setRegion(int x, int y, int w, int h) {
        if (x < 0 || y < 0 || w < 1 || h < 1) {
            throw new IllegalArgumentException("invalid region");
        }
        region = new Rectangle(x, y, w, h);
    }

    /**
     * Gets the sub-sampling in X- and Y-direction (vertical and horizontal).
     *
     * @param subSamplingX sub-sampling in X-direction, must always be greater than zero
     * @param subSamplingY sub-sampling in Y-direction, must always be greater than zero
     */
    public void setSubSampling(int subSamplingX, int subSamplingY) {
        if (subSamplingX < 1 || subSamplingY < 1) {
            throw new IllegalArgumentException("invalid sub-sampling");
        }
        this.subSamplingX = subSamplingX;
        this.subSamplingY = subSamplingY;
    }

    /**
     * Gets the sub-sampling in X-direction (horizontal).
     *
     * @return the sub-sampling in X-direction which is always greater than zero
     */
    public int getSubSamplingX() {
        return subSamplingX;
    }

    /**
     * Gets the sub-sampling in Y-direction (vertical).
     *
     * @return the sub-sampling in Y-direction which is always greater than zero
     */
    public int getSubSamplingY() {
        return subSamplingY;
    }


    /**
     * Gets the required size for a raster required to hold all pixels for the spatial subset for the given maximum
     * raster width and height.
     *
     * @param maxWidth  the maximum raster width
     * @param maxHeight the maximum raster height
     *
     * @return the required raster size, never <code>null</code>
     */
    public Dimension getSceneRasterSize(int maxWidth, int maxHeight) {
        return getSceneRasterSize(maxWidth, maxHeight, null);
    }

    public Dimension getSceneRasterSize(int maxWidth, int maxHeight, String bandName) {

        int width = maxWidth;
        int height = maxHeight;
        if (region != null) {
            width = region.width;
            height = region.height;
        }
        if(bandName != null && regionMap != null && regionMap.containsKey(bandName)) {
            width = regionMap.get(bandName).width;
            height = regionMap.get(bandName).height;
        }
        return new Dimension((width - 1) / subSamplingX + 1,
                             (height - 1) / subSamplingY + 1);
    }

    /**
     * Sets the ignore metadata information
     *
     * @param ignoreMetadata if <code>true</code>, metadata may be ignored during write or read a product.
     */
    public void setIgnoreMetadata(boolean ignoreMetadata) {
        this.ignoreMetadata = ignoreMetadata;
    }

    /**
     * Gets the ignore metadata information
     */
    public boolean isIgnoreMetadata() {
        return ignoreMetadata;
    }

    /**
     * Checks whether or not this subset definition select the entire product.
     */
    public boolean isEntireProductSelected() {
        return region == null
               && subSamplingX == 1
               && subSamplingY == 1
               && nodeNameList == null
               && !ignoreMetadata;
    }

    /**
     * Gets the index for the given node name. If the name is not contained in this subset, <code>-1</code> is
     * returned.
     *
     * @param name the node name
     *
     * @return the node index or <code>-1</code>
     */
    private int getNodeNameIndex(String name) {
        if (nodeNameList != null) {
            for (int i = 0; i < nodeNameList.size(); i++) {
                String nodeName = (String) nodeNameList.get(i);
                if (nodeName.equalsIgnoreCase(name)) {
                    return i;
                }
            }
        }
        return -1;
    }
}
