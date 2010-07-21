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
package org.esa.beam.dataio.modis.hdf;

import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.dataop.maptransf.CartographicMapTransform;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformUI;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.StringUtils;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HdfEosStructMetadata {

    private EosMetadata eosMetadata;

    /**
     * Constructs the object with default values.
     *
     * @param structMetaString -
     *
     * @throws ProductIOException -
     */
    public HdfEosStructMetadata(final String structMetaString) throws ProductIOException {
        parse(structMetaString);
    }

    /**
     * Retrieves the data field with the given name
     *
     * @param name the name of the Datafield
     *
     * @return the data field
     */
    public HdfDataField getDatafield(final String name) {
        return eosMetadata.getDatafield(name);
    }

    /**
     * Gets the product dimension.
     *
     * @return the product dimension.
     */
    public Dimension getProductDimensions() {
        return eosMetadata.getProductDimensions();
    }

    public int[] getSubsamplingAndOffset(String dimName) {
        return eosMetadata.getSubsamplingAndOffset(dimName);
    }

    public String getEosType() {
        return eosMetadata.getEosType();
    }

    public GeoCoding createGeocoding() {
        return eosMetadata.createGeocoding();
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void parse(String metaString) throws ProductIOException {
        final String ssStartKey = "GROUP=SwathStructure";
        final String ssEndKey = "END_GROUP=SwathStructure";
        final String gsStartKey = "GROUP=GridStructure";
        final String gsEndKey = "END_GROUP=GridStructure";
        final String psStartKey = "GROUP=PointStructure";
        final String psEndKey = "END_GROUP=PointStructure";

        final int ssStartIdx = metaString.indexOf(ssStartKey) + ssStartKey.length();
        final int ssEndIdx = metaString.indexOf(ssEndKey);
        final int gsStartIdx = metaString.indexOf(gsStartKey) + gsStartKey.length();
        final int gsEndIdx = metaString.indexOf(gsEndKey);
        final int psStartIdx = metaString.indexOf(psStartKey) + psStartKey.length();
        final int psEndIdx = metaString.indexOf(psEndKey);

        final String swathStructureMetadata = metaString.substring(ssStartIdx, ssEndIdx).trim();
        final String gridStructureMetadata = metaString.substring(gsStartIdx, gsEndIdx).trim();
        final String pointStructureMetadata = metaString.substring(psStartIdx, psEndIdx).trim();

        if (isGridStructure(gridStructureMetadata)) {
            eosMetadata = GridMetadata.parse(gridStructureMetadata);
        } else if (isSwathStructure(swathStructureMetadata)) {
            eosMetadata = SwathMetadata.parse(swathStructureMetadata);
        } else if (isPointStructure(pointStructureMetadata)) {
            eosMetadata = PointMetadata.parse(pointStructureMetadata);
        }
    }

    private boolean isPointStructure(String pointStructureMetadata) {
        return pointStructureMetadata != null && pointStructureMetadata.length() > 0;
    }

    private boolean isGridStructure(final String gridStructureMetadata) {
        return gridStructureMetadata != null && gridStructureMetadata.length() > 0;
    }

    private boolean isSwathStructure(String swathStructureMetadata) {
        return swathStructureMetadata != null && swathStructureMetadata.length() > 0;
    }

    private static String getAssignedValue(String line) {
        int posEqual = line.indexOf('=');

        String value = line.substring(posEqual + 1, line.length());
        value = value.trim();
        if (value.startsWith("\"")) {
            value = value.substring(1);
        }
        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void parseDimensionGroup(final LineNumberReader reader,
                                            final Map<String, HdfDimension> dimensions) throws IOException {
        HdfDimension dim = null;
        String name = "";
        String value = "";

        String line;
        while (!(line = reader.readLine()).contains(ModisConstants.GROUP_END_KEY)) {
            line = line.trim();

            // note: the order of asking for END_OBJECT and OBJECT should not be changed.
            if (line.contains(ModisConstants.DIMENSION_NAME_KEY)) {
                name = getAssignedValue(line);
            } else if (line.contains(ModisConstants.SIZE_KEY)) {
                value = getAssignedValue(line);
            } else if (line.contains(ModisConstants.OBJECT_END_KEY)) {
                if (dim != null) {
                    dim.setName(name);
                    dim.setValue(Integer.parseInt(value));
                    dimensions.put(name, dim);
                }
            } else if (line.contains(ModisConstants.OBJECT_KEY)) {
                dim = new HdfDimension();
            }
        }
    }

    private static void parseDimensionMapGroup(LineNumberReader reader,
                                               final Map<String, HdfDimensionMap> dimensionMaps) throws IOException {
        HdfDimensionMap dim = null;

        String line;
        while (!(line = reader.readLine()).contains(ModisConstants.GROUP_END_KEY)) {
            line = line.trim();

            // note: the order of asking for END_OBJECT and OBJECT should not be changed.
            if (line.startsWith(ModisConstants.GEO_DIMENSION_KEY)) {
                dim.setGeoDim(getAssignedValue(line));
            } else if (line.startsWith(ModisConstants.DATA_DIMENSION_KEY)) {
                dim.setDataDim(getAssignedValue(line));
            } else if (line.startsWith(ModisConstants.OFFSET_KEY)) {
                dim.setOffset(Integer.parseInt(getAssignedValue(line)));
            } else if (line.startsWith(ModisConstants.INCREMENT_KEY)) {
                dim.setIncrement(Integer.parseInt(getAssignedValue(line)));
            } else if (line.startsWith(ModisConstants.OBJECT_END_KEY)) {
                final String geoDim = dim.getGeoDim();
                if (geoDim != null && !"".equals(geoDim)) {
                    dimensionMaps.put(geoDim, dim);
                    dim = null;
                }
            } else if (line.startsWith(ModisConstants.OBJECT_KEY)) {
                dim = new HdfDimensionMap();
            }
        }
    }

    private static void parseDataFields(final LineNumberReader reader,
                                        final Map<String, HdfDataField> target,
                                        final Map<String, HdfDimension> dimensions) throws IOException {
        HdfDataField field = null;

        String line;
        while (!(line = reader.readLine()).contains(ModisConstants.GROUP_END_KEY)) {
            line = line.trim();

            // note: the order of asking for END_OBJECT and OBJECT should not be changed.
            if (line.startsWith(ModisConstants.GEO_FIELD_NAME_KEY) || line.contains(
                    ModisConstants.DATA_FIELD_NAME_KEY)) {
                field.setName(getAssignedValue(line));
            } else if (line.startsWith(ModisConstants.DATA_TYPE_KEY)) {
                // not used
                // getAssignedValue(line);
            } else if (line.startsWith(ModisConstants.DIMENSION_LIST_KEY)) {
                final String dimList = getAssignedValue(line);
                final String[] dimNames = parseValues(dimList);
                field.setDimensionNames(dimNames);
                final int[] dims = decodeDimNames(dimensions, dimNames);
                if (dims.length == 1) {
                    field.setWidth(dims[0]);
                } else if (dims.length == 2) {
                    field.setHeight(dims[0]);
                    field.setWidth(dims[1]);
                } else if (dims.length == 3) {
                    field.setLayers(dims[0]);
                    field.setHeight(dims[1]);
                    field.setWidth(dims[2]);
                }
            } else if (line.startsWith(ModisConstants.OBJECT_END_KEY)) {
                if (field != null) {
                    target.put(field.getName(), field);
                    field = null;
                }
            } else if (line.startsWith(ModisConstants.OBJECT_KEY)) {
                field = new HdfDataField();
            }
        }
    }

    private static int[] decodeDimNames(final Map<String, HdfDimension> _dimensions,
                                        final String[] dimNames) {
        final int[] ints = new int[dimNames.length];

        for (int i = 0; i < dimNames.length; i++) {
            final HdfDimension dim = _dimensions.get(dimNames[i]);
            if (dim != null) {
                ints[i] = dim.getValue();
            }
        }
        return ints;
    }

    private static String[] parseValues(String values) {
        final List<String> tokens = new ArrayList<String>();
        StringUtils.split(values, new char[]{'(', '\"', ',', ')'}, true, tokens);

        // remove empty tokens
        while (tokens.contains("")) {
            tokens.remove("");
        }

        return tokens.toArray(new String[tokens.size()]);
    }

    private static interface EosMetadata {

        HdfDataField getDatafield(final String name);

        Dimension getProductDimensions();

        int[] getSubsamplingAndOffset(String dimName);

        String getEosType();

        GeoCoding createGeocoding();
    }

    private static class SwathMetadata implements EosMetadata {

        private final Map<String, HdfDataField> _dataFields;
        private final Map<String, HdfDimensionMap> _dimensionMaps;
        private final Map<String, HdfDataField> _geoFields;

        public SwathMetadata(final Map<String, HdfDataField> dataFields,
                             final Map<String, HdfDimensionMap> dimensionMaps,
                             final Map<String, HdfDataField> geoFields) {
            _dataFields = dataFields;
            _dimensionMaps = dimensionMaps;
            _geoFields = geoFields;
        }

        public HdfDataField getDatafield(String name) {
            final HdfDataField dfRet = _dataFields.get(name);
            if (dfRet != null) {
                return dfRet;
            }
            return _geoFields.get(name);
        }

        public Dimension getProductDimensions() {
            Iterator<HdfDataField> it = _dataFields.values().iterator();

            int width = -1;
            int height = -1;
            while (it.hasNext()) {
                final HdfDataField data = it.next();
                width = Math.max(width, data.getWidth());
                height = Math.max(height, data.getHeight());
            }
            return new Dimension(width, height);
        }

        public int[] getSubsamplingAndOffset(String dimName) {
            final HdfDimensionMap dim = _dimensionMaps.get(dimName);
            final int subsampling;
            final int offset;
            if (dim != null) {
                subsampling = dim.getIncrement();
                offset = dim.getOffset();
                return new int[]{subsampling, offset};
            } else {
                subsampling = 1;
                offset = 0;
            }
            return new int[]{subsampling, offset};
        }

        public String getEosType() {
            return ModisConstants.EOS_TYPE_SWATH;
        }

        public GeoCoding createGeocoding() {
            return null;
        }

        public static EosMetadata parse(final String swathStructureMetadata) throws ProductIOException {
            final LineNumberReader reader = new LineNumberReader(new StringReader(swathStructureMetadata));

            String line;
            String value;

            final Map<String, HdfDimension> dimensions = new HashMap<String, HdfDimension>();

            final Map<String, HdfDimensionMap> dimensionMaps = new HashMap<String, HdfDimensionMap>();
            final Map<String, HdfDataField> geoDataFields = new HashMap<String, HdfDataField>();
            final Map<String, HdfDataField> dataFields = new HashMap<String, HdfDataField>();

            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.startsWith(ModisConstants.GROUP_KEY)) {
                        value = getAssignedValue(line);

                        if (value.equalsIgnoreCase(ModisConstants.DIMENSION_KEY)) {
                            parseDimensionGroup(reader, dimensions);
                        } else if (value.equalsIgnoreCase(ModisConstants.DIMENSION_MAP_KEY)) {
                            parseDimensionMapGroup(reader, dimensionMaps);
                        } else if (value.equalsIgnoreCase(ModisConstants.GEO_FIELD_KEY)) {
                            parseDataFields(reader, geoDataFields, dimensions);
                        } else if (value.equalsIgnoreCase(ModisConstants.DATA_FIELD_KEY)) {
                            parseDataFields(reader, dataFields, dimensions);
                        }
                    }
                }
            } catch (IOException e) {
                throw new ProductIOException(e.getMessage());
            }
            return new SwathMetadata(dataFields, dimensionMaps, geoDataFields);
        }
    }

    private static class GridMetadata implements EosMetadata {

        public static final String KEY_XDIM = "XDim";
        public static final String KEY_YDIM = "YDim";
        public static final String KEY_UL_METERS = "UpperLeftPointMtrs";
        public static final String KEY_LR_METERS = "LowerRightMtrs";
        public static final String KEY_PROJECTION = "Projection";
        public static final String KEY_PROJ_PARAMS = "ProjParams";
        public static final String KEY_SPHERE_CODE = "SphereCode";
        public static final String KEY_GRID_ORIGIN = "GridOrigin";
        public static final String KEY_PIXEL_ORIGIN = "PixelRegistration";

        public static final String GRID_ORIGIN_UPPER_LEFT = "HDFE_GD_UL";
        public static final String GRID_ORIGIN_UPPER_RIGHT = "HDFE_GD_UR";
        public static final String GRID_ORIGIN_LOWER_LEFT = "HDFE_GD_LL";
        public static final String GRID_ORIGIN_LOWER_RIGHT = "HDFE_GD_LR";

        private final Dimension _productDimension;
        private final HashMap<String, HdfDataField> _dataFields;
        private final GeoCodingParams _geoCodingParams;

        public GridMetadata(final Dimension productDimension,
                            final HashMap<String, HdfDataField> dataFields,
                            final GeoCodingParams geoCodingParams) {
            _productDimension = productDimension;
            _dataFields = dataFields;
            _geoCodingParams = geoCodingParams;
            _geoCodingParams.setProductDimension(productDimension);
        }

        public HdfDataField getDatafield(String name) {
            return _dataFields.get(name);
        }

        public Dimension getProductDimensions() {
            return new Dimension(_productDimension.width, _productDimension.height);
        }

        public int[] getSubsamplingAndOffset(String dimName) {
            final int subsampling = 1;
            final int offset = 0;
            return new int[]{subsampling, offset};
        }

        public String getEosType() {
            return ModisConstants.EOS_TYPE_GRID;
        }

        public GeoCoding createGeocoding() {
            return _geoCodingParams.createGeocoding();
        }

        public static EosMetadata parse(final String gridStructureMetadata) throws ProductIOException {
            final Dimension productDimension = new Dimension();
            final HashMap<String, HdfDataField> dataFields = new HashMap<String, HdfDataField>();
            final HashMap<String, HdfDimension> dimensions = new HashMap<String, HdfDimension>();
            final GeoCodingParams geoCodingParams = new GeoCodingParams();

            final LineNumberReader reader = new LineNumberReader(new StringReader(gridStructureMetadata));
            String line;
            String value;
            try {
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    value = getAssignedValue(line);

                    if (line.startsWith(KEY_XDIM)) {
                        final int dimVal = Integer.parseInt(value);
                        addDimension(dimensions, KEY_XDIM, dimVal);
                        productDimension.width = dimVal;
                    } else if (line.startsWith(KEY_YDIM)) {
                        final int dimVal = Integer.parseInt(value);
                        addDimension(dimensions, KEY_YDIM, dimVal);
                        productDimension.height = dimVal;
                    } else if (line.startsWith(KEY_UL_METERS)) {
                        geoCodingParams.setUlMeters(getDoubles(value));
                    } else if (line.startsWith(KEY_LR_METERS)) {
                        geoCodingParams.setLrMeters(getDoubles(value));
                    } else if (line.startsWith(KEY_PROJECTION)) {
                        geoCodingParams.setProjection(value);
                    } else if (line.startsWith(KEY_PROJ_PARAMS)) {
                        geoCodingParams.setProjParams(getDoubles(value));
                    } else if (line.startsWith(KEY_SPHERE_CODE)) {
                        geoCodingParams.setSphereCode(value);
                    } else if (line.startsWith(KEY_GRID_ORIGIN)) {
                        geoCodingParams.setGridOrigin(value);
                    } else if (line.startsWith(KEY_PIXEL_ORIGIN)) {
                        geoCodingParams.setPixelOrigin(value);
                    } else if (line.startsWith(ModisConstants.GROUP_KEY)) {
                        if (value.equalsIgnoreCase(ModisConstants.DATA_FIELD_KEY)) {
                            parseDataFields(reader, dataFields, dimensions);
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalFileFormatException(e.getMessage());
            }

            return new GridMetadata(productDimension, dataFields, geoCodingParams);
        }

        private static void addDimension(HashMap<String, HdfDimension> dimensions,
                                         final String dimName, final int dimVal) {
            final HdfDimension dimension = new HdfDimension(dimName, dimVal);
            dimensions.put(dimension.getName(), dimension);
        }

        private static double[] getDoubles(String value) {
            return getDoubles(value, null);
        }

        private static double[] getDoubles(String value, double[] target) {
            final String[] values = parseValues(value);
            if (target == null || target.length != values.length) {
                target = new double[values.length];
            }
            for (int i = 0; i < target.length; i++) {
                target[i] = Double.parseDouble(values[i]);
            }
            return target;
        }

        private static class GeoCodingParams {

            private Dimension productDimension;
            private double[] ulMeters = null;
            private double[] lrMeters = null;
            private String projection = null;
            private double[] projParams = null;
            private String sphereCode = null;
            private String gridOrigin = null;
            private String pixelOrigin = null;

            public void setProductDimension(final Dimension productDimension) {
                this.productDimension = productDimension;
            }

            public void setUlMeters(double[] ulMeters) {
                this.ulMeters = ulMeters;
            }

            public void setLrMeters(double[] lrMeters) {
                this.lrMeters = lrMeters;
            }

            public void setProjection(String projection) {
                this.projection = projection;
            }

            public void setProjParams(double[] projParams) {
                this.projParams = projParams;
            }

            public void setSphereCode(String sphereCode) {
                this.sphereCode = sphereCode;
            }

            public void setGridOrigin(String gridOrigin) {
                this.gridOrigin = gridOrigin;
            }

            public void setPixelOrigin(String pixelOrigin) {
                this.pixelOrigin = pixelOrigin;
            }

            public GeoCoding createGeocoding() {
                final String mapProjectionName = "Sinusoidal";
                final MapTransform mapAlgorithm = new SinMapTransform(projParams);
                final MapProjection mapProjection = new MapProjection(mapProjectionName, mapAlgorithm);
                final int w = productDimension.width;
                final int h = productDimension.height;
                final double ulx = ulMeters[0];
                final double uly = ulMeters[1];
                final double lrx = lrMeters[0];
                final double lry = lrMeters[1];
                final double xMeters = lrx - ulx;
                final double yMeters = uly - lry;
                final float pixelX = 0.5f;
                final float pixelY = 0.5f;
//                final float pixelX = w / 2 + 0.5f;
//                final float pixelY = h / 2 + 0.5f;
                final float easting = (float) ulx;
//                final float easting = (float) (xMeters / 2 + lrx);
                final float northing = (float) uly;
//                final float northing = (float) (yMeters / 2 + lry);
                final float pixelSizeX = (float) (xMeters / w);
                final float pixelSizeY = (float) (yMeters / h);

                final MapInfo mapInfo = new MapInfo(
                        mapProjection, pixelX, pixelY, easting, northing, pixelSizeX, pixelSizeY, Datum.WGS_84);
                mapInfo.setSceneWidth(productDimension.width);
                mapInfo.setSceneHeight(productDimension.height);
                mapInfo.setOrientation(0);

                return new MapGeoCoding(mapInfo);
            }

            private static class SinMapTransform extends CartographicMapTransform {

                private final double[] projectionParameters;

                protected SinMapTransform(final double[] projectionParameters) {
                    super(0.0, 0.0, 0.0, projectionParameters[0]);
                    this.projectionParameters = projectionParameters;
                }

                @Override
                protected Point2D forward_impl(float lat, float lon, Point2D mapPoint) {
                    final double phi = Math.toRadians(lat);
                    final double lam = Math.toRadians(lon);
                    final double cosphi = Math.cos(phi);

                    final double x = (lam - _centralMeridian) * cosphi;
                    final double y = phi;

                    if (mapPoint == null) {
                        mapPoint = new Point2D.Double();
                    }
                    mapPoint.setLocation(x, y);
                    return mapPoint;
                }

                @Override
                protected GeoPos inverse_impl(float x, float y, GeoPos geoPoint) {
                    final float cm = _centralMeridian;
                    final double phi = y;
                    final double lam = cm + x / Math.cos(phi);

                    if (geoPoint == null) {
                        geoPoint = new GeoPos();
                    }
                    geoPoint.setLocation((float) Math.toDegrees(phi), (float) Math.toDegrees(lam));

                    return geoPoint;
                }

                public MapTransformDescriptor getDescriptor() {
                    return new MapTransformDescriptor() {
                        public static final String TYPE_ID = "Sinusuidal";
                        public static final String NAME = TYPE_ID;
                        public static final String MAP_UNIT = "meter";
                        public static final double semiMajor = 6371007.181000;
                        public static final double VAL = 0.0;

                        public void registerProjections() {
                            //Todo change body of created method. Use File | Settings | File Templates to change
                        }

                        public MapTransform createTransform(double[] parameterValues) {
                            return new SinMapTransform(parameterValues);
                        }

                        public String getTypeID() {
                            return TYPE_ID;
                        }

                        public String getName() {
                            return NAME;
                        }

                        public String getMapUnit() {
                            return MAP_UNIT;
                        }

                        public double[] getParameterDefaultValues() {
                            return new double[]{
                                    semiMajor, VAL, VAL, VAL, VAL, VAL, VAL, VAL, VAL, VAL, VAL, VAL, VAL
                            };
                        }

                        public Parameter[] getParameters() {
                            final Parameter parameter = new Parameter("SemiMajor", semiMajor);
                            parameter.getProperties().setLabel("semiMajor");
                            parameter.getProperties().setPhysicalUnit("meters");
                            return new Parameter[]{parameter};
                        }

                        public boolean hasTransformUI() {
                            return false;
                        }

                        public MapTransformUI getTransformUI(MapTransform transform) {
                            return null;
                        }
                    };
                }

                public double[] getParameterValues() {
                    final double[] doubles = new double[projectionParameters.length];
                    System.arraycopy(projectionParameters, 0, doubles, 0, doubles.length);
                    return doubles;
                }

                public MapTransform createDeepClone() {
                    return new SinMapTransform(getParameterValues());
                }
            }
        }
    }

    private static class PointMetadata implements EosMetadata {

        public HdfDataField getDatafield(String name) {
            return null;  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        public Dimension getProductDimensions() {
            return null;  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        public int[] getSubsamplingAndOffset(String dimName) {
            return new int[0];  //Todo change body of created method. Use File | Settings | File Templates to change
        }

        public String getEosType() {
            return ModisConstants.EOS_TYPE_POINT;
        }

        public GeoCoding createGeocoding() {
            return null;
        }

        public static EosMetadata parse(final String pointStructureMetadata) {
            return new PointMetadata();
        }
    }
}
