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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.transform.GeoCodingMathTransform;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * <code>AbstractGeoCoding</code> is the base class of all geo-coding implementation.
 * <p> <b> Note:</b> New geo-coding implementations shall implement this abstract class, instead of
 * implementing the interface {@link GeoCoding}.
 *
 * @author Marco Peters
 */
public abstract class AbstractGeoCoding implements GeoCoding {

    private CoordinateReferenceSystem imageCRS;
    private CoordinateReferenceSystem mapCRS;
    private CoordinateReferenceSystem geoCRS;
    private volatile MathTransform image2Map;

    /**
     * Default constructor. Sets WGS84 as both the geographic CRS and map CRS.
     */
    protected AbstractGeoCoding() {
        this(DefaultGeographicCRS.WGS84);
    }

    /**
     * Constructor.
     *
     * @param geoCRS The CRS to be used as both the geographic CRS and map CRS.
     */
    protected AbstractGeoCoding(CoordinateReferenceSystem geoCRS) {
        setGeoCRS(geoCRS);
        setMapCRS(geoCRS);
        setImageCRS(createImageCRS(getMapCRS(), new GeoCodingMathTransform(this)));
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     * @return true, if the geo-coding could be transferred.
     */
    public abstract boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef);

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return imageCRS;
    }

    protected final void setImageCRS(CoordinateReferenceSystem imageCRS) {
        Assert.notNull(imageCRS, "imageCRS");
        this.imageCRS = imageCRS;
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return mapCRS;
    }

    protected final void setMapCRS(CoordinateReferenceSystem mapCRS) {
        Assert.notNull(mapCRS, "mapCRS");
        this.mapCRS = mapCRS;
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return geoCRS;
    }

    public final void setGeoCRS(CoordinateReferenceSystem geoCRS) {
        Assert.notNull(geoCRS, "geoCRS");
        this.geoCRS = geoCRS;
    }

    @Override
    public MathTransform getImageToMapTransform() {
        if (image2Map == null) {
            synchronized (this) {
                if (image2Map == null) {
                    try {
                        image2Map = CRS.findMathTransform(imageCRS, mapCRS);
                    } catch (FactoryException e) {
                        throw new IllegalArgumentException(
                                "Not able to find a math transformation from image to map CRS.", e);
                    }
                }
            }
        }
        return image2Map;
    }

    protected static DefaultDerivedCRS createImageCRS(CoordinateReferenceSystem baseCRS,
                                                      MathTransform baseToDerivedTransform) {
        return new DefaultDerivedCRS("Image CS based on " + baseCRS.getName(),
                                     baseCRS,
                                     baseToDerivedTransform,
                                     DefaultCartesianCS.DISPLAY);
    }
}
