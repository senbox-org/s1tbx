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

package org.esa.beam.dataio.arcbin;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

class MetaDataHandler {

    private MetaDataHandler() {
    }

    static MetadataElement createHeaderElement(Header header) {
        MetadataElement elem = new MetadataElement("Header");
        elem.addAttribute(createIntAttr("cellType", header.cellType, "1 = int cover, 2 = float cover."));
        elem.addAttribute(
                createDoubleAttr("pixelSizeX", header.pixelSizeX, "Width of a pixel in georeferenced coordinates."));
        elem.addAttribute(
                createDoubleAttr("pixelSizeY", header.pixelSizeY, "Height of a pixel in georeferenced coordinates."));
        elem.addAttribute(createDoubleAttr("xRef", header.xRef, null));
        elem.addAttribute(createDoubleAttr("yRef", header.yRef, null));
        elem.addAttribute(createIntAttr("tilesPerRow", header.tilesPerRow, "The width of the file in tiles."));
        elem.addAttribute(createIntAttr("tilesPerColumn", header.tilesPerColumn,
                                        "The height of the file in tiles. Note this may be much more than the number of tiles actually represented in the index file."));
        elem.addAttribute(createIntAttr("tileXSize", header.tileXSize, "The width of a file in pixels. Normally 256."));
        elem.addAttribute(createIntAttr("tileYSize", header.tileYSize, "Height of a tile in pixels, usually 4."));
        return elem;
    }

    static MetadataElement createGeorefBoundsElement(GeorefBounds georefBounds) {
        MetadataElement elem = new MetadataElement("GeorefBounds");
        elem.addAttribute(createDoubleAttr("llx", georefBounds.lowerLeftX, "Lower left X (easting) of the grid."));
        elem.addAttribute(createDoubleAttr("lly", georefBounds.lowerLeftY, "Lower left Y (northing) of the grid."));
        elem.addAttribute(createDoubleAttr("urx", georefBounds.upperRightX, "Upper right X (northing) of the grid."));
        elem.addAttribute(createDoubleAttr("ury", georefBounds.upperRightY, "Upper right Y (northing) of the grid."));
        return elem;
    }

    static MetadataElement createRasterStatisticsElement(RasterStatistics rasterStat) {
        MetadataElement elem = new MetadataElement("RasterStatistics");
        elem.addAttribute(createDoubleAttr("min", rasterStat.min, "Minimum value of a raster cell in this grid."));
        elem.addAttribute(createDoubleAttr("max", rasterStat.max, "Maximum value of a raster cell in this grid."));
        elem.addAttribute(createDoubleAttr("mean", rasterStat.mean, "Mean value of a raster cells in this grid."));
        elem.addAttribute(
                createDoubleAttr("stddev", rasterStat.stddev, "Standard deviation of raster cells in this grid."));
        return elem;
    }

    static MetadataAttribute createIntAttr(String name, int value, String desc) {
        ProductData productData = ProductData.createInstance(new int[]{value});
        MetadataAttribute attribute = new MetadataAttribute(name, productData, true);
        if (desc != null) {
            attribute.setDescription(desc);
        }
        return attribute;
    }

    static MetadataAttribute createDoubleAttr(String name, double value, String desc) {
        ProductData productData = ProductData.createInstance(new double[]{value});
        MetadataAttribute attribute = new MetadataAttribute(name, productData, true);
        if (desc != null) {
            attribute.setDescription(desc);
        }
        return attribute;
    }

}
