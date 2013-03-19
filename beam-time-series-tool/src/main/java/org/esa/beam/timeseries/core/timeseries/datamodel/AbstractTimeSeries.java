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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.timeseries.core.insitu.InsituSource;
import org.esa.beam.util.Guardian;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Instances of AbstractTimeSeries allow the framework user access to every kind of data needed when dealing with time
 * series. Additionally, it provides methods to
 * <ul>
 *  <li>register listeners (of type TimeSeriesListener) in order to get informed about changes of the time series</li>
 *  <li>retrieve the user-selected variables, either in-situ or EO data</li>
 *  <li>retrieve the product, which is internally used as container for the time series</li>
 *  <li>retrieve an instance of {@link AxisMapping}</li>
 * </ul>
 *
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 * @author Thomas Storm
 */
public abstract class AbstractTimeSeries {

    /**
     * must NOT contain SEPARATOR
     */
    static final String DATE_FORMAT = "yyyyMMdd.HHmmss.SSS";

    static final String SEPARATOR = "_";

    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.beam.glob.timeseries";
    public static final String TIME_SERIES_ROOT_NAME = "TIME_SERIES";
    public static final String PRODUCT_LOCATIONS = "PRODUCT_LOCATIONS";
    public static final String SOURCE_PRODUCT_PATHS = "SOURCE_PRODUCT_PATHS";
    public static final String VARIABLE_NAME = "NAME";
    public static final String AUTO_ADJUSTING_TIME_CODING = "AUTO_ADJUSTING_TIME_CODING";
    public static final String VARIABLE_SELECTION = "SELECTION";
    public static final String PL_PATH = "PATH";
    public static final String PL_TYPE = "TYPE";
    public static final String VARIABLES = "VARIABLES";

    public static boolean isPixelValid(Product tsProduct, PixelPos pixelPos) {
        return pixelPos.isValid() &&
               pixelPos.x < tsProduct.getSceneRasterWidth() &&
               pixelPos.x >= 0 &&
               pixelPos.y < tsProduct.getSceneRasterHeight() &&
               pixelPos.y >= 0;
    }

    public abstract List<String> getEoVariables();

    public abstract List<ProductLocation> getProductLocations();

    public abstract void addProductLocation(ProductLocation productLocation);

    public abstract void removeProductLocation(ProductLocation productLocation);

    public abstract boolean isEoVariableSelected(String variableName);

    public abstract void setEoVariableSelected(String variableName, boolean selected);

    public abstract boolean isInsituVariableSelected(String variableName);

    public abstract void setInsituVariableSelected(String variableName, boolean selected);

    public abstract Product getTsProduct();

    public abstract List<Band> getBandsForVariable(String variableName);

    public abstract List<Band> getBandsForProductLocation(ProductLocation location);

    public abstract Map<RasterDataNode, TimeCoding> getRasterTimeMap();

    public abstract boolean isAutoAdjustingTimeCoding();

    public abstract void setAutoAdjustingTimeCoding(boolean autoAdjust);

    public abstract TimeCoding getTimeCoding();

    public abstract void setTimeCoding(TimeCoding timeCoding);

    public abstract void addTimeSeriesListener(TimeSeriesListener listener);

    public abstract void removeTimeSeriesListener(TimeSeriesListener listener);

    public abstract boolean isProductCompatible(Product product, String rasterName);

    public abstract void setInsituSource(InsituSource insituSource);

    public abstract InsituSource getInsituSource();

    public abstract void clearInsituPlacemarks();

    public abstract GeoPos getInsituGeoposFor(Placemark placemark);

    public abstract void registerRelation(Placemark placemark, GeoPos insituGeopos);

    public static String variableToRasterName(String variableName, TimeCoding timeCoding) {
        final ProductData.UTC rasterStartTime = timeCoding.getStartTime();
        Guardian.assertNotNull("rasterStartTime", rasterStartTime);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        return variableName + SEPARATOR + dateFormat.format(rasterStartTime.getAsDate());
    }

    public static String rasterToVariableName(String rasterName) {
        final int lastSeparator = rasterName.lastIndexOf(SEPARATOR);
        return rasterName.substring(0, lastSeparator);
    }

    public abstract boolean hasInsituData();

    public abstract Set<String> getSelectedInsituVariables();

    public abstract AxisMapping getAxisMapping();
}
